package net.firstwemine1.weaponry.client.crossbow;

import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What the renderer mixins ask: is this stack held like a crossbow, and held like a loaded one? */
public final class CrossbowPose {
	private CrossbowPose() {
	}

	/** Whether a check for {@code item} should also accept this stack. */
	public static boolean counts(ItemStack stack, Object item) {
		return item == Items.CROSSBOW && ModCrossbows.isCrossbow(stack);
	}

	/** A rapid crossbow with arrows in it is held like a loaded crossbow, except while reloading. */
	public static boolean isCharged(ItemStack stack) {
		return stack.is(ModCrossbows.RAPID) && RapidCrossbowItem.looksLoaded(stack, Minecraft.getInstance().player);
	}
}
