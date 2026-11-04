package net.firstwemine1.weaponry.datagen;

import java.util.concurrent.CompletableFuture;

import net.firstwemine1.weaponry.bow.BowType;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Crafting recipes. M = tier material, S = stick, C = chain, X = string, L = leather. Shapes avoid every vanilla tool/weapon pattern
 * and each other (vanilla shaped recipes also match mirrored, which was checked too).
 *
 * <p>Bows follow vanilla's bow layout (the bow on the left, string down the right), from
 * {@code pixel-art/weaponry/docs/bow-stats.md} §6: S = stick, X = string, L = leather, I = iron ingot, G = gold ingot.
 *
 * <p>Crossbows follow vanilla's crossbow layout, from {@code crossbow-stats.md} §6: S = stick, I = iron ingot,
 * X = string, H = tripwire hook, L = lapis lazuli, D = dispenser.
 */
public class WeaponRecipeProvider extends FabricRecipeProvider {
	public WeaponRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public String getName() {
		return "Weaponry recipes";
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
		return new RecipeProvider(registries, output) {
			@Override
			public void buildRecipes() {
				for (WeaponItem item : ModItems.all()) {
					ShapedRecipeBuilder builder = shaped(RecipeCategory.COMBAT, item);
					for (String row : pattern(item)) {
						builder.pattern(row);
					}
					MaterialInput material = materialInput(item);
					builder.define('M', material.ingredient());
					// A key defined but missing from the pattern fails datagen, so only define the ones used.
					String keys = String.join("", pattern(item));
					if (keys.indexOf('S') >= 0) {
						builder.define('S', Items.STICK);
					}
					if (keys.indexOf('C') >= 0) {
						builder.define('C', Items.IRON_CHAIN);
					}
					if (keys.indexOf('X') >= 0) {
						builder.define('X', Items.STRING);
					}
					if (keys.indexOf('L') >= 0) {
						builder.define('L', Items.LEATHER);
					}
					builder.unlockedBy(material.criterionName(), material.criterion())
							.save(output);
				}
				bows();
				crossbows();
			}

			private void bows() {
				for (BowType type : BowType.values()) {
					ShapedRecipeBuilder builder = shaped(RecipeCategory.COMBAT, ModBows.get(type));
					String[] rows = bowPattern(type);
					for (String row : rows) {
						builder.pattern(row);
					}
					String keys = String.join("", rows);
					builder.define('S', Items.STICK).define('X', Items.STRING);
					if (keys.indexOf('L') >= 0) {
						builder.define('L', Items.LEATHER);
					}
					if (keys.indexOf('I') >= 0) {
						builder.define('I', Items.IRON_INGOT);
					}
					if (keys.indexOf('G') >= 0) {
						builder.define('G', Items.GOLD_INGOT);
					}
					builder.unlockedBy(getHasName(Items.STRING), has(Items.STRING)).save(output);
				}
			}

			private void crossbows() {
				shaped(RecipeCategory.COMBAT, ModCrossbows.HEAVY)
						.pattern("SIS").pattern("XHX").pattern("ISI")
						.define('S', Items.STICK).define('I', Items.IRON_INGOT).define('X', Items.STRING).define('H', Items.TRIPWIRE_HOOK)
						.unlockedBy(getHasName(Items.TRIPWIRE_HOOK), has(Items.TRIPWIRE_HOOK)).save(output);
				shaped(RecipeCategory.COMBAT, ModCrossbows.RAPID)
						.pattern("LIL").pattern("XHX").pattern(" D ")
						.define('L', Items.LAPIS_LAZULI).define('I', Items.IRON_INGOT).define('X', Items.STRING).define('H', Items.TRIPWIRE_HOOK)
						.define('D', Items.DISPENSER)
						.unlockedBy(getHasName(Items.TRIPWIRE_HOOK), has(Items.TRIPWIRE_HOOK)).save(output);
			}

			private MaterialInput of(TagKey<Item> tag, String name) {
				return new MaterialInput(tag(tag), "has_" + name, has(tag));
			}

			private MaterialInput of(Item item) {
				return new MaterialInput(Ingredient.of(item), getHasName(item), has(item));
			}

			private MaterialInput materialInput(WeaponItem item) {
				return switch (item.material()) {
					// The club is the cheap weapon: a wooden one is carved from a whole log.
					case WOODEN -> item.type() == WeaponType.CLUB
							? of(ItemTags.LOGS, "logs")
							: of(ItemTags.PLANKS, "planks");
					case STONE -> of(ItemTags.STONE_TOOL_MATERIALS, "stone_tool_materials");
					case IRON -> of(Items.IRON_INGOT);
					case GOLDEN -> of(Items.GOLD_INGOT);
					case DIAMOND -> of(Items.DIAMOND);
				};
			}
		};
	}

	private static String[] bowPattern(BowType type) {
		return switch (type) {
			case SELFBOW -> new String[] {" SX", "S  ", " SX"};
			case LONGBOW -> new String[] {"SSX", "L X", "SSX"};
			case RECURVE_BOW -> new String[] {" SX", "I X", " SX"};
			case HORSE_BOW -> new String[] {"GSX", "S X", "GSX"};
		};
	}

	private static String[] pattern(WeaponItem item) {
		return switch (item.type()) {
			case DAGGER -> new String[] {"M", "S"};
			case SABRE -> new String[] {"  M", " M ", "S  "};
			case SCIMITAR -> new String[] {" MM", " M ", "S  "};
			// Vanilla's spear (1.21.11+) took the plain diagonal {"  M", " S ", "S  "}, so ours has a wider head.
			case SPEAR -> new String[] {"  M", " SM", "S  "};
			case PIKE -> new String[] {" MM", " SM", "S  "};
			case LANCE -> new String[] {"  M", "MM ", "S  "};
			case HALBERD -> new String[] {"MMM", " S ", "S  "};
			case HATCHET -> new String[] {"MM", "MS"};
			case WAR_HAMMER -> new String[] {"MMM", "MSM", " S "};
			case CLUB -> new String[] {" M", "S "};
			case RAPIER -> new String[] {"  M", " M ", "MS "};
			case HOOK_SWORD -> new String[] {"MM", "M ", "S "};
			case GREATSWORD -> new String[] {" M ", "MMM", " S "};
			case BATTLE_AXE -> new String[] {"M M", "MSM", " S "};
			case WAR_PICK -> new String[] {"MMM", "MS ", " S "};
			// The 1.21.1 glaive shape (..M / .SM / S..) is the spear's here, so the blade sits on the other side.
			case GLAIVE -> new String[] {"  M", "MS ", "S  "};
			case SCYTHE -> new String[] {"MMM", "M S", "  S"};
			case MACE -> new String[] {" MM", " MM", "S  "};
			case MORNING_STAR -> new String[] {"M M", " M ", "S  "};
			case FLAIL -> new String[] {"  M", " C ", "S  "};
			case GAUNTLETS -> new String[] {"MMM", " L "};
			case ROPE_DART -> new String[] {"  M", " X ", "X  "};
		};
	}

	private record MaterialInput(Ingredient ingredient, String criterionName, Criterion<InventoryChangeTrigger.TriggerInstance> criterion) {
	}
}
