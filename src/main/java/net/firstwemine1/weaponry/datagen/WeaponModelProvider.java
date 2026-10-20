package net.firstwemine1.weaponry.datagen;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.bow.ModBows;
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
	/** Bows: vanilla's bow model (its in-hand pose) with our texture. */
	private static final ResourceLocation BOW_PARENT = new ResourceLocation("item/bow");
	private static final ModelTemplate BOW = new ModelTemplate(Optional.of(BOW_PARENT), Optional.empty(), TextureSlot.LAYER0);
	/** Vanilla's draw frames: shown while drawing, from 0.65 of a full draw, and from 0.9. */
	private static final float[] PULL = {0, 0.65f, 0.9f};

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
				generator.generateFlatItem(item, flippable(item));
			} else {
				generator.generateFlatItem(item, mirrored(item.type()) ? HANDHELD_MIRRORED : ModelTemplates.FLAT_HANDHELD_ITEM);
			}
		});
		ModBows.all().forEach(bow -> generator.generateFlatItem(bow, bow(bow)));
	}

	/**
	 * A handheld model that switches to {@code <item>_flipped} (texture of the same name) while the
	 * {@code weaponry:flipped} item property is 1. The client registers that property.
	 */
	private static ModelTemplate flippable(Item item) {
		return new OverridingTemplate(new ResourceLocation("item/handheld")) {
			@Override
			JsonArray overrides(BiConsumer<ResourceLocation, Supplier<JsonElement>> output) {
				ResourceLocation flipped = ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(item, "_flipped"),
						TextureMapping.layer0(TextureMapping.getItemTexture(item, "_flipped")), output);
				JsonObject predicate = new JsonObject();
				predicate.addProperty(WarPickAbility.FLIPPED_ID.toString(), 1);
				JsonArray overrides = new JsonArray();
				overrides.add(override(predicate, flipped));
				return overrides;
			}
		};
	}

	/** A bow and its three draw frames ({@code <bow>_pulling_0..2}), switched like vanilla's bow model. */
	private static ModelTemplate bow(Item item) {
		return new OverridingTemplate(BOW_PARENT) {
			@Override
			JsonArray overrides(BiConsumer<ResourceLocation, Supplier<JsonElement>> output) {
				JsonArray overrides = new JsonArray();
				for (int frame = 0; frame < PULL.length; frame++) {
					ResourceLocation model = BOW.create(ModelLocationUtils.getModelLocation(item, "_pulling_" + frame),
							TextureMapping.layer0(TextureMapping.getItemTexture(item, "_pulling_" + frame)), output);
					JsonObject predicate = new JsonObject();
					predicate.addProperty("pulling", 1);
					if (PULL[frame] > 0) {
						predicate.addProperty("pull", PULL[frame]);
					}
					overrides.add(override(predicate, model));
				}
				return overrides;
			}
		};
	}

	private static JsonObject override(JsonObject predicate, ResourceLocation model) {
		JsonObject override = new JsonObject();
		override.add("predicate", predicate);
		override.addProperty("model", model.toString());
		return override;
	}

	/**
	 * A one-layer item model that also gets {@code overrides}. This version's {@code ModelTemplate} takes no
	 * custom JSON and the generators' output is private, but {@code generateFlatItem} hands its output to the
	 * template, so the template writes the models the overrides point to and adds the overrides itself.
	 */
	private abstract static class OverridingTemplate extends ModelTemplate {
		OverridingTemplate(ResourceLocation parent) {
			super(Optional.of(parent), Optional.empty(), TextureSlot.LAYER0);
		}

		/** Writes the models the overrides point to, and returns the overrides. */
		abstract JsonArray overrides(BiConsumer<ResourceLocation, Supplier<JsonElement>> output);

		@Override
		public ResourceLocation create(ResourceLocation id, TextureMapping textures,
				BiConsumer<ResourceLocation, Supplier<JsonElement>> output) {
			JsonArray overrides = overrides(output);
			return super.create(id, textures, (modelId, json) -> output.accept(modelId, () -> {
				JsonObject model = json.get().getAsJsonObject();
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
