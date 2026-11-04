package net.firstwemine1.weaponry.crossbow;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Gatling, on the server. The attack key never reaches the server, so the client says when it's held
 * ({@link RapidFiringPayload}) and the server fires on its own timer: the client only chooses whether
 * to fire, and sending the packet faster can't raise the rate. Everything here runs on the server thread.
 */
public final class RapidFiring {
	/** 5 arrows per second. */
	public static final int FIRE_INTERVAL = 4;
	private static final Set<ServerPlayer> FIRING = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<ServerPlayer, Integer> LAST_SHOT = new WeakHashMap<>();

	private RapidFiring() {
	}

	static void register(IEventBus modBus) {
		modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1").optional().playToServer(RapidFiringPayload.TYPE, RapidFiringPayload.CODEC,
				(payload, context) -> setFiring((ServerPlayer) context.player(), payload.firing())));
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tick(event.getServer()));
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
			if (player.isRemoved() || !stack.is(ModCrossbows.RAPID.get())) {
				it.remove();
				continue;
			}
			Integer last = LAST_SHOT.get(player);
			boolean ready = last == null || server.getTickCount() - last >= FIRE_INTERVAL;
			if (ready && !player.isUsingItem() && !player.isSpectator() && player.isAlive()
					&& ModCrossbows.RAPID.get().fire((ServerLevel) player.level(), player, stack)) {
				LAST_SHOT.put(player, server.getTickCount());
			}
		}
	}
}
