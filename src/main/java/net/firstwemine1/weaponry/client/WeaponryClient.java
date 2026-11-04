package net.firstwemine1.weaponry.client;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CrossbowItem;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.crossbow.HeavyCrossbowItem;
import net.firstwemine1.weaponry.client.crossbow.RapidCrossbowClient;
import net.firstwemine1.weaponry.ability.FlailAbility;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.bow.BowAbility;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup: renderers for ability entities, the flipped war pick model, the flail's wind-up frames, the bows' draw
 * animation, zoom and walk speed, wrapped tooltips, and the Stagger and Daze screen wobble.
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
	/** Vanilla's bow zoom at full draw. */
	private static final float FULL_DRAW_ZOOM = 0.15f;

	private WeaponryClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
			event.registerEntityRenderer(ModEntities.THROWN_WEAPON.get(), ThrownWeaponRenderer::new);
			event.registerEntityRenderer(ModEntities.ROPE_DART.get(), RopeDartRenderer::new);
		});
		// Item properties aren't thread-safe, so they go on the main thread.
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(WeaponryClient::registerItemProperties));
		modBus.addListener((ModelEvent.RegisterAdditional event) -> event.register(RopeDartRenderer.HEAD_MODEL));
		MinecraftForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> TooltipLines.wrap(event.getItemStack(), event.getToolTip()));
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				wobble(Minecraft.getInstance().player);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				RapidCrossbowClient.tick(Minecraft.getInstance());
			}
		});
		MinecraftForge.EVENT_BUS.addListener(RapidCrossbowClient::onClick);
		MinecraftForge.EVENT_BUS.addListener(WeaponryClient::bowZoom);
		MinecraftForge.EVENT_BUS.addListener(WeaponryClient::bowWalkSpeed);
	}

	private static void registerItemProperties() {
		for (WeaponItem item : ModItems.variants(WeaponType.WAR_PICK)) {
			ItemProperties.register(item, WarPickAbility.FLIPPED_ID,
					(stack, level, entity, seed) -> WarPickAbility.isFlipped(stack) ? 1 : 0);
		}
		for (WeaponItem item : ModItems.variants(WeaponType.FLAIL)) {
			ItemProperties.register(item, FlailAbility.SPIN_ID,
					(stack, level, entity, seed) -> FlailAbility.spinFrame(stack, entity));
		}
		ModBows.all().forEach(WeaponryClient::registerBowAnimation);
		registerHeavyCrossbowAnimation();
		registerRapidCrossbowAnimation();
	}

	/** The properties vanilla gives {@code minecraft:crossbow}, on this crossbow's own load time. */
	private static void registerHeavyCrossbowAnimation() {
		Item heavy = ModCrossbows.HEAVY.get();
		ItemProperties.register(heavy, new ResourceLocation("pull"), (stack, level, entity, seed) ->
				entity == null || CrossbowItem.isCharged(stack) ? 0.0f
						: (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / (float) HeavyCrossbowItem.chargeTicks(stack));
		ItemProperties.register(heavy, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack && !CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(heavy, new ResourceLocation("charged"), (stack, level, entity, seed) ->
				CrossbowItem.isCharged(stack) ? 1.0f : 0.0f);
		ItemProperties.register(heavy, new ResourceLocation("firework"), (stack, level, entity, seed) ->
				CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET) ? 1.0f : 0.0f);
	}

	/** While reloading, {@code pull} shows how full the magazine is, so the string draws back as it fills. */
	private static void registerRapidCrossbowAnimation() {
		Item rapid = ModCrossbows.RAPID.get();
		ItemProperties.register(rapid, new ResourceLocation("pull"), (stack, level, entity, seed) ->
				RapidCrossbowItem.magazine(stack).count() / (float) Magazine.CAPACITY);
		ItemProperties.register(rapid, new ResourceLocation("pulling"), (stack, level, entity, seed) ->
				entity != null && entity.isUsingItem() && entity.getUseItem().is(rapid) ? 1.0f : 0.0f);
		ItemProperties.register(rapid, new ResourceLocation("charged"), (stack, level, entity, seed) ->
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
	 * Draw zoom: vanilla only zooms the view while drawing {@code minecraft:bow}. Our bows zoom the same
	 * way over their own draw time, plus any extra zoom from their ability (the longbow's Steady Aim).
	 */
	private static void bowZoom(ComputeFovModifierEvent event) {
		Player player = event.getPlayer();
		WeaponBowItem bow = player.isUsingItem() ? ModBows.of(player.getUseItem()) : null;
		float scale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
		if (bow == null || scale <= 0) {
			return;
		}
		int drawTicks = bow.drawTicks(player);
		int drawn = player.getTicksUsingItem() + BowState.headStart(player);
		float pull = drawn / (float) drawTicks;
		float g = pull >= 1 ? 1 : pull * pull;
		float zoom = 1 - g * FULL_DRAW_ZOOM - bow.type().ability().extraZoom(WeaponBowItem.ticksAtFullDraw(drawn, drawTicks));
		// The new modifier is lerp(scale, 1, f): undo it, apply the zoom to f, and redo it.
		float f = (event.getNewFovModifier() - 1) / scale + 1;
		event.setNewFovModifier(1 + scale * (f * zoom - 1));
	}

	/**
	 * Light Draw: right after this event vanilla slows a player using an item to 20% (unless riding). A
	 * bow can set its own walk speed while drawing, so scale the input to end up there instead.
	 */
	private static void bowWalkSpeed(MovementInputUpdateEvent event) {
		Player player = event.getEntity();
		WeaponBowItem bow = player.isUsingItem() && !player.isPassenger() ? ModBows.of(player.getUseItem()) : null;
		if (bow == null) {
			return;
		}
		Input input = event.getInput();
		float factor = bow.type().ability().walkSpeedWhileDrawing() / BowAbility.VANILLA_WALK_SPEED;
		input.leftImpulse *= factor;
		input.forwardImpulse *= factor;
	}

	/**
	 * Vanilla only wobbles the view for Nausea with over 3 s left, so these short effects would never
	 * show. After the player's tick, wind the wobble up while affected; vanilla winds it back down.
	 */
	private static void wobble(LocalPlayer player) {
		if (player == null) {
			return;
		}
		if (player.hasEffect(ModEffects.STAGGER.get())) {
			windUp(player, STAGGER_WOBBLE, STAGGER_WOBBLE_PER_TICK);
		} else if (player.hasEffect(ModEffects.DAZE.get())) {
			windUp(player, DAZE_WOBBLE, DAZE_WOBBLE_PER_TICK);
		}
	}

	private static void windUp(LocalPlayer player, float max, float perTick) {
		player.spinningEffectIntensity = Math.max(player.spinningEffectIntensity,
				Math.min(max, player.oSpinningEffectIntensity + perTick));
	}
}
