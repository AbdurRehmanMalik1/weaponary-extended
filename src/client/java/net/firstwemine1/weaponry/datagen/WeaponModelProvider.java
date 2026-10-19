package net.firstwemine1.weaponry.datagen;

import java.util.Optional;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;

public class WeaponModelProvider extends FabricModelProvider {
	/**
	 * For textures drawn mirrored (head top-left, handle bottom-right): handheld, turned a quarter turn
	 * so the grip still sits in the hand. Parent lives in assets/weaponry/models/item/handheld_mirrored.json.
	 */
	private static final ModelTemplate HANDHELD_MIRRORED = new ModelTemplate(
			Optional.of(Weaponry.id("item/handheld_mirrored")), Optional.empty(), TextureSlot.LAYER0);

	public WeaponModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generator) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators generator) {
		// Held like a sword; texture is textures/item/<material>_<weapon>.png. Also writes items/<id>.json.
		ModItems.all().forEach(item -> {
			if (item.type() == WeaponType.WAR_PICK) {
				flippable(generator, item);
			} else {
				generator.generateFlatItem(item, mirrored(item.type()) ? HANDHELD_MIRRORED : ModelTemplates.FLAT_HANDHELD_ITEM);
			}
		});
	}

	/**
	 * A handheld model whose item definition switches to {@code <item>_flipped} (texture of the same name)
	 * while the stack has the {@code weaponry:flipped} component.
	 */
	private static void flippable(ItemModelGenerators generator, Item item) {
		ItemModel.Unbaked normal = ItemModelUtils.plainModel(generator.createFlatItemModel(item, ModelTemplates.FLAT_HANDHELD_ITEM));
		ItemModel.Unbaked flipped = ItemModelUtils.plainModel(
				generator.createFlatItemModel(item, "_flipped", ModelTemplates.FLAT_HANDHELD_ITEM));
		generator.itemModelOutput.accept(item, ItemModelUtils.conditional(ItemModelUtils.hasComponent(ModComponents.FLIPPED), flipped, normal));
	}

	/** Weapons whose texture is mirrored left-right (pixel-art/weaponry/docs/rule.md, Rule 2 exceptions). */
	private static boolean mirrored(WeaponType type) {
		return type == WeaponType.SCYTHE || type == WeaponType.GAUNTLETS;
	}
}
