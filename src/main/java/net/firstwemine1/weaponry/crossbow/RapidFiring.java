package net.firstwemine1.weaponry.crossbow;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Gatling, on the server. The attack key never reaches the server, so the client says when it's held
 * ({@link #PACKET}) and the server fires on its own timer: the client only chooses whether
 * to fire, and sending the packet faster can't raise the rate. Everything here runs on the server thread.
 */
public final class RapidFiring {
	/** Client to server: holding attack with a rapid crossbow started (true) or stopped (false). */
	public static final ResourceLocation PACKET = Weaponry.id("rapid_firing");
	/** 5 arrows per second. */
	public static final int FIRE_INTERVAL = 4;
	private static final Set<ServerPlayer> FIRING = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<ServerPlayer, Integer> LAST_SHOT = new WeakHashMap<>();

	private RapidFiring() {
	}

	static void register() {
		ServerPlayNetworking.registerGlobalReceiver(PACKET, (server, player, handler, buf, responseSender) -> {
			boolean firing = buf.readBoolean();
			server.execute(() -> setFiring(player, firing));
		});
		ServerTickEvents.END_SERVER_TICK.register(RapidFiring::tick);
	}

	public static void setFiring(ServerPlayer player, boolean firing) {
		if (firing) {
			FIRING.add(player);
		} else {
			FIRING.remove(player);
		}
	}

	public static boolean isFiring(ServerPlayer player) {
		return FIRING.contains(player);
	}

	private static void tick(MinecraftServer server) {
		for (Iterator<ServerPlayer> it = FIRING.iterator(); it.hasNext(); ) {
			ServerPlayer player = it.next();
			ItemStack stack = player.getMainHandItem();
			if (player.removed || stack.getItem() != ModCrossbows.RAPID) {
				it.remove();
				continue;
			}
			Integer last = LAST_SHOT.get(player);
			boolean ready = last == null || server.getTickCount() - last >= FIRE_INTERVAL;
			if (ready && !player.isUsingItem() && !player.isSpectator() && player.isAlive()
					&& ModCrossbows.RAPID.fire(player.getLevel(), player, stack)) {
				LAST_SHOT.put(player, server.getTickCount());
			}
		}
	}
}
