package net.firstwemine1.weaponry.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.client.crossbow.CrossbowPose;
import net.firstwemine1.weaponry.crossbow.WeaponCrossbow;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;

/** Third person: vanilla only aims a loaded {@code minecraft:crossbow}. */
@Mixin(PlayerRenderer.class)
abstract class PlayerRendererMixin {
	/** NeoForge checks {@code instanceof CrossbowItem} here, which the rapid crossbow isn't. */
	@WrapOperation(method = "getArmPose", constant = @Constant(classValue = CrossbowItem.class))
	private static boolean weaponry$isCrossbow(Object item, Operation<Boolean> original) {
		return item instanceof WeaponCrossbow || original.call(item);
	}

	@WrapOperation(method = "getArmPose",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;isCharged(Lnet/minecraft/world/item/ItemStack;)Z"))
	private static boolean weaponry$isCharged(ItemStack stack, Operation<Boolean> original) {
		return CrossbowPose.isCharged(stack) || original.call(stack);
	}
}
