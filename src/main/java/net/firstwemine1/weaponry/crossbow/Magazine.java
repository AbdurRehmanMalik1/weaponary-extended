package net.firstwemine1.weaponry.crossbow;

import net.minecraft.util.registry.Registry;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** The arrows in a rapid crossbow: how many, and which type (one type at a time). Saved on the item's tag. */
public final class Magazine {
	private final int count;
	private final Item arrow;

	public Magazine(int count, Item arrow) {
		this.count = count;
		this.arrow = arrow;
	}

	public int count() {
		return count;
	}

	public Item arrow() {
		return arrow;
	}

	public static final int CAPACITY = 64;
	public static final Magazine EMPTY = new Magazine(0, Items.ARROW);
	private static final String TAG = "WeaponryMagazine";
	private static final String TAG_COUNT = "Count";
	private static final String TAG_ARROW = "Arrow";

	public static Magazine read(ItemStack stack) {
		CompoundNBT root = stack.getTag();
		if (root == null || !root.contains(TAG)) {
			return EMPTY;
		}
		CompoundNBT tag = root.getCompound(TAG);
		ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_ARROW));
		Item arrow = id == null ? Items.AIR : Registry.ITEM.get(id);
		return new Magazine(tag.getInt(TAG_COUNT), arrow == Items.AIR ? Items.ARROW : arrow);
	}

	public void write(ItemStack stack) {
		CompoundNBT tag = new CompoundNBT();
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
