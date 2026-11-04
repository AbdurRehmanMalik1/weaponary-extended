package net.firstwemine1.weaponry.client.crossbow;

import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.crossbow.WeaponCrossbow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What the renderer mixins ask: is this stack held like a crossbow, and held like a loaded one? */
public final class CrossbowPose {
	private CrossbowPose() {
	}

	/** What a comparison against vanilla's crossbow should see. */
	public static Item asCrossbow(Item item) {
		return item instanceof WeaponCrossbow ? Items.CROSSBOW : item;
	}

	/** A rapid crossbow with arrows in it is held like a loaded crossbow, except while reloading. */
	public static boolean isCharged(ItemStack stack) {
		return stack.getItem() == ModCrossbows.RAPID && RapidCrossbowItem.looksLoaded(stack, Minecraft.getInstance().player);
	}
}
