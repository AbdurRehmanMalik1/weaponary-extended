package net.firstwemine1.weaponry.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.bow.ModBows;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

public final class ModItems {
	private static final Map<WeaponType, Map<WeaponMaterial, WeaponItem>> WEAPONS = new EnumMap<>(WeaponType.class);
	private static final List<WeaponItem> ALL = new ArrayList<>();

	public static final String CREATIVE_TAB_KEY = "itemGroup." + Weaponry.MOD_ID;

	private ModItems() {
	}

	public static void register() {
		for (WeaponType type : WeaponType.values()) {
			Map<WeaponMaterial, WeaponItem> byMaterial = new EnumMap<>(WeaponMaterial.class);
			for (WeaponMaterial material : type.materials()) {
				ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Weaponry.id(itemId(type, material)));
				WeaponItem item = new WeaponItem(type, material, new Item.Properties().setId(key));
				Registry.register(BuiltInRegistries.ITEM, key, item);
				byMaterial.put(material, item);
				ALL.add(item);
			}
			WEAPONS.put(type, byMaterial);
		}

		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.title(Component.translatable(CREATIVE_TAB_KEY))
				.icon(() -> get(WeaponType.HALBERD, WeaponMaterial.IRON).getDefaultInstance())
				.displayItems((parameters, output) -> {
					ALL.forEach(output::accept);
					ModBows.all().forEach(output::accept);
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Weaponry.id("weapons"), tab);
	}

	public static String itemId(WeaponType type, WeaponMaterial material) {
		return type.singleVersion() ? type.id() : material.id() + "_" + type.id();
	}

	/** The weapon of {@code type} in {@code material}, or {@code null} if the type doesn't come in it. */
	public static WeaponItem get(WeaponType type, WeaponMaterial material) {
		return WEAPONS.get(type).get(material);
	}

	/** Every version of {@code type}, one per material it comes in. */
	public static Collection<WeaponItem> variants(WeaponType type) {
		return Collections.unmodifiableCollection(WEAPONS.get(type).values());
	}

	/** Every weapon, ordered by type then material. */
	public static List<WeaponItem> all() {
		return Collections.unmodifiableList(ALL);
	}
}
