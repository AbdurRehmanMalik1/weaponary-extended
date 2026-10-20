package net.firstwemine1.weaponry.client.mixin;

import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Light Draw: vanilla slows the player to 20% while using an item ({@code aiStep}, the only two 0.2
 * constants there). A bow can set its own walk speed while drawing.
 */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
	@ModifyConstant(method = "aiStep", constant = @Constant(floatValue = 0.2f))
	private float weaponry$bowWalkSpeed(float original) {
		WeaponBowItem bow = ModBows.of(((LocalPlayer) (Object) this).getUseItem());
		return bow != null ? bow.type().ability().walkSpeedWhileDrawing() : original;
	}
}
