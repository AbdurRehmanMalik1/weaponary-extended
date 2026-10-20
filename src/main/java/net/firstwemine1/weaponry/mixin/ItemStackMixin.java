package net.firstwemine1.weaponry.mixin;

import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code Item.isCorrectToolForDrops} doesn't see the stack in this version, so a flipped war pick
 * gets its pickaxe drops (stone, ores) here instead. Both the drops and the mining speed penalty for
 * the wrong tool go through this.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void weaponry$flippedWarPick(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		ItemStack stack = (ItemStack) (Object) this;
		if (WarPickAbility.isFlipped(stack)) {
			cir.setReturnValue(WarPickAbility.isCorrectToolForDrops(((WeaponItem) stack.getItem()).material(), state));
		}
	}
}
