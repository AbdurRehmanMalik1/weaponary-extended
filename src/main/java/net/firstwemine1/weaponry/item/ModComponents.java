package net.firstwemine1.weaponry.item;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

/** Item data components the mod adds. */
public final class ModComponents {
	/** Id of {@link #FLIPPED}, and of the item model property the client derives from it. */
	public static final ResourceLocation FLIPPED_ID = Weaponry.id("flipped");
	/** Present while a war pick is held the other way round, pick first (it mines). */
	public static final DataComponentType<Unit> FLIPPED = DataComponentType.<Unit>builder()
			.persistent(Unit.CODEC)
			.networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
			.build();

	private ModComponents() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, FLIPPED_ID, FLIPPED);
	}
}
