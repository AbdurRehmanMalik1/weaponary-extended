package net.firstwemine1.weaponry.client;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.CrossbowItem;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.client.crossbow.RapidCrossbowClient;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.ability.FlailAbility;
import net.firstwemine1.weaponry.bow.BowState;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-only setup: renderers for ability entities, the flipped war pick model, the flail's wind-up
 * frames, the bows' draw animation, wrapped tooltips, and the Stagger and Daze screen wobble.
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
			ItemProperties.register(item, WarPickAbility.FLIPPED_ID,
					(stack, level, entity, seed) -> WarPickAbility.isFlipped(stack) ? 1 : 0);
		}
		for (WeaponItem item : ModItems.variants(WeaponType.FLAIL)) {
			ItemProperties.register(item, FlailAbility.SPIN_ID,
					(stack, level, entity, seed) -> FlailAbility.spinFrame(stack, entity));
		}
		ModelLoadingPlugin.register(context -> context.addModels(RopeDartRenderer.HEAD_MODEL));
		ModBows.all().forEach(WeaponryClient::registerBowAnimation);
		registerHeavyCrossbowAnimation();
		registerRapidCrossbowAnimation();
		ClientTickEvents.END_CLIENT_TICK.register(RapidCrossbowClient::tick);
		TooltipLines.register();
		ClientTickEvents.END_CLIENT_TICK.register(client -> wobble(client.player));
	}

	/**
	 * The properties vanilla gives {@code minecraft:crossbow}. {@code getChargeDuration} already gives
	 * this crossbow's own load time, so the frames switch at the right moments.
	 */
	private static void registerHeavyCrossbowAnimation() {
		ItemProperties.register(ModCrossbows.HEAVY, new ResourceLocation("pull"), (stack, level, entity, seed) ->
				entity == null || CrossbowItem.isCharged(stack) ? 0.0f
						: (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack));
		ItemProperties.register(ModCrossbows.HEAVY, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.HEAVY, new ResourceLocation("charged"), (stack, level, entity, seed) ->
				CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.HEAVY, new ResourceLocation("firework"), (stack, level, entity, seed) ->
				CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.0f : 0.0f);
	}

	/** While reloading, {@code pull} shows how full the magazine is, so the string draws back as it fills. */
	private static void registerRapidCrossbowAnimation() {
		ItemProperties.register(ModCrossbows.RAPID, new ResourceLocation("pull"), (stack, level, entity, seed) ->
				RapidCrossbowItem.magazine(stack).count() / (float) Magazine.CAPACITY);
		ItemProperties.register(ModCrossbows.RAPID, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem().is(ModCrossbows.RAPID) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.RAPID, new ResourceLocation("charged"), (stack, level, entity, seed) ->
				RapidCrossbowItem.looksLoaded(stack, entity) ? 1.0f : 0.0f);
	}

	/**
	 * The {@code pull} and {@code pulling} properties vanilla gives {@code minecraft:bow}, which pick the
	 * draw frame. {@code pull} is measured against this bow's own draw time (and any Quick Nock head
	 * start), so every bow reaches its last frame when it's fully drawn.
	 */
	private static void registerBowAnimation(WeaponBowItem bow) {
		ItemProperties.register(bow, new ResourceLocation("pull"), (stack, level, entity, seed) ->
				entity == null || entity.getUseItem() != stack ? 0.0f
						: (entity.getTicksUsingItem() + BowState.headStart(entity)) / (float) bow.drawTicks(entity));
		ItemProperties.register(bow, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
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
