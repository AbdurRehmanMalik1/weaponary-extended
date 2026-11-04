package net.firstwemine1.weaponry.crossbow;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server, when the player starts or stops holding attack with a rapid crossbow. */
public record RapidFiringPayload(boolean firing) implements CustomPacketPayload {
	public static final Type<RapidFiringPayload> TYPE = new Type<>(Weaponry.id("rapid_firing"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RapidFiringPayload> CODEC =
			ByteBufCodecs.BOOL.<RegistryFriendlyByteBuf>cast().map(RapidFiringPayload::new, RapidFiringPayload::firing);

	@Override
	public Type<RapidFiringPayload> type() {
		return TYPE;
	}
}
