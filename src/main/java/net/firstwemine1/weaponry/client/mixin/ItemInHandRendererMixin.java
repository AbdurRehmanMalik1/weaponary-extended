package net.firstwemine1.weaponry.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.client.crossbow.CrossbowPose;
import net.firstwemine1.weaponry.crossbow.WeaponCrossbow;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;

/** First person: vanilla only holds and loads {@code minecraft:crossbow} like a crossbow. */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@WrapOperation(method = {"evaluateWhichHandsToRender", "selectionUsingItemWhileHoldingBowLike", "isChargedCrossbow"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
	private static boolean weaponry$isCrossbowStatic(ItemStack stack, Item item, Operation<Boolean> original) {
		return CrossbowPose.counts(stack, item) || original.call(stack, item);
	}

	/** NeoForge checks {@code instanceof CrossbowItem} here, which the rapid crossbow isn't. */
	@WrapOperation(method = "renderArmWithItem", constant = @Constant(classValue = CrossbowItem.class))
	private boolean weaponry$isCrossbow(Object item, Operation<Boolean> original) {
		return item instanceof WeaponCrossbow || original.call(item);
	}

	@WrapOperation(method = "isChargedCrossbow",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private static boolean weaponry$isChargedStatic(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}

	@WrapOperation(method = "renderArmWithItem",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean weaponry$isCharged(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}
}
