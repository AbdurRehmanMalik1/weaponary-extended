package net.firstwemine1.weaponry.client;

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
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemModelsProperties;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup: renderers for ability entities, the flipped war pick model, the bows' draw
 * animation, zoom and walk speed, and the Stagger and Daze screen wobble.
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
		modBus.addListener((FMLClientSetupEvent event) -> {
			RenderingRegistry.registerEntityRenderingHandler(ModEntities.THROWN_WEAPON.get(), ThrownWeaponRenderer::new);
			RenderingRegistry.registerEntityRenderingHandler(ModEntities.ROPE_DART.get(), RopeDartRenderer::new);
			// The property registry is a plain map, so it is filled on the main thread.
			event.enqueueWork(WeaponryClient::registerItemProperties);
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				wobble(Minecraft.getInstance().player);
			}
		});
		MinecraftForge.EVENT_BUS.addListener(WeaponryClient::bowZoom);
		MinecraftForge.EVENT_BUS.addListener(WeaponryClient::bowWalkSpeed);
	}

	private static void registerItemProperties() {
		for (WeaponItem item : ModItems.variants(WeaponType.WAR_PICK)) {
			ItemModelsProperties.register(item, WarPickAbility.FLIPPED_ID,
					(stack, level, entity) -> WarPickAbility.isFlipped(stack) ? 1 : 0);
		}
		ModBows.all().forEach(WeaponryClient::registerBowAnimation);
	}

	/**
	 * The {@code pull} and {@code pulling} properties vanilla gives {@code minecraft:bow}, which pick the
	 * draw frame. {@code pull} is measured against this bow's own draw time (and any Quick Nock head
	 * start), so every bow reaches its last frame when it's fully drawn.
	 */
	private static void registerBowAnimation(WeaponBowItem bow) {
		ItemModelsProperties.register(bow, new ResourceLocation("pull"), (stack, level, entity) ->
				entity == null || entity.getUseItem() != stack ? 0.0f
						: (entity.getTicksUsingItem() + BowState.headStart(entity)) / (float) bow.drawTicks(entity));
		ItemModelsProperties.register(bow, new ResourceLocation("pulling"), (stack, level, entity) ->
				entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
	}

	/**
	 * Draw zoom: vanilla only zooms the view while drawing {@code minecraft:bow}. Our bows zoom the same
	 * way over their own draw time, plus any extra zoom from their ability (the longbow's Steady Aim).
	 */
	private static void bowZoom(FOVUpdateEvent event) {
		PlayerEntity player = event.getEntity();
		WeaponBowItem bow = player.isUsingItem() ? ModBows.of(player.getUseItem()) : null;
		float scale = Minecraft.getInstance().options.fovEffectScale;
		if (bow == null || scale <= 0) {
			return;
		}
		int drawTicks = bow.drawTicks(player);
		int drawn = player.getTicksUsingItem() + BowState.headStart(player);
		float pull = drawn / (float) drawTicks;
		float g = pull >= 1 ? 1 : pull * pull;
		float zoom = 1 - g * FULL_DRAW_ZOOM - bow.type().ability().extraZoom(WeaponBowItem.ticksAtFullDraw(drawn, drawTicks));
		// The new modifier is lerp(scale, 1, f): undo it, apply the zoom to f, and redo it.
		float f = (event.getNewfov() - 1) / scale + 1;
		event.setNewfov(1 + scale * (f * zoom - 1));
	}

	/**
	 * Light Draw: right after this event vanilla slows a player using an item to 20% (unless riding). A
	 * bow can set its own walk speed while drawing, so scale the input to end up there instead.
	 */
	private static void bowWalkSpeed(InputUpdateEvent event) {
		PlayerEntity player = event.getPlayer();
		WeaponBowItem bow = player.isUsingItem() && !player.isPassenger() ? ModBows.of(player.getUseItem()) : null;
		if (bow == null) {
			return;
		}
		MovementInput input = event.getMovementInput();
		float factor = bow.type().ability().walkSpeedWhileDrawing() / BowAbility.VANILLA_WALK_SPEED;
		input.leftImpulse *= factor;
		input.forwardImpulse *= factor;
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
