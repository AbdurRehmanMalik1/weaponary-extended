package net.firstwemine1.weaponry.ability;

import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Two-handed weapons need both hands. While one is equipped, whatever is in the off hand is moved
 * into the inventory, and a two-handed weapon put in the off hand is moved out too. If the inventory
 * is full, the off hand is blocked instead: no shield, no dual wielding, no off-hand item use. The
 * use callbacks run on both sides, so the client never even raises the shield.
 */
final class TwoHanded {
	/** Ticks between "inventory full" reminders, so the action bar isn't refreshed every tick. */
	private static final int FULL_WARNING_INTERVAL = 40;

	private static final int OFF_HAND_SLOT = 40;

	private TwoHanded() {
	}

	private static boolean isTwoHanded(ItemStack stack) {
		return stack.getItem() instanceof WeaponItem weapon && weapon.type().twoHanded();
	}

	static boolean blocksOffHand(Player player, InteractionHand hand) {
		return hand == InteractionHand.OFF_HAND
				&& !player.isSpectator()
				&& (isTwoHanded(player.getMainHandItem()) || isTwoHanded(player.getOffhandItem()));
	}

	/** True if the use is blocked; tells the player why when they actually tried to use something. */
	static boolean check(Player player, InteractionHand hand) {
		if (!blocksOffHand(player, hand)) {
			return false;
		}
		if (!player.level().isClientSide() && !player.getOffhandItem().isEmpty()) {
			ItemStack weapon = isTwoHanded(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
			player.sendOverlayMessage(Component.translatable(WeaponItem.TWO_HANDED_MESSAGE_KEY, weapon.getHoverName()).withStyle(ChatFormatting.RED));
		}
		return true;
	}

	/**
	 * Clears the off hand into the inventory while a two-handed weapon is equipped (or when one is put
	 * in the off hand). If the inventory is full, the item stays and the off hand stays blocked.
	 * Also lowers a shield that was raised before switching to the weapon.
	 */
	static void tick(ServerPlayer player) {
		if (player.isUsingItem() && blocksOffHand(player, player.getUsedItemHand())) {
			player.stopUsingItem();
		}
		ItemStack offHand = player.getOffhandItem();
		if (offHand.isEmpty() || player.isSpectator()
				|| !(isTwoHanded(player.getMainHandItem()) || isTwoHanded(offHand))) {
			return;
		}
		// Copies: the off hand empties below, and either may be what it held.
		ItemStack weapon = (isTwoHanded(player.getMainHandItem()) ? player.getMainHandItem() : offHand).copy();
		ItemStack moving = offHand.copy();
		// Out of the slot first: the inventory looks for room in the off hand too, and a stack merged
		// into itself is lost.
		Inventory inventory = player.getInventory();
		inventory.setItem(OFF_HAND_SLOT, ItemStack.EMPTY);
		moveToMainSlots(inventory, offHand);
		if (offHand.isEmpty()) {
			player.sendOverlayMessage(Component.translatable(WeaponItem.TWO_HANDED_MOVED_KEY,
					weapon.getHoverName(), moving.getHoverName()).withStyle(ChatFormatting.YELLOW));
			return;
		}
		inventory.setItem(OFF_HAND_SLOT, offHand);
		if (player.tickCount % FULL_WARNING_INTERVAL == 0) {
			player.sendOverlayMessage(Component.translatable(WeaponItem.TWO_HANDED_FULL_KEY,
					weapon.getHoverName(), offHand.getHoverName()).withStyle(ChatFormatting.RED));
		}
	}

	/**
	 * Moves what fits, shrinking the stack. Not {@code Inventory.add}: for a player with infinite
	 * materials, that deletes whatever doesn't fit.
	 */
	private static void moveToMainSlots(Inventory inventory, ItemStack stack) {
		while (!stack.isEmpty()) {
			int slot = inventory.getSlotWithRemainingSpace(stack);
			if (slot < 0) {
				slot = inventory.getFreeSlot();
			}
			if (slot < 0) {
				return;
			}
			ItemStack there = inventory.getItem(slot);
			if (there.isEmpty()) {
				inventory.setItem(slot, stack.split(stack.getCount()));
			} else {
				int moved = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
				there.grow(moved);
				stack.shrink(moved);
			}
		}
	}
}
