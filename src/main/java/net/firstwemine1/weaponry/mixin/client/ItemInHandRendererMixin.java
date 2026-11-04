package net.firstwemine1.weaponry.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.client.crossbow.CrossbowPose;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** First person: vanilla compares against {@code Items.CROSSBOW} for its hold and loading poses. */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@ModifyExpressionValue(method = {"renderHandsWithItems", "renderArmWithItem"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
	private Item weaponry$isCrossbow(Item item) {
		return CrossbowPose.asCrossbow(item);
	}

	@WrapOperation(method = {"renderHandsWithItems", "renderArmWithItem"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean weaponry$isCharged(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}
}
