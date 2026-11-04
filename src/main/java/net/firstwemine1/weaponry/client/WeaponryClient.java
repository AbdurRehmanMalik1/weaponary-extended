package net.firstwemine1.weaponry.client;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.client.crossbow.RapidCrossbowClient;
import net.firstwemine1.weaponry.client.crossbow.RapidFill;
import net.firstwemine1.weaponry.client.crossbow.RapidLoaded;
import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-only setup: renderers for ability entities, the bows' draw frame and flail wind-up properties,
 * wrapped tooltips, and the Stagger and Daze screen wobble.
 */
@Mod(value = Weaponry.MOD_ID, dist = Dist.CLIENT)
public final class WeaponryClient {
	/**
	 * Both effects use the nausea-style view wobble (vanilla Nausea winds it up to 1.0). Stagger is a
	 * short, sharp lurch: it winds up fast. Daze is a slower, steadier sway.
	 */
	private static final float STAGGER_WOBBLE = 0.6f;
	private static final float STAGGER_WOBBLE_PER_TICK = 0.15f;
	private static final float DAZE_WOBBLE = 0.5f;
	private static final float DAZE_WOBBLE_PER_TICK = 0.04f;

	public WeaponryClient(IEventBus modBus) {
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
			event.registerEntityRenderer(ModEntities.THROWN_WEAPON.get(), ThrownWeaponRenderer::new);
			event.registerEntityRenderer(ModEntities.ROPE_DART.get(), RopeDartRenderer::new);
		});
		modBus.addListener(BowPull::register);
		modBus.addListener(FlailSpin::register);
		modBus.addListener(RapidFill::register);
		modBus.addListener(RapidLoaded::register);
		NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> TooltipLines.wrap(event.getItemStack(), event.getToolTip()));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> wobble(Minecraft.getInstance().player));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> RapidCrossbowClient.tick(Minecraft.getInstance()));
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
		player.portalEffectIntensity = Math.max(player.portalEffectIntensity,
				Math.min(max, player.oPortalEffectIntensity + perTick));
	}
}
