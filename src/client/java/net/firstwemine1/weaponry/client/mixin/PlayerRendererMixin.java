package net.firstwemine1.weaponry.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.client.crossbow.CrossbowPose;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Third person: vanilla only aims a loaded {@code minecraft:crossbow}. */
@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
	@WrapOperation(method = "getArmPose(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
	private static boolean weaponry$isCrossbow(ItemStack stack, Item item, Operation<Boolean> original) {
		return CrossbowPose.counts(stack, item) || original.call(stack, item);
	}

	@WrapOperation(method = "getArmPose(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private static boolean weaponry$isCharged(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}
}
