package net.firstwemine1.weaponry.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The enchanting table never offers the rapid crossbow Multishot, though it counts as a crossbow. */
@Mixin(EnchantmentHelper.class)
abstract class RapidEnchantmentMixin {
	@ModifyReturnValue(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
	private static List<EnchantmentInstance> weaponry$noMultishot(List<EnchantmentInstance> results, int cost, ItemStack stack, boolean treasure) {
		if (stack.is(ModCrossbows.RAPID)) {
			results.removeIf(result -> result.enchantment == Enchantments.MULTISHOT);
		}
		return results;
	}
}
