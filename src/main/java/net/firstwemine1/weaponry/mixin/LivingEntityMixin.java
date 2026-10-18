package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.ability.SabreAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weapon effects on the entity being hit: the sabre parry, some weapons hit through a raised shield
 * (flail), and some wear armor faster (morning star). Shield breaking is in
 * {@code PlayerMixin.blockUsingShield} in this version.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	/** Set at the start of {@code hurt} when this hit gets past the shield, read by the block check. */
	@Unique
	private boolean weaponry$throughShield;

	/** The sabre parry, run where later Fabric API versions fire {@code ALLOW_DAMAGE}. */
	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void weaponry$parry(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level.isClientSide() && !SabreAbility.allowDamage(self, source, amount)) {
			cir.setReturnValue(false);
		}
	}

	/** Over the shield: a hit the shield would block deals part of its damage instead of none. */
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float weaponry$passShield(float amount, DamageSource source, float originalAmount) {
		LivingEntity self = (LivingEntity) (Object) this;
		weaponry$throughShield = false;
		LivingEntity attacker = weaponry$meleeAttacker(source);
		if (attacker == null || amount <= 0 || !self.isDamageSourceBlocked(source)) {
			return amount;
		}
		float pass = Math.min(1, WeaponItem.abilityOf(attacker.getMainHandItem()).shieldPassThrough(attacker));
		if (pass <= 0) {
			return amount;
		}
		weaponry$throughShield = true;
		return amount * pass;
	}

	@WrapOperation(method = "hurt", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
	private boolean weaponry$unblocked(LivingEntity self, DamageSource source, Operation<Boolean> original) {
		if (weaponry$throughShield) {
			weaponry$throughShield = false;
			self.level.playSound(null, self.getX(), self.getY(), self.getZ(),
					SoundEvents.CHAIN_HIT, self.getSoundSource(), 1.0f, 0.8f);
			return false;
		}
		return original.call(self, source);
	}

	/** Armor wear: the hit costs the target's armor more durability. Only players' armor wears in vanilla. */
	@WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
	private void weaponry$armorWear(LivingEntity self, DamageSource source, float damage, Operation<Void> original) {
		LivingEntity attacker = weaponry$meleeAttacker(source);
		float wear = attacker != null && attacker.getMainHandItem().getItem() instanceof WeaponItem weapon
				? weapon.type().ability().armorWearMultiplier(weapon.material())
				: 1;
		original.call(self, source, damage * wear);
	}

	/**
	 * The attacker of a plain melee hit (no projectiles, thorns or sweeps), or {@code null}. There are no
	 * damage types yet: melee hits are the "player" and "mob" sources.
	 */
	@Unique
	private static LivingEntity weaponry$meleeAttacker(DamageSource source) {
		if (("player".equals(source.getMsgId()) || "mob".equals(source.getMsgId()))
				&& source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
			return attacker;
		}
		return null;
	}
}
