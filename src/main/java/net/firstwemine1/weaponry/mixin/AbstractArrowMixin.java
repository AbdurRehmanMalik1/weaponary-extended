package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.firstwemine1.weaponry.bow.HorseBowAbility;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Light Arrows: Punch on a horse bow's arrow pushes half as far. */
@Mixin(AbstractArrow.class)
abstract class AbstractArrowMixin {
	@ModifyExpressionValue(method = "doKnockback", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyKnockback(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private float weaponry$lightArrowPunch(float knockback) {
		return HorseBowAbility.isLight((AbstractArrow) (Object) this) ? knockback * 0.5f : knockback;
	}
}
