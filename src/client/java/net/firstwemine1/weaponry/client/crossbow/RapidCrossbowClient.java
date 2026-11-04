package net.firstwemine1.weaponry.client.crossbow;

import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.RapidFiring;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

/** Gatling, on the client: tells the server when attack starts and stops being held with a rapid crossbow. */
public final class RapidCrossbowClient {
	private static boolean firing;

	private RapidCrossbowClient() {
	}

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		boolean wanted = player != null && client.screen == null && !player.isUsingItem() && !player.isSpectator()
				&& player.getMainHandItem().is(ModCrossbows.RAPID) && client.options.keyAttack.isDown();
		if (wanted != firing) {
			firing = wanted;
			if (ClientPlayNetworking.canSend(RapidFiring.PACKET)) {
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeBoolean(wanted);
				ClientPlayNetworking.send(RapidFiring.PACKET, buf);
			}
		}
	}
}
