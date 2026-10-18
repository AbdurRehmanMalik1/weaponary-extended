package net.firstwemine1.weaponry.datagen;

import java.util.function.Consumer;

import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Crafting recipes. M = tier material, S = stick, C = chain, X = string, L = leather. Shapes avoid every vanilla tool/weapon pattern
 * and each other (vanilla shaped recipes also match mirrored, which was checked too).
 */
public class WeaponRecipeProvider extends FabricRecipeProvider {
	public WeaponRecipeProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void buildRecipes(Consumer<FinishedRecipe> output) {
		for (WeaponItem item : ModItems.all()) {
			ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item);
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
				builder.define('C', Items.CHAIN);
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
	}

	private static String[] pattern(WeaponItem item) {
		return switch (item.type()) {
			case DAGGER -> new String[] {"M", "S"};
			case SABRE -> new String[] {"  M", " M ", "S  "};
			case SCIMITAR -> new String[] {" MM", " M ", "S  "};
			case SPEAR -> new String[] {"  M", " S ", "S  "};
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
			case GLAIVE -> new String[] {"  M", " SM", "S  "};
			case SCYTHE -> new String[] {"MMM", "M S", "  S"};
			case MACE -> new String[] {" MM", " MM", "S  "};
			case MORNING_STAR -> new String[] {"M M", " M ", "S  "};
			case FLAIL -> new String[] {"  M", " C ", "S  "};
			case GAUNTLETS -> new String[] {"MMM", " L "};
			case ROPE_DART -> new String[] {"  M", " X ", "X  "};
		};
	}

	private record MaterialInput(Ingredient ingredient, String criterionName, InventoryChangeTrigger.TriggerInstance criterion) {
		static MaterialInput of(TagKey<Item> tag, String name) {
			return new MaterialInput(Ingredient.of(tag), "has_" + name, has(tag));
		}

		static MaterialInput of(Item item) {
			return new MaterialInput(Ingredient.of(item), getHasName(item), has(item));
		}
	}

	private static MaterialInput materialInput(WeaponItem item) {
		return switch (item.material()) {
			// The club is the cheap weapon: a wooden one is carved from a whole log.
			case WOODEN -> item.type() == WeaponType.CLUB
					? MaterialInput.of(ItemTags.LOGS, "logs")
					: MaterialInput.of(ItemTags.PLANKS, "planks");
			case STONE -> MaterialInput.of(ItemTags.STONE_TOOL_MATERIALS, "stone_tool_materials");
			case IRON -> MaterialInput.of(Items.IRON_INGOT);
			case GOLDEN -> MaterialInput.of(Items.GOLD_INGOT);
			case DIAMOND -> MaterialInput.of(Items.DIAMOND);
		};
	}
}
