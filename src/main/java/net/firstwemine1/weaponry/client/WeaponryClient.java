package net.firstwemine1.weaponry.client;

import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemModelsProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup: renderers for ability entities, the flipped war pick model, and the Stagger and
 * Daze screen wobble.
 */
public final class WeaponryClient {
	/**
	 * Both effects use the nausea-style view wobble (vanilla Nausea winds it up to 1.0). Stagger is a
	 * short, sharp lurch: it winds up fast. Daze is a slower, steadier sway.
	 */
	private static final float STAGGER_WOBBLE = 0.6f;
	private static final float STAGGER_WOBBLE_PER_TICK = 0.15f;
	private static final float DAZE_WOBBLE = 0.5f;
	private static final float DAZE_WOBBLE_PER_TICK = 0.04f;

	private WeaponryClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener((FMLClientSetupEvent event) -> {
			RenderingRegistry.registerEntityRenderingHandler(ModEntities.THROWN_WEAPON.get(), ThrownWeaponRenderer::new);
			RenderingRegistry.registerEntityRenderingHandler(ModEntities.ROPE_DART.get(), RopeDartRenderer::new);
			// The property registry is a plain map, so it is filled on the main thread.
			event.enqueueWork(() -> {
				for (WeaponMaterial material : WeaponMaterial.values()) {
					ItemModelsProperties.register(ModItems.get(WeaponType.WAR_PICK, material), WarPickAbility.FLIPPED_ID,
							(stack, level, entity) -> WarPickAbility.isFlipped(stack) ? 1 : 0);
				}
			});
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				wobble(Minecraft.getInstance().player);
			}
		});
	}

	/**
	 * Vanilla only wobbles the view for Nausea with over 3 s left, so these short effects would never
	 * show. After the player's tick, wind the wobble up while affected; vanilla winds it back down.
	 */
	private static void wobble(ClientPlayerEntity player) {
		if (player == null) {
			return;
		}
		if (player.hasEffect(ModEffects.STAGGER.get())) {
			windUp(player, STAGGER_WOBBLE, STAGGER_WOBBLE_PER_TICK);
		} else if (player.hasEffect(ModEffects.DAZE.get())) {
			windUp(player, DAZE_WOBBLE, DAZE_WOBBLE_PER_TICK);
		}
	}

	private static void windUp(ClientPlayerEntity player, float max, float perTick) {
		// The nausea wobble is still called portalTime in this version.
		player.portalTime = Math.max(player.portalTime, Math.min(max, player.oPortalTime + perTick));
	}
}
