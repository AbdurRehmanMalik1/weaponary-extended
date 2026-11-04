package net.firstwemine1.weaponry.crossbow;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The arrows in a rapid crossbow: how many, and which type (one type at a time). */
public record Magazine(int count, Item arrow) {
	public static final int CAPACITY = 64;
	public static final Magazine EMPTY = new Magazine(0, Items.ARROW);
	public static final Codec<Magazine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("count").forGetter(Magazine::count),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("arrow").forGetter(Magazine::arrow)
	).apply(instance, Magazine::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, Magazine> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Magazine::count,
			ByteBufCodecs.registry(Registries.ITEM), Magazine::arrow,
			Magazine::new);

	public boolean isEmpty() {
		return count <= 0;
	}

	public boolean isFull() {
		return count >= CAPACITY;
	}
}
