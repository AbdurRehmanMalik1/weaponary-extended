package net.firstwemine1.weaponry.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.client.crossbow.CrossbowPose;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** First person: vanilla only holds a {@code minecraft:crossbow} in both hands, and aims a loaded one. */
@Mixin(FirstPersonHandsAndItems.class)
abstract class FirstPersonHandsAndItemsMixin {
	@WrapOperation(method = {"evaluateWhichHandsToRender", "isChargedCrossbow"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
	private static boolean weaponry$isCrossbow(ItemStack stack, Object item, Operation<Boolean> original) {
		return CrossbowPose.counts(stack, item) || original.call(stack, item);
	}

	@WrapOperation(method = "isChargedCrossbow",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private static boolean weaponry$isCharged(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}
}
