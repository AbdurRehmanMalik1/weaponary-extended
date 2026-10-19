package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.firstwemine1.weaponry.bow.HorseBowAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Weapon effects on the entity being hit: shield-breakers (halberd, hatchet) disable shields the way
 * axes do, some weapons hit through a raised shield (flail), and some wear armor faster (morning star).
 * Horse bow arrows knock back half as far.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	/** Set at the start of {@code hurt} when this hit gets past the shield, read by the block check. */
	@Unique
	private boolean weaponry$throughShield;

	@ModifyReturnValue(method = "canDisableShield", at = @At("RETURN"))
	private boolean weaponry$shieldBreaker(boolean original) {
		return original || WeaponItem.abilityOf(((LivingEntity) (Object) this).getWeaponItem()).disablesShields();
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
			self.level().playSound(null, self.getX(), self.getY(), self.getZ(),
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

	/** Light Arrows: a horse bow's arrow knocks its target back half as far. */
	@ModifyArg(method = "hurt", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"), index = 0)
	private double weaponry$lightArrow(double strength, @Local(argsOnly = true) DamageSource source) {
		return HorseBowAbility.isLight(source.getDirectEntity()) ? strength * 0.5 : strength;
	}

	/** The attacker of a plain melee hit (no projectiles, thorns or sweeps), or {@code null}. */
	@Unique
	private static LivingEntity weaponry$meleeAttacker(DamageSource source) {
		if ((source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))
				&& source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
			return attacker;
		}
		return null;
	}
}
