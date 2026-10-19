package net.firstwemine1.weaponry.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Vanilla tags that make weapons enchantable and behave like melee weapons (and the hatchet and war pick like mining tools). */
public class WeaponItemTagProvider extends FabricTagProvider.ItemTagProvider {
	public WeaponItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// The old enchantable/sword tag is split; melee_weapon is its non-sweep half (knockback, looting, ...).
		addAll(ItemTags.MELEE_WEAPON_ENCHANTABLE);
		addAll(ItemTags.SHARP_WEAPON_ENCHANTABLE);
		addAll(ItemTags.WEAPON_ENCHANTABLE);
		addAll(ItemTags.FIRE_ASPECT_ENCHANTABLE);
		addAll(ItemTags.DURABILITY_ENCHANTABLE);
		addAll(ItemTags.BREAKS_DECORATED_POTS);

		// The hatchet chops wood and the flipped war pick mines, so they take Efficiency, Fortune and
		// Silk Touch like an axe or pickaxe.
		for (TagKey<Item> tag : List.of(ItemTags.MINING_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE)) {
			var builder = valueLookupBuilder(tag);
			for (WeaponType type : List.of(WeaponType.HATCHET, WeaponType.WAR_PICK)) {
				for (WeaponMaterial material : WeaponMaterial.values()) {
					builder.add(ModItems.get(type, material));
				}
			}
		}
	}

	private void addAll(TagKey<Item> tag) {
		var builder = valueLookupBuilder(tag);
		ModItems.all().forEach(builder::add);
	}
}
