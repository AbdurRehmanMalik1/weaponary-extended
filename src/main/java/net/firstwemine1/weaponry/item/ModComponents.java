package net.firstwemine1.weaponry.item;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components the mod adds. */
public final class ModComponents {
	private static final DeferredRegister.DataComponents COMPONENTS =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Weaponry.MOD_ID);

	/** Id of {@link #FLIPPED}. The war pick's item definition switches model on it ({@code has_component}). */
	public static final Identifier FLIPPED_ID = Weaponry.id("flipped");
	/** Present while a war pick is held the other way round, pick first (it mines). */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> FLIPPED = COMPONENTS.registerComponentType(
			FLIPPED_ID.getPath(), builder -> builder
					.persistent(Unit.CODEC)
					.networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

	private ModComponents() {
	}

	public static void register(IEventBus modBus) {
		COMPONENTS.register(modBus);
	}
}
