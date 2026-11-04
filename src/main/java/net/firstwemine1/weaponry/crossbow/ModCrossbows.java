package net.firstwemine1.weaponry.crossbow;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The two crossbows. Both are single items, with no material tiers. */
public final class ModCrossbows {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Weaponry.MOD_ID);
	public static final RegistryObject<HeavyCrossbowItem> HEAVY = ITEMS.register("heavy_crossbow", () -> new HeavyCrossbowItem(new Item.Properties().tab(ModItems.TAB)));
	public static final RegistryObject<RapidCrossbowItem> RAPID = ITEMS.register("rapid_crossbow", () -> new RapidCrossbowItem(new Item.Properties().tab(ModItems.TAB)));

	private ModCrossbows() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		HeavyCrossbowItem.registerSounds(modBus);
		RapidFiring.register();
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
