package net.firstwemine1.weaponry.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.firstwemine1.weaponry.bow.BowState;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draw zoom: vanilla only zooms the view while drawing {@code minecraft:bow}. Our bows zoom the same
 * way over their own draw time, plus any extra zoom from their ability (the longbow's Steady Aim).
 */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerMixin {
	/** Vanilla's zoom at full draw. */
	@Unique
	private static final float FULL_DRAW_ZOOM = 0.15f;

	@ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
	private float weaponry$bowZoom(float result) {
		AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
		WeaponBowItem bow = self.isUsingItem() ? ModBows.of(self.getUseItem()) : null;
		float scale = Minecraft.getInstance().options.fovEffectScale;
		if (bow == null || scale <= 0) {
			return result;
		}
		int drawTicks = bow.drawTicks(self);
		int drawn = self.getTicksUsingItem() + BowState.headStart(self);
		float pull = drawn / (float) drawTicks;
		float g = pull >= 1 ? 1 : pull * pull;
		float zoom = 1 - g * FULL_DRAW_ZOOM - bow.type().ability().extraZoom(WeaponBowItem.ticksAtFullDraw(drawn, drawTicks));
		// The result is lerp(scale, 1, f): undo it, apply the zoom to f, and redo it.
		float f = (result - 1) / scale + 1;
		return 1 + scale * (f * zoom - 1);
	}
}
