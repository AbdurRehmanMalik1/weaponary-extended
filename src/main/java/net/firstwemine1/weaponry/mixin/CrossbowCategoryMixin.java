package net.firstwemine1.weaponry.mixin;

import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The rapid crossbow takes crossbow enchantments, though it isn't vanilla's {@code CrossbowItem}. */
@Mixin(targets = "net.minecraft.world.item.enchantment.EnchantmentCategory$13")
abstract class CrossbowCategoryMixin {
	@Inject(method = "canEnchant(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
	private void weaponry$acceptRapid(Item item, CallbackInfoReturnable<Boolean> cir) {
		if (item instanceof RapidCrossbowItem) {
			cir.setReturnValue(true);
		}
	}
}
