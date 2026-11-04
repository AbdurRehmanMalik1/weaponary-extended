package net.firstwemine1.weaponry.datagen;

import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.ability.FlailAbility;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.firstwemine1.weaponry.bow.ModBows;
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
			Optional.of(Weaponry.id("item/handheld_mirrored")), Optional.empty(), TextureSlot.LAYER0);

	/** Bows: vanilla's bow model (its in-hand pose) with our texture. */
	private static final ModelTemplate BOW = new ModelTemplate(
			Optional.of(new ResourceLocation("item/bow")), Optional.empty(), TextureSlot.LAYER0);
	/** Vanilla's draw frames: shown while drawing, from 0.65 of a full draw, and from 0.9. */
	private static final float[] PULL = {0, 0.65f, 0.9f};
	/** The flail's wind-up frames: {@code weaponry:spin} is 1/3, 2/3 or 1 while spinning, 0 otherwise. */
	private static final float[] SPIN = {0.33f, 0.66f, 1};

	/** Crossbows: vanilla's crossbow model (its in-hand pose) with our texture. */
	private static final ModelTemplate CROSSBOW = new ModelTemplate(
			Optional.of(new ResourceLocation("item/crossbow")), Optional.empty(), TextureSlot.LAYER0);
	/** Vanilla's load frames: shown while loading, from 0.58 of a full load, and when it's done. */
	private static final float[] CROSSBOW_PULL = {0, 0.58f, 1.0f};

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
			} else if (item.type() == WeaponType.FLAIL) {
				spinning(generator, item);
			} else {
				generator.generateFlatItem(item, mirrored(item.type()) ? HANDHELD_MIRRORED : ModelTemplates.FLAT_HANDHELD_ITEM);
			}
		});
		ModBows.all().forEach(bow -> bow(generator, bow));
		// The thrown rope dart: the dart without its rope coil. Not an item; the client loads it by name.
		ModelTemplates.FLAT_ITEM.create(Weaponry.id("item/rope_dart_head"),
				TextureMapping.layer0(Weaponry.id("item/rope_dart_head")), generator.output);
		crossbow(generator, ModCrossbows.HEAVY, true);
		// The rapid crossbow doesn't take rockets, so it has no firework frame.
		crossbow(generator, ModCrossbows.RAPID, false);
	}

	/**
	 * A handheld flail and its three wind-up frames ({@code <flail>_spin_0..2}), switched by the
	 * {@code weaponry:spin} item property. The client registers that property.
	 */
	private static void spinning(ItemModelGenerators generator, Item item) {
		JsonArray overrides = new JsonArray();
		for (int frame = 0; frame < SPIN.length; frame++) {
			ResourceLocation model = ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(item, "_spin_" + frame),
					TextureMapping.layer0(TextureMapping.getItemTexture(item, "_spin_" + frame)), generator.output);
			JsonObject predicate = new JsonObject();
			predicate.addProperty(FlailAbility.SPIN_ID.toString(), SPIN[frame]);
			JsonObject override = new JsonObject();
			override.add("predicate", predicate);
			override.addProperty("model", model.toString());
			overrides.add(override);
		}
		ModelTemplates.FLAT_HANDHELD_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item),
				generator.output, (id, textures) -> {
					JsonObject json = ModelTemplates.FLAT_HANDHELD_ITEM.createBaseTemplate(id, textures);
					json.add("overrides", overrides);
					return json;
				});
	}

	/** A crossbow, its three load frames and its loaded frames, switched like vanilla's crossbow model. */
	private static void crossbow(ItemModelGenerators generator, Item item, boolean firework) {
		JsonArray overrides = new JsonArray();
		for (int frame = 0; frame < CROSSBOW_PULL.length; frame++) {
			JsonObject predicate = new JsonObject();
			predicate.addProperty("pulling", 1);
			if (CROSSBOW_PULL[frame] > 0) {
				predicate.addProperty("pull", CROSSBOW_PULL[frame]);
			}
			overrides.add(crossbowOverride(generator, item, "_pulling_" + frame, predicate));
		}
		JsonObject charged = new JsonObject();
		charged.addProperty("charged", 1);
		overrides.add(crossbowOverride(generator, item, "_arrow", charged));
		if (firework) {
			JsonObject rocket = new JsonObject();
			rocket.addProperty("charged", 1);
			rocket.addProperty("firework", 1);
			overrides.add(crossbowOverride(generator, item, "_firework", rocket));
		}
		CROSSBOW.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), generator.output, (id, textures) -> {
			JsonObject json = CROSSBOW.createBaseTemplate(id, textures);
			json.add("overrides", overrides);
			return json;
		});
	}

	private static JsonObject crossbowOverride(ItemModelGenerators generator, Item item, String suffix, JsonObject predicate) {
		ResourceLocation model = CROSSBOW.create(ModelLocationUtils.getModelLocation(item, suffix),
				TextureMapping.layer0(TextureMapping.getItemTexture(item, suffix)), generator.output);
		JsonObject override = new JsonObject();
		override.add("predicate", predicate);
		override.addProperty("model", model.toString());
		return override;
	}

	/** A bow and its three draw frames ({@code <bow>_pulling_0..2}), switched like vanilla's bow model. */
	private static void bow(ItemModelGenerators generator, Item item) {
		JsonArray overrides = new JsonArray();
		for (int frame = 0; frame < PULL.length; frame++) {
			ResourceLocation model = BOW.create(ModelLocationUtils.getModelLocation(item, "_pulling_" + frame),
					TextureMapping.layer0(TextureMapping.getItemTexture(item, "_pulling_" + frame)), generator.output);
			JsonObject predicate = new JsonObject();
			predicate.addProperty("pulling", 1);
			if (PULL[frame] > 0) {
				predicate.addProperty("pull", PULL[frame]);
			}
			JsonObject override = new JsonObject();
			override.add("predicate", predicate);
			override.addProperty("model", model.toString());
			overrides.add(override);
		}
		BOW.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), generator.output, (id, textures) -> {
			JsonObject json = BOW.createBaseTemplate(id, textures);
			json.add("overrides", overrides);
			return json;
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
					predicate.addProperty(WarPickAbility.FLIPPED_ID.toString(), 1);
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
