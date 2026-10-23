package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Weapon effects on the entity being hit: some weapons hit through a raised shield (flail), and some
 * wear armor faster (morning star). Shield-breakers disable shields through their {@code weapon} component.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	/** Set at the start of {@code hurtServer} when this hit gets past the shield, read by the block step. */
	@Unique
	private boolean weaponry$throughShield;

	/** Over the shield: a hit the shield would block deals part of its damage instead of none. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float weaponry$passShield(float amount, ServerLevel level, DamageSource source, float originalAmount) {
		LivingEntity self = (LivingEntity) (Object) this;
		weaponry$throughShield = false;
		LivingEntity attacker = weaponry$meleeAttacker(source);
		if (attacker == null || amount <= 0 || !weaponry$wouldBlock(self, source, amount)) {
			return amount;
		}
		float pass = Math.min(1, WeaponItem.abilityOf(attacker.getMainHandItem()).shieldPassThrough(attacker));
		if (pass <= 0) {
			return amount;
		}
		weaponry$throughShield = true;
		return amount * pass;
	}

	/** Skips the shield for a hit that got past it: it blocks nothing, takes no damage and isn't disabled. */
	@WrapOperation(method = "hurtServer", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;applyItemBlocking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private float weaponry$unblocked(LivingEntity self, ServerLevel level, DamageSource source, float amount, Operation<Float> original) {
		if (weaponry$throughShield) {
			weaponry$throughShield = false;
			self.level().playSound(null, self.getX(), self.getY(), self.getZ(),
					SoundEvents.CHAIN_HIT, self.getSoundSource(), 1.0f, 0.8f);
			return 0.0f;
		}
		return original.call(self, level, source, amount);
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
	 * Whether {@code applyItemBlocking} would block some of this hit, without its side effects (shield
	 * damage, disabling, knockback). Melee hits only, so the piercing-arrow case doesn't apply.
	 */
	@Unique
	private static boolean weaponry$wouldBlock(LivingEntity self, DamageSource source, float amount) {
		ItemStack shield = self.getItemBlockingWith();
		BlocksAttacks blocks = shield == null ? null : shield.get(DataComponents.BLOCKS_ATTACKS);
		if (blocks == null || blocks.bypassedBy().map(source::is).orElse(false)) {
			return false;
		}
		Vec3 from = source.getSourcePosition();
		double angle = Math.PI;
		if (from != null) {
			Vec3 view = self.calculateViewVector(0.0f, self.getYHeadRot());
			Vec3 toSource = from.subtract(self.position());
			angle = Math.acos(new Vec3(toSource.x, 0.0, toSource.z).normalize().dot(view));
		}
		return blocks.resolveBlockedDamage(source, amount, angle) > 0;
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
