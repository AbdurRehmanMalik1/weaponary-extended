package net.firstwemine1.weaponry.bow;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** The four bows, one item each ({@code weaponry:selfbow}, {@code weaponry:longbow}, ...). */
public final class ModBows {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Weaponry.MOD_ID);

	private static final Map<BowType, RegistryObject<WeaponBowItem>> BOWS = new EnumMap<>(BowType.class);

	static {
		for (BowType type : BowType.values()) {
			BOWS.put(type, ITEMS.register(type.id(), () -> new WeaponBowItem(type, new Item.Properties().tab(ModItems.TAB))));
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
		List<WeaponBowItem> bows = new ArrayList<>(BOWS.size());
		BOWS.values().forEach(bow -> bows.add(bow.get()));
		return bows;
	}

	/** The bow in {@code stack}, or {@code null} if it isn't one of ours. */
	public static WeaponBowItem of(ItemStack stack) {
		return stack.getItem() instanceof WeaponBowItem ? (WeaponBowItem) stack.getItem() : null;
	}
}
