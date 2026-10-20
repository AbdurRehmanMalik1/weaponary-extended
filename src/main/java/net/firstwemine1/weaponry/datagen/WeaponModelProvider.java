package net.firstwemine1.weaponry.datagen;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
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
			Optional.of(Weaponry.id("item/handheld_mirrored")), Optional.empty(), TextureSlot.LAYER0);

	public WeaponModelProvider(FabricDataGenerator generator) {
		super(generator);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generator) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generator) {
		// Held like a sword; texture is textures/item/<material>_<weapon>.png
		ModItems.all().forEach(item -> {
			if (item.type() == WeaponType.WAR_PICK) {
				// Switches to <item>_flipped while the weaponry:flipped item property (registered by the client) is 1.
				generator.generateFlatItem(item, new FlippableTemplate(item));
			} else {
				generator.generateFlatItem(item, mirrored(item.type()) ? HANDHELD_MIRRORED : ModelTemplates.FLAT_HANDHELD_ITEM);
			}
		});
	}

	/**
	 * {@code FLAT_HANDHELD_ITEM} that also writes the flipped model and the override to it. This version's
	 * {@code ModelTemplate} takes no custom JSON and the generators' output is private, but
	 * {@code generateFlatItem} hands its output to the template, so the template adds the override itself.
	 */
	private static final class FlippableTemplate extends ModelTemplate {
		private final Item item;

		FlippableTemplate(Item item) {
			super(Optional.of(new ResourceLocation("item/handheld")), Optional.empty(), TextureSlot.LAYER0);
			this.item = item;
		}

		@Override
		public ResourceLocation create(ResourceLocation id, TextureMapping textures,
				BiConsumer<ResourceLocation, Supplier<JsonElement>> output) {
			ResourceLocation flipped = ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(item, "_flipped"),
					TextureMapping.layer0(TextureMapping.getItemTexture(item, "_flipped")), output);
			return super.create(id, textures, (modelId, json) -> output.accept(modelId, () -> {
				JsonObject model = json.get().getAsJsonObject();
				JsonObject predicate = new JsonObject();
				predicate.addProperty(WarPickAbility.FLIPPED_ID.toString(), 1);
				JsonObject override = new JsonObject();
				override.add("predicate", predicate);
				override.addProperty("model", flipped.toString());
				JsonArray overrides = new JsonArray();
				overrides.add(override);
				model.add("overrides", overrides);
				return model;
			}));
		}
	}

	/** Weapons whose texture is mirrored left-right (pixel-art/weaponry/docs/rule.md, Rule 2 exceptions). */
	private static boolean mirrored(WeaponType type) {
		return type == WeaponType.SCYTHE || type == WeaponType.GAUNTLETS;
	}
}
