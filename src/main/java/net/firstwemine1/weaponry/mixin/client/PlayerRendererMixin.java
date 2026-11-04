package net.firstwemine1.weaponry.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
	@ModifyExpressionValue(method = "getArmPose",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
	private static Item weaponry$isCrossbow(Item item) {
		return CrossbowPose.asCrossbow(item);
	}

	@WrapOperation(method = "getArmPose",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private static boolean weaponry$isCharged(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}
}
