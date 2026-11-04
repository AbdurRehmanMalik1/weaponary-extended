package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.bow.HorseBowAbility;
import net.firstwemine1.weaponry.crossbow.HeavyCrossbowItem;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Light Arrows: Punch on a horse bow's arrow pushes half as far. Heavy Bolt: a heavy crossbow's arrow
 * has built-in Punch II. Rapid hits: a rapid crossbow's arrow clears the hurt cooldown it just started,
 * or the next arrows in the stream would bounce off.
 */
@Mixin(AbstractArrow.class)
abstract class AbstractArrowMixin {
	@ModifyExpressionValue(method = "doKnockback", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyKnockback(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private float weaponry$lightArrowPunch(float knockback) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (HeavyCrossbowItem.isHeavyBolt(self)) {
			return knockback + HeavyCrossbowItem.PUNCH;
		}
		return HorseBowAbility.isLight(self) ? knockback * 0.5f : knockback;
	}

	@WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean weaponry$rapidHits(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		boolean hurt = original.call(target, source, amount);
		if (hurt && RapidCrossbowItem.isRapidArrow((AbstractArrow) (Object) this)) {
			target.invulnerableTime = 0;
		}
		return hurt;
	}
}
