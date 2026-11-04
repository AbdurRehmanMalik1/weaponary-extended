package net.firstwemine1.weaponry.client;

import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.core.component.DataComponents;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.client.crossbow.RapidCrossbowClient;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.ability.FlailAbility;
import net.firstwemine1.weaponry.bow.BowState;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-only setup: renderers for ability entities, the flipped war pick model, the flail's wind-up frames, the bows' draw
 * animation, wrapped tooltips, and the Stagger and Daze screen wobble.
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
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			registerHeavyCrossbowAnimation();
			registerRapidCrossbowAnimation();
		}));
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
			event.registerEntityRenderer(ModEntities.THROWN_WEAPON.get(), ThrownWeaponRenderer::new);
			event.registerEntityRenderer(ModEntities.ROPE_DART.get(), RopeDartRenderer::new);
		});
		// The item property map isn't thread-safe, so register on the main thread.
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			for (WeaponItem item : ModItems.variants(WeaponType.WAR_PICK)) {
				ItemProperties.register(item, ModComponents.FLIPPED_ID,
						(stack, level, entity, seed) -> stack.has(ModComponents.FLIPPED.get()) ? 1 : 0);
			}
			for (WeaponItem item : ModItems.variants(WeaponType.FLAIL)) {
				ItemProperties.register(item, FlailAbility.SPIN_ID,
						(stack, level, entity, seed) -> FlailAbility.spinFrame(stack, entity));
			}
			ModBows.all().forEach(WeaponryClient::registerBowAnimation);
		}));
		modBus.addListener((ModelEvent.RegisterAdditional event) -> event.register(RopeDartRenderer.HEAD_MODEL));
		NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> TooltipLines.wrap(event.getItemStack(), event.getToolTip()));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> wobble(Minecraft.getInstance().player));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> RapidCrossbowClient.tick(Minecraft.getInstance()));
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
	 * The properties vanilla gives {@code minecraft:crossbow}. {@code getChargeDuration} already gives
	 * this crossbow's own load time, so the frames switch at the right moments.
	 */
	private static void registerHeavyCrossbowAnimation() {
		ItemProperties.register(ModCrossbows.HEAVY.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
				entity == null || CrossbowItem.isCharged(stack) ? 0.0f
						: (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float) CrossbowItem.getChargeDuration(stack, entity));
		ItemProperties.register(ModCrossbows.HEAVY.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.HEAVY.get(), ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) ->
				CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.HEAVY.get(), ResourceLocation.withDefaultNamespace("firework"), (stack, level, entity, seed) -> {
			ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
			return projectiles != null && projectiles.contains(Items.FIREWORK_ROCKET) ? 1.0f : 0.0f;
		});
	}

	/** While reloading, {@code pull} shows how full the magazine is, so the string draws back as it fills. */
	private static void registerRapidCrossbowAnimation() {
		ItemProperties.register(ModCrossbows.RAPID.get(), ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) ->
				RapidCrossbowItem.magazine(stack).count() / (float) Magazine.CAPACITY);
		ItemProperties.register(ModCrossbows.RAPID.get(), ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem().is(ModCrossbows.RAPID.get()) ? 1.0f : 0.0f);
		ItemProperties.register(ModCrossbows.RAPID.get(), ResourceLocation.withDefaultNamespace("charged"), (stack, level, entity, seed) ->
				RapidCrossbowItem.looksLoaded(stack, entity) ? 1.0f : 0.0f);
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
