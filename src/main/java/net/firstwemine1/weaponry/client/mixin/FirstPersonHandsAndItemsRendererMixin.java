package net.firstwemine1.weaponry.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.client.crossbow.CrossbowPose;
import net.firstwemine1.weaponry.crossbow.WeaponCrossbow;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;

/** First person: the crossbow's hold and loading poses. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class FirstPersonHandsAndItemsRendererMixin {
	/** NeoForge checks {@code instanceof CrossbowItem} here, which the rapid crossbow isn't. */
	@WrapOperation(method = "submitArmWithItem", constant = @Constant(classValue = CrossbowItem.class))
	private boolean weaponry$isCrossbow(Object item, Operation<Boolean> original) {
		return item instanceof WeaponCrossbow || original.call(item);
	}

	@WrapOperation(method = "submitArmWithItem",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean weaponry$isCharged(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}
}
