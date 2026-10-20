package net.firstwemine1.weaponry.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Weaponry.MOD_ID);
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Weaponry.MOD_ID);

	private static final Map<WeaponType, Map<WeaponMaterial, RegistryObject<WeaponItem>>> WEAPONS = new EnumMap<>(WeaponType.class);
	private static final List<RegistryObject<WeaponItem>> ALL = new ArrayList<>();

	public static final String CREATIVE_TAB_KEY = "itemGroup." + Weaponry.MOD_ID;

	static {
		for (WeaponType type : WeaponType.values()) {
			Map<WeaponMaterial, RegistryObject<WeaponItem>> byMaterial = new EnumMap<>(WeaponMaterial.class);
			for (WeaponMaterial material : type.materials()) {
				RegistryObject<WeaponItem> item = ITEMS.register(itemId(type, material),
						() -> new WeaponItem(type, material, new Item.Properties()));
				byMaterial.put(material, item);
				ALL.add(item);
			}
			WEAPONS.put(type, byMaterial);
		}
		TABS.register("weapons", () -> CreativeModeTab.builder()
				.title(Component.translatable(CREATIVE_TAB_KEY))
				.icon(() -> get(WeaponType.HALBERD, WeaponMaterial.IRON).getDefaultInstance())
				.displayItems((parameters, output) -> all().forEach(output::accept))
				.build());
	}

	private ModItems() {
	}

	public static void register(IEventBus modBus) {
		ITEMS.register(modBus);
		TABS.register(modBus);
	}

	public static String itemId(WeaponType type, WeaponMaterial material) {
		return type.singleVersion() ? type.id() : material.id() + "_" + type.id();
	}

	/** The weapon of {@code type} in {@code material}, or {@code null} if the type doesn't come in it. */
	public static WeaponItem get(WeaponType type, WeaponMaterial material) {
		RegistryObject<WeaponItem> item = WEAPONS.get(type).get(material);
		return item != null ? item.get() : null;
	}

	/** Every version of {@code type}, one per material it comes in. Only valid once items are registered. */
	public static List<WeaponItem> variants(WeaponType type) {
		List<WeaponItem> items = new ArrayList<>();
		WEAPONS.get(type).values().forEach(item -> items.add(item.get()));
		return items;
	}

	/** Every weapon, ordered by type then material. Only valid once items are registered. */
	public static List<WeaponItem> all() {
		List<WeaponItem> items = new ArrayList<>(ALL.size());
		ALL.forEach(item -> items.add(item.get()));
		return items;
	}
}
