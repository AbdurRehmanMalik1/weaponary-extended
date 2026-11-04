package net.firstwemine1.weaponry.crossbow;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/**
 * Gatling, on the server. The attack key never reaches the server, so the client says when it's held
 * ({@link #CHANNEL}) and the server fires on its own timer: the client only chooses whether
 * to fire, and sending the packet faster can't raise the rate. Everything here runs on the server thread.
 */
public final class RapidFiring {
	private static final String PROTOCOL = "1";
	/** Client to server: holding attack with a rapid crossbow started (true) or stopped (false). Optional on both sides. */
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Weaponry.id("rapid_firing"), () -> PROTOCOL,
			NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));
	/** 5 arrows per second. */
	public static final int FIRE_INTERVAL = 4;
	private static final Set<ServerPlayerEntity> FIRING = Collections.newSetFromMap(new WeakHashMap<>());
	private static final Map<ServerPlayerEntity, Integer> LAST_SHOT = new WeakHashMap<>();

	private RapidFiring() {
	}

	static void register() {
		CHANNEL.registerMessage(0, Boolean.class, (firing, buf) -> buf.writeBoolean(firing), PacketBuffer::readBoolean, (firing, context) -> {
			ServerPlayerEntity player = context.get().getSender();
			context.get().enqueueWork(() -> {
				if (player != null) {
					setFiring(player, firing);
				}
			});
			context.get().setPacketHandled(true);
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				tick(ServerLifecycleHooks.getCurrentServer());
			}
		});
		MinecraftForge.EVENT_BUS.addListener(RapidFiring::rapidHits);
	}

	/** Rapid Hits: its arrows clear the hurt cooldown, so a stream of them all land. Fires after the cooldown is set. */
	private static void rapidHits(LivingDamageEvent event) {
		if (event.getSource().getDirectEntity() != null && RapidCrossbowItem.isRapidArrow(event.getSource().getDirectEntity())) {
			event.getEntityLiving().invulnerableTime = 0;
		}
	}

	public static void setFiring(ServerPlayerEntity player, boolean firing) {
		if (firing) {
			FIRING.add(player);
		} else {
			FIRING.remove(player);
		}
	}

	public static boolean isFiring(ServerPlayerEntity player) {
		return FIRING.contains(player);
	}

	private static void tick(MinecraftServer server) {
		for (Iterator<ServerPlayerEntity> it = FIRING.iterator(); it.hasNext(); ) {
			ServerPlayerEntity player = it.next();
			ItemStack stack = player.getMainHandItem();
			if (player.removed || stack.getItem() != ModCrossbows.RAPID.get()) {
				it.remove();
				continue;
			}
			Integer last = LAST_SHOT.get(player);
			boolean ready = last == null || server.getTickCount() - last >= FIRE_INTERVAL;
			if (ready && !player.isUsingItem() && !player.isSpectator() && player.isAlive()
					&& ModCrossbows.RAPID.get().fire(player.getLevel(), player, stack)) {
				LAST_SHOT.put(player, server.getTickCount());
			}
		}
	}
}
