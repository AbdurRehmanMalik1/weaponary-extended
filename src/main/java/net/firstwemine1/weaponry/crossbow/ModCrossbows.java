package net.firstwemine1.weaponry.crossbow;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The two crossbows. Both are single items, with no material tiers. */
public final class ModCrossbows {
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Weaponry.MOD_ID);
	public static final DeferredItem<HeavyCrossbowItem> HEAVY = ITEMS.registerItem("heavy_crossbow", HeavyCrossbowItem::new);
	public static final DeferredItem<RapidCrossbowItem> RAPID = ITEMS.registerItem("rapid_crossbow", RapidCrossbowItem::new);

	private ModCrossbows() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		HeavyCrossbowItem.registerSounds(modBus);
		RapidFiring.register(modBus);
	}

	/** Only valid once items are registered. */
	public static List<Item> all() {
		return List.of(HEAVY.get(), RAPID.get());
	}

	public static boolean isCrossbow(ItemStack stack) {
		return stack.getItem() instanceof WeaponCrossbow;
	}

	public static String tooltipKey(WeaponCrossbow crossbow, int line) {
		return "tooltip." + Weaponry.MOD_ID + "." + crossbow.id() + "." + line;
	}
}
