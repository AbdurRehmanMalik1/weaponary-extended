package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.firstwemine1.weaponry.crossbow.HeavyCrossbowItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Slow Crank: vanilla's load time is a static method and its load sounds a package-private one. */
@Mixin(CrossbowItem.class)
abstract class CrossbowItemMixin {
	@ModifyArg(method = "getChargeDuration", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyCrossbowChargingTime(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;F)F"), index = 2)
	private static float weaponry$slowCrank(float seconds, @Local(argsOnly = true) ItemStack stack) {
		return stack.getItem() instanceof HeavyCrossbowItem ? HeavyCrossbowItem.CHARGE_SECONDS : seconds;
	}

	/** Quick Charge has its own, faster sounds, which stay as they are. */
	@ModifyReturnValue(method = "getChargingSounds", at = @At("RETURN"))
	private static CrossbowItem.ChargingSounds weaponry$slowCrankSounds(CrossbowItem.ChargingSounds sounds, @Local(argsOnly = true) ItemStack stack) {
		boolean vanilla = sounds.start().filter(start -> start == SoundEvents.CROSSBOW_LOADING_START).isPresent();
		return vanilla && stack.getItem() instanceof HeavyCrossbowItem ? HeavyCrossbowItem.sounds() : sounds;
	}
}
