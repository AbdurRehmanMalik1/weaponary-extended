package net.firstwemine1.weaponry.crossbow;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The two crossbows, one item each ({@code weaponry:heavy_crossbow}, {@code weaponry:rapid_crossbow}). */
public final class ModCrossbows {
	public static final HeavyCrossbowItem HEAVY = new HeavyCrossbowItem(properties("heavy_crossbow"));
	public static final RapidCrossbowItem RAPID = new RapidCrossbowItem(properties("rapid_crossbow"));
	private static final List<Item> ALL = List.of(HEAVY, RAPID);

	private ModCrossbows() {
	}

	/** Items need their id set before they are built. */
	private static Item.Properties properties(String id) {
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Weaponry.id(id)));
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, Weaponry.id(HEAVY.id()), HEAVY);
		Registry.register(BuiltInRegistries.ITEM, Weaponry.id(RAPID.id()), RAPID);
		HeavyCrossbowItem.registerSounds();
		RapidFiring.register();
	}

	public static List<Item> all() {
		return ALL;
	}

	public static boolean isCrossbow(ItemStack stack) {
		return stack.getItem() instanceof WeaponCrossbow;
	}

	public static String tooltipKey(WeaponCrossbow crossbow, int line) {
		return "tooltip." + Weaponry.MOD_ID + "." + crossbow.id() + "." + line;
	}
}
