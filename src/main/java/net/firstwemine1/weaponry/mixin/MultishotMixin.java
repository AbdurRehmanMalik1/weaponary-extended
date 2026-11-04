package net.firstwemine1.weaponry.mixin;

import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No Multishot on the rapid crossbow from an anvil. */
@Mixin(Enchantment.class)
abstract class MultishotMixin {
	@Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
	private void weaponry$noMultishot(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this == Enchantments.MULTISHOT && stack.getItem() instanceof RapidCrossbowItem) {
			cir.setReturnValue(false);
		}
	}
}
