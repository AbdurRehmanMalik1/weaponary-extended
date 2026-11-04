package net.firstwemine1.weaponry.client;

import net.firstwemine1.weaponry.client.crossbow.RapidCrossbowClient;
import net.firstwemine1.weaponry.client.crossbow.RapidFill;
import net.firstwemine1.weaponry.client.crossbow.RapidLoaded;
import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-only setup: renderers for ability entities, the bows' draw frame, the flail's wind-up and the
 * rapid crossbow's model properties, the rapid crossbow's firing, wrapped tooltips, and the Stagger and Daze screen wobble.
 */
public class WeaponryClient implements ClientModInitializer {
	/**
	 * Both effects use the nausea-style view wobble (vanilla Nausea winds it up to 1.0). Stagger is a
	 * short, sharp lurch: it winds up fast. Daze is a slower, steadier sway.
	 */
	private static final float STAGGER_WOBBLE = 0.6f;
	private static final float STAGGER_WOBBLE_PER_TICK = 0.15f;
	private static final float DAZE_WOBBLE = 0.5f;
	private static final float DAZE_WOBBLE_PER_TICK = 0.04f;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.THROWN_WEAPON, ThrownWeaponRenderer::new);
		EntityRendererRegistry.register(ModEntities.ROPE_DART, RopeDartRenderer::new);
		BowPull.register();
		FlailSpin.register();
		RapidFill.register();
		RapidLoaded.register();
		TooltipLines.register();
		ClientTickEvents.END_CLIENT_TICK.register(client -> wobble(client.player));
		ClientTickEvents.END_CLIENT_TICK.register(RapidCrossbowClient::tick);
	}

	/**
	 * Vanilla only wobbles the view for Nausea with over 3 s left, so these short effects would never
	 * show. After the player's tick, wind the wobble up while affected; vanilla winds it back down.
	 */
	private static void wobble(LocalPlayer player) {
		if (player == null) {
			return;
		}
		if (player.hasEffect(ModEffects.STAGGER)) {
			windUp(player, STAGGER_WOBBLE, STAGGER_WOBBLE_PER_TICK);
		} else if (player.hasEffect(ModEffects.DAZE)) {
			windUp(player, DAZE_WOBBLE, DAZE_WOBBLE_PER_TICK);
		}
	}

	private static void windUp(LocalPlayer player, float max, float perTick) {
		player.spinningEffectIntensity = Math.max(player.spinningEffectIntensity,
				Math.min(max, player.oSpinningEffectIntensity + perTick));
	}
}
