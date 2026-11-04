package net.firstwemine1.weaponry.client.mixin;

import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gatling: with a rapid crossbow in the main hand, attack fires instead of hitting or breaking blocks. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Shadow
	public LocalPlayer player;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void weaponry$noRapidCrossbowAttack(CallbackInfoReturnable<Boolean> cir) {
		if (weaponry$holdingRapidCrossbow()) {
			cir.setReturnValue(false);
		}
	}

	/** Called with "attack is held"; reporting false also stops any block breaking in progress. */
	@ModifyVariable(method = "continueAttack", at = @At("HEAD"), argsOnly = true)
	private boolean weaponry$noRapidCrossbowBreaking(boolean attacking) {
		return attacking && !weaponry$holdingRapidCrossbow();
	}

	@Unique
	private boolean weaponry$holdingRapidCrossbow() {
		return player != null && player.getMainHandItem().is(ModCrossbows.RAPID);
	}
}
