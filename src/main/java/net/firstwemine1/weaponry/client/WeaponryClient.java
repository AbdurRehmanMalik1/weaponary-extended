package net.firstwemine1.weaponry.client;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;

/**
 * Client-only setup: renderers for ability entities, the flipped war pick model, and the Stagger and
 * Daze screen wobble.
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
		// The item property map isn't thread-safe, so register on the main thread.
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			for (WeaponMaterial material : WeaponMaterial.values()) {
				ItemProperties.register(ModItems.get(WeaponType.WAR_PICK, material), ModComponents.FLIPPED_ID,
						(stack, level, entity, seed) -> stack.has(ModComponents.FLIPPED.get()) ? 1 : 0);
			}
		}));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> wobble(Minecraft.getInstance().player));
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
