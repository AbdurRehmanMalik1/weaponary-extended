package net.firstwemine1.weaponry.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Vanilla tags that make weapons enchantable and behave like melee weapons (and the hatchet and war pick like mining tools). */
public class WeaponItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	public WeaponItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Sword enchantments minus Sweeping Edge (#enchantable/sweeping): weapons don't sweep.
		addAll(ItemTags.MELEE_WEAPON_ENCHANTABLE);
		addAll(ItemTags.SHARP_WEAPON_ENCHANTABLE);
		addAll(ItemTags.WEAPON_ENCHANTABLE);
		addAll(ItemTags.FIRE_ASPECT_ENCHANTABLE);
		addAll(ItemTags.DURABILITY_ENCHANTABLE);
		addAll(ItemTags.BREAKS_DECORATED_POTS);

		// The bows take the same enchantments as vanilla's: Power, Punch, Flame, Infinity, Unbreaking, Mending.
		addBows(ItemTags.BOW_ENCHANTABLE);
		addBows(ItemTags.DURABILITY_ENCHANTABLE);
		addBows(ItemTags.VANISHING_ENCHANTABLE);

		// The hatchet chops wood and the flipped war pick mines, so they take Efficiency, Fortune and
		// Silk Touch like an axe or pickaxe.
		for (TagKey<Item> tag : List.of(ItemTags.MINING_ENCHANTABLE, ItemTags.MINING_LOOT_ENCHANTABLE)) {
			var builder = builder(tag);
			for (WeaponType type : List.of(WeaponType.HATCHET, WeaponType.WAR_PICK)) {
				ModItems.variants(type).forEach(item -> builder.add(item.builtInRegistryHolder().key()));
			}
		}
	}

	private void addBows(TagKey<Item> tag) {
		var builder = builder(tag);
		ModBows.all().forEach(bow -> builder.add(bow.builtInRegistryHolder().key()));
	}

	private void addAll(TagKey<Item> tag) {
		var builder = builder(tag);
		ModItems.all().forEach(item -> builder.add(item.builtInRegistryHolder().key()));
	}
}
