package net.firstwemine1.weaponry.datagen;

import java.util.Optional;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.client.BowPull;
import net.firstwemine1.weaponry.client.FlailSpin;
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
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;

public class WeaponModelProvider extends FabricModelProvider {
	/**
	 * For textures drawn mirrored (head top-left, handle bottom-right): handheld, turned a quarter turn
	 * so the grip still sits in the hand. Parent lives in assets/weaponry/models/item/handheld_mirrored.json.
	 */
	private static final ModelTemplate HANDHELD_MIRRORED = new ModelTemplate(
			Optional.of(ResourceLocation.fromNamespaceAndPath(Weaponry.MOD_ID, "item/handheld_mirrored")),
			Optional.empty(), TextureSlot.LAYER0);

	/** Vanilla's draw frames: the first while drawing, the second from 0.65 of a full draw, the third from 0.9. */
	private static final float PULL_1 = 0.65f;
	private static final float PULL_2 = 0.9f;

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
			} else if (item.type() == WeaponType.FLAIL) {
				spinning(generator, item);
			} else if (item.type() == WeaponType.ROPE_DART) {
				ropeDart(generator, item);
			} else {
				generator.generateFlatItem(item, mirrored(item.type()) ? HANDHELD_MIRRORED : ModelTemplates.FLAT_HANDHELD_ITEM);
			}
		});
		ModBows.all().forEach(bow -> bow(generator, bow));
	}

	/**
	 * A bow and its three draw frames ({@code <bow>_pulling_0..2}) on vanilla's bow model (its in-hand pose),
	 * switched like vanilla's bow but on {@link BowPull}: the draw measured against this bow's own draw time.
	 */
	private static void bow(ItemModelGenerators generator, Item item) {
		ItemModel.Unbaked idle = ItemModelUtils.plainModel(generator.createFlatItemModel(item, ModelTemplates.BOW));
		ItemModel.Unbaked frame0 = ItemModelUtils.plainModel(generator.createFlatItemModel(item, "_pulling_0", ModelTemplates.BOW));
		ItemModel.Unbaked frame1 = ItemModelUtils.plainModel(generator.createFlatItemModel(item, "_pulling_1", ModelTemplates.BOW));
		ItemModel.Unbaked frame2 = ItemModelUtils.plainModel(generator.createFlatItemModel(item, "_pulling_2", ModelTemplates.BOW));
		generator.itemModelOutput.accept(item, ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
				ItemModelUtils.rangeSelect(new BowPull(), frame0,
						ItemModelUtils.override(frame1, PULL_1), ItemModelUtils.override(frame2, PULL_2)),
				idle));
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

	/**
	 * A handheld flail whose item definition cycles through its wind-up frames ({@code <flail>_spin_0..2},
	 * textures of the same name) on {@link FlailSpin} while it's in use.
	 */
	private static void spinning(ItemModelGenerators generator, Item item) {
		ItemModel.Unbaked normal = ItemModelUtils.plainModel(generator.createFlatItemModel(item, ModelTemplates.FLAT_HANDHELD_ITEM));
		ItemModel.Unbaked[] frames = new ItemModel.Unbaked[FlailSpin.FRAMES];
		for (int frame = 0; frame < frames.length; frame++) {
			frames[frame] = ItemModelUtils.plainModel(generator.createFlatItemModel(item, "_spin_" + frame, ModelTemplates.FLAT_HANDHELD_ITEM));
		}
		generator.itemModelOutput.accept(item, ItemModelUtils.conditional(ItemModelUtils.isUsingItem(),
				ItemModelUtils.rangeSelect(new FlailSpin(), frames[0],
						ItemModelUtils.override(frames[1], 1), ItemModelUtils.override(frames[2], 2)),
				normal));
	}

	/**
	 * The rope dart, plus {@code rope_dart_head} (the dart without its rope coil) for the thrown dart, which
	 * {@code RopeDartRenderer} draws with the {@code none} display context.
	 */
	private static void ropeDart(ItemModelGenerators generator, Item item) {
		ItemModel.Unbaked normal = ItemModelUtils.plainModel(generator.createFlatItemModel(item, ModelTemplates.FLAT_HANDHELD_ITEM));
		ItemModel.Unbaked head = ItemModelUtils.plainModel(generator.createFlatItemModel(item, "_head", ModelTemplates.FLAT_ITEM));
		generator.itemModelOutput.accept(item, ItemModelUtils.select(new DisplayContext(), normal,
				ItemModelUtils.when(ItemDisplayContext.NONE, head)));
	}

	/** Weapons whose texture is mirrored left-right (pixel-art/weaponry/docs/rule.md, Rule 2 exceptions). */
	private static boolean mirrored(WeaponType type) {
		return type == WeaponType.SCYTHE || type == WeaponType.GAUNTLETS;
	}
}
