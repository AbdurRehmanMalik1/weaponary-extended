package net.firstwemine1.weaponry.mixin;

import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The hatchet chops wood like an axe and the flipped war pick mines like a pickaxe, so they take the
 * {@code DIGGER} enchantments (Efficiency, Fortune, Silk Touch). That category only accepts {@code DiggerItem}; {@code DIGGER} is the 7th
 * constant, so its body is {@code EnchantmentCategory$7}. See {@code WeaponEnchantmentCategoryMixin}.
 */
@Mixin(targets = "net.minecraft.world.item.enchantment.EnchantmentCategory$7")
public abstract class WeaponDiggerCategoryMixin {
	@Inject(method = "canEnchant(Lnet/minecraft/world/item/Item;)Z", at = @At("HEAD"), cancellable = true)
	private void weaponry$acceptDiggingWeapons(Item item, CallbackInfoReturnable<Boolean> cir) {
		if (item instanceof WeaponItem && ((WeaponItem) item).type().ability().diggerEnchantable()
				&& (Object) this == EnchantmentCategory.DIGGER) {
			cir.setReturnValue(true);
		}
	}
}
