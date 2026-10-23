package net.firstwemine1.weaponry.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Weaponry.MOD_ID);

	private static final Map<WeaponType, Map<WeaponMaterial, RegistryObject<WeaponItem>>> WEAPONS = new EnumMap<>(WeaponType.class);
	private static final List<RegistryObject<WeaponItem>> ALL = new ArrayList<>();

	/** The tab's label is {@code weaponry.weapons}, so its name key is {@code itemGroup.weaponry.weapons}. */
	public static final String CREATIVE_TAB_KEY = "itemGroup." + Weaponry.MOD_ID + ".weapons";

	/** Creative tabs aren't registered in this version: items join it through {@code Item.Properties.tab}. */
	public static final ItemGroup TAB = new ItemGroup(Weaponry.MOD_ID + ".weapons") {
		@Override
		public ItemStack makeIcon() {
			return get(WeaponType.HALBERD, WeaponMaterial.IRON).getDefaultInstance();
		}
	};

	static {
		for (WeaponType type : WeaponType.values()) {
			Map<WeaponMaterial, RegistryObject<WeaponItem>> byMaterial = new EnumMap<>(WeaponMaterial.class);
			for (WeaponMaterial material : type.materials()) {
				RegistryObject<WeaponItem> item = ITEMS.register(itemId(type, material),
						() -> new WeaponItem(type, material, new Item.Properties().tab(TAB)));
				byMaterial.put(material, item);
				ALL.add(item);
			}
			WEAPONS.put(type, byMaterial);
		}
	}

	private ModItems() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
	}

	public static String itemId(WeaponType type, WeaponMaterial material) {
		return type.singleVersion() ? type.id() : material.id() + "_" + type.id();
	}

	/** The weapon of {@code type} in {@code material}, or {@code null} if the type doesn't come in it. */
	public static WeaponItem get(WeaponType type, WeaponMaterial material) {
		RegistryObject<WeaponItem> item = WEAPONS.get(type).get(material);
		return item == null ? null : item.get();
	}

	/** Every version of {@code type}, one per material it comes in. Only valid once items are registered. */
	public static Collection<WeaponItem> variants(WeaponType type) {
		List<WeaponItem> items = new ArrayList<>();
		for (RegistryObject<WeaponItem> item : WEAPONS.get(type).values()) {
			items.add(item.get());
		}
		return items;
	}

	/** Every weapon, ordered by type then material. Only valid once items are registered. */
	public static List<WeaponItem> all() {
		List<WeaponItem> items = new ArrayList<>(ALL.size());
		for (RegistryObject<WeaponItem> item : ALL) {
			items.add(item.get());
		}
		return items;
	}
}
