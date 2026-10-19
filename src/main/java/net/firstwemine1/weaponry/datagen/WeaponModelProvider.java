package net.firstwemine1.weaponry.datagen;

import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class WeaponModelProvider extends FabricModelProvider {
	/**
	 * For textures drawn mirrored (head top-left, handle bottom-right): handheld, turned a quarter turn
	 * so the grip still sits in the hand. Parent lives in assets/weaponry/models/item/handheld_mirrored.json.
	 */
	private static final ModelTemplate HANDHELD_MIRRORED = new ModelTemplate(
			Optional.of(ResourceLocation.fromNamespaceAndPath(Weaponry.MOD_ID, "item/handheld_mirrored")),
			Optional.empty(), TextureSlot.LAYER0);

	public WeaponModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generator) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generator) {
		// Held like a sword; texture is textures/item/<material>_<weapon>.png
		ModItems.all().forEach(item -> {
			if (item.type() == WeaponType.WAR_PICK) {
				flippable(generator, item);
			} else {
				generator.generateFlatItem(item, mirrored(item.type()) ? HANDHELD_MIRRORED : ModelTemplates.FLAT_HANDHELD_ITEM);
			}
		});
	}

	/**
	 * A handheld model that switches to {@code <item>_flipped} (texture of the same name) while the
	 * {@code weaponry:flipped} item property is 1. The client registers that property.
	 */
	private static void flippable(ItemModelGenerators generator, Item item) {
		ResourceLocation flipped = ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(item, "_flipped"),
				TextureMapping.layer0(TextureMapping.getItemTexture(item, "_flipped")), generator.output);
		ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item),
				generator.output, (id, textures) -> {
					JsonObject json = ModelTemplates.FLAT_HANDHELD_ITEM.createBaseTemplate(id, textures);
					JsonObject predicate = new JsonObject();
					predicate.addProperty(ModComponents.FLIPPED_ID.toString(), 1);
					JsonObject override = new JsonObject();
					override.add("predicate", predicate);
					override.addProperty("model", flipped.toString());
					JsonArray overrides = new JsonArray();
					overrides.add(override);
					json.add("overrides", overrides);
					return json;
				});
	}

	/** Weapons whose texture is mirrored left-right (pixel-art/weaponry/docs/rule.md, Rule 2 exceptions). */
	private static boolean mirrored(WeaponType type) {
		return type == WeaponType.SCYTHE || type == WeaponType.GAUNTLETS;
	}
}
