package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.bow.HorseBowAbility;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Light Arrows: Punch on a horse bow's arrow pushes half as far. Punch is an int level here, so this
 * halves the push itself: the first {@code Vec3.scale} in {@code onHitEntity} is
 * {@code knockback * 0.6} (the second is the bounce off a target it didn't hurt).
 */
@Mixin(AbstractArrow.class)
abstract class AbstractArrowMixin {
	@ModifyArg(method = "onHitEntity", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal = 0))
	private double weaponry$lightArrowPunch(double knockback) {
		return HorseBowAbility.isLight((AbstractArrow) (Object) this) ? knockback * 0.5 : knockback;
	}

	/** Rapid Hits: its arrows clear the hurt cooldown, so a stream of them all land. */
	@WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean weaponry$rapidHits(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		boolean hurt = original.call(target, source, amount);
		if (hurt && RapidCrossbowItem.isRapidArrow((AbstractArrow) (Object) this)) {
			target.invulnerableTime = 0;
		}
		return hurt;
	}
}
