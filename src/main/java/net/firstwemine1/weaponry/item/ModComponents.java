package net.firstwemine1.weaponry.item;

import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components the mod adds. */
public final class ModComponents {
	private static final DeferredRegister.DataComponents COMPONENTS =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Weaponry.MOD_ID);

	/** Id of {@link #FLIPPED}, and of the item model property the client derives from it. */
	public static final ResourceLocation FLIPPED_ID = Weaponry.id("flipped");
	/** Present while a war pick is held the other way round, pick first (it mines). */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> FLIPPED = COMPONENTS.registerComponentType(
			FLIPPED_ID.getPath(), builder -> builder
					.persistent(Unit.CODEC)
					.networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

	/** The arrows in a rapid crossbow. */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Magazine>> MAGAZINE = COMPONENTS.registerComponentType(
			"magazine", builder -> builder.persistent(Magazine.CODEC).networkSynchronized(Magazine.STREAM_CODEC));

	private ModComponents() {
	}

	public static void register(IEventBus modBus) {
		COMPONENTS.register(modBus);
	}
}
