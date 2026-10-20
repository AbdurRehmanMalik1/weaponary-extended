package net.firstwemine1.weaponry.bow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The four bows, one item each ({@code weaponry:selfbow}, {@code weaponry:longbow}, ...). */
public final class ModBows {
	private static final Map<BowType, WeaponBowItem> BOWS = new EnumMap<>(BowType.class);
	private static final List<WeaponBowItem> ALL = new ArrayList<>();

	private ModBows() {
	}

	/** After {@link ModItems#register()}: the bows join its creative tab. */
	public static void register() {
		for (BowType type : BowType.values()) {
			WeaponBowItem item = new WeaponBowItem(type, new Item.Properties().tab(ModItems.tab()));
			Registry.register(Registry.ITEM, Weaponry.id(type.id()), item);
			BOWS.put(type, item);
			ALL.add(item);
		}
	}

	public static WeaponBowItem get(BowType type) {
		return BOWS.get(type);
	}

	public static List<WeaponBowItem> all() {
		return Collections.unmodifiableList(ALL);
	}

	/** The bow in {@code stack}, or {@code null} if it isn't one of ours. */
	public static WeaponBowItem of(ItemStack stack) {
		return stack.getItem() instanceof WeaponBowItem bow ? bow : null;
	}
}
