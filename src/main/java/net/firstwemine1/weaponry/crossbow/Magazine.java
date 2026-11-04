package net.firstwemine1.weaponry.crossbow;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The arrows in a rapid crossbow: how many, and which type (one type at a time). Saved on the item's tag. */
public record Magazine(int count, Item arrow) {
	public static final int CAPACITY = 64;
	public static final Magazine EMPTY = new Magazine(0, Items.ARROW);
	private static final String TAG = "WeaponryMagazine";
	private static final String TAG_COUNT = "Count";
	private static final String TAG_ARROW = "Arrow";

	public static Magazine read(ItemStack stack) {
		CompoundTag root = stack.getTag();
		if (root == null || !root.contains(TAG)) {
			return EMPTY;
		}
		CompoundTag tag = root.getCompound(TAG);
		ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_ARROW));
		Item arrow = id == null ? Items.AIR : Registry.ITEM.get(id);
		return new Magazine(tag.getInt(TAG_COUNT), arrow == Items.AIR ? Items.ARROW : arrow);
	}

	public void write(ItemStack stack) {
		CompoundTag tag = new CompoundTag();
		tag.putInt(TAG_COUNT, count);
		tag.putString(TAG_ARROW, Registry.ITEM.getKey(arrow).toString());
		stack.getOrCreateTag().put(TAG, tag);
		// Vanilla holds a charged crossbow up, in first and third person.
		CrossbowItem.setCharged(stack, count > 0);
	}

	public boolean isEmpty() {
		return count <= 0;
	}

	public boolean isFull() {
		return count >= CAPACITY;
	}
}
