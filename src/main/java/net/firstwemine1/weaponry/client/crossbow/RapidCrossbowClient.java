package net.firstwemine1.weaponry.client.crossbow;

import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.RapidFiringPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Gatling, on the client: tells the server when attack starts and stops being held with a rapid crossbow. */
public final class RapidCrossbowClient {
	private static boolean firing;

	private RapidCrossbowClient() {
	}

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		boolean wanted = player != null && client.screen == null && !player.isUsingItem() && !player.isSpectator()
				&& player.getMainHandItem().is(ModCrossbows.RAPID.get()) && client.options.keyAttack.isDown();
		if (wanted != firing) {
			firing = wanted;
			if (client.getConnection() != null && client.getConnection().hasChannel(RapidFiringPayload.TYPE)) {
				ClientPacketDistributor.sendToServer(new RapidFiringPayload(wanted));
			}
		}
	}
}
