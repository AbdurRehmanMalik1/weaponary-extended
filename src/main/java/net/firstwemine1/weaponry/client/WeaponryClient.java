package net.firstwemine1.weaponry.client;

import net.firstwemine1.weaponry.client.render.RopeDartRenderer;
import net.firstwemine1.weaponry.client.render.ThrownWeaponRenderer;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.firstwemine1.weaponry.entity.SpawnPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Client-only setup: renderers and the spawn packet for ability entities, and the Stagger and Daze
 * screen wobble.
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
		EntityRendererRegistry.INSTANCE.register(ModEntities.THROWN_WEAPON, (dispatcher, context) -> new ThrownWeaponRenderer(dispatcher));
		EntityRendererRegistry.INSTANCE.register(ModEntities.ROPE_DART, (dispatcher, context) -> new RopeDartRenderer(dispatcher));
		ClientPlayNetworking.registerGlobalReceiver(SpawnPacket.ID, (client, handler, buf, sender) -> {
			SpawnPacket.Data data = SpawnPacket.read(buf);
			client.execute(() -> spawn(client, data));
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> wobble(client.player));
	}

	/** What {@code ClientPacketListener.handleAddEntity} does for a vanilla arrow. */
	private static void spawn(Minecraft client, SpawnPacket.Data data) {
		ClientLevel level = client.level;
		if (level == null || data.type == null) {
			return;
		}
		Entity entity = data.type.create(level);
		if (entity == null) {
			return;
		}
		entity.setPacketCoordinates(data.x, data.y, data.z);
		entity.moveTo(data.x, data.y, data.z, data.yRot, data.xRot);
		entity.setDeltaMovement(data.motion);
		entity.setId(data.id);
		entity.setUUID(data.uuid);
		Entity owner = level.getEntity(data.ownerId);
		if (owner != null && entity instanceof Projectile) {
			((Projectile) entity).setOwner(owner);
		}
		level.putNonPlayerEntity(data.id, entity);
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
		// The nausea wobble is still called portalTime in this version.
		player.portalTime = Math.max(player.portalTime, Math.min(max, player.oPortalTime + perTick));
	}
}
