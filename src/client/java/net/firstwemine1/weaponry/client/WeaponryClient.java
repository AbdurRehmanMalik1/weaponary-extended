package net.firstwemine1.weaponry.client;

import net.firstwemine1.weaponry.bow.BowState;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.firstwemine1.weaponry.client.crossbow.RapidCrossbowClient;
import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

/**
 * Client-only setup: renderers for ability entities, the flipped war pick model, the bows' and
 * crossbows' animations, the rapid crossbow's firing, and the Stagger and Daze screen wobble.
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
		for (WeaponItem item : ModItems.variants(WeaponType.WAR_PICK)) {
			ItemProperties.register(item, ModComponents.FLIPPED_ID,
					(stack, level, entity, seed) -> stack.has(ModComponents.FLIPPED) ? 1 : 0);
		}
		ModBows.all().forEach(WeaponryClient::registerBowAnimation);
		registerHeavyCrossbowAnimation();
		registerRapidCrossbowAnimation();
		ClientTickEvents.END_CLIENT_TICK.register(client -> wobble(client.player));
		ClientTickEvents.END_CLIENT_TICK.register(RapidCrossbowClient::tick);
	}

	/**
	 * The properties vanilla gives {@code minecraft:crossbow}. {@code getChargeDuration} already gives
	 * this crossbow's own load time, so the frames switch at the right moments.
	 */
	private static void registerHeavyCrossbowAnimation() {
		ItemProperties.register(ModCrossbows.HEAVY, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
				entity == null || CrossbowItem.isCharged(stack) ? 0.0f
						: (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack, entity));
		ItemProperties.register(ModCrossbows.HEAVY, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.HEAVY, ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) ->
				CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.HEAVY, ResourceLocation.withDefaultNamespace("firework"), (stack, level, entity, seed) -> {
			ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
			return projectiles != null && projectiles.contains(Items.FIREWORK_ROCKET) ? 1.0f : 0.0f;
		});
	}

	/** While reloading, {@code pull} shows how full the magazine is, so the string draws back as it fills. */
	private static void registerRapidCrossbowAnimation() {
		ItemProperties.register(ModCrossbows.RAPID, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
				RapidCrossbowItem.magazine(stack).count() / (float) Magazine.CAPACITY);
		ItemProperties.register(ModCrossbows.RAPID, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem().is(ModCrossbows.RAPID) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.RAPID, ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) ->
				RapidCrossbowItem.looksLoaded(stack, entity) ? 1.0f : 0.0f);
	}

	/**
	 * The {@code pull} and {@code pulling} properties vanilla gives {@code minecraft:bow}, which pick the
	 * draw frame. {@code pull} is measured against this bow's own draw time (and any Quick Nock head
	 * start), so every bow reaches its last frame when it's fully drawn.
	 */
	private static void registerBowAnimation(WeaponBowItem bow) {
		ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
				entity == null || entity.getUseItem() != stack ? 0.0f
						: (entity.getTicksUsingItem() + BowState.headStart(entity)) / (float) bow.drawTicks(entity));
		ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
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
