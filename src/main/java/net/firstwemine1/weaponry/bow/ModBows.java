package net.firstwemine1.weaponry.bow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The four bows, one item each ({@code weaponry:selfbow}, {@code weaponry:longbow}, ...). */
public final class ModBows {
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Weaponry.MOD_ID);

	private static final Map<BowType, DeferredItem<WeaponBowItem>> BOWS = new EnumMap<>(BowType.class);
	private static final List<DeferredItem<WeaponBowItem>> ALL = new ArrayList<>();

	static {
		for (BowType type : BowType.values()) {
			DeferredItem<WeaponBowItem> item = ITEMS.registerItem(type.id(),
					properties -> new WeaponBowItem(type, properties), new Item.Properties());
			BOWS.put(type, item);
			ALL.add(item);
		}
	}

	private ModBows() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
	}

	public static WeaponBowItem get(BowType type) {
		return BOWS.get(type).get();
	}

	/** Every bow, in {@link BowType} order. Only valid once items are registered. */
	public static List<WeaponBowItem> all() {
		return ALL.stream().map(DeferredItem::get).toList();
	}

	/** The bow in {@code stack}, or {@code null} if it isn't one of ours. */
	public static WeaponBowItem of(ItemStack stack) {
		return stack.getItem() instanceof WeaponBowItem bow ? bow : null;
	}
}
