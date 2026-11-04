package net.firstwemine1.weaponry.test;

import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * A player reported that switching to a greatsword deleted what was in the off hand. Whatever happens
 * to the off hand, nothing may ever be lost: every test counts the item before and after real server
 * ticks. Where it ends up is the second question: the inventory if there's room, else the off hand.
 */
public class TwoHandedGameTest {
	private static final int SETTLE = 5;

	private static ItemStack greatsword() {
		WeaponItem item = ModItems.variants(WeaponType.GREATSWORD).iterator().next();
		return new ItemStack(item);
	}

	private static ServerPlayer player(GameTestHelper helper, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		player.getInventory().clearContent();
		return player;
	}

	/** Every main slot but the held one (slot 0), holding a stack that nothing else merges into. */
	private static void fill(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 1; slot < inventory.getNonEquipmentItems().size(); slot++) {
			inventory.getNonEquipmentItems().set(slot, new ItemStack(Items.DIRT, 64));
		}
	}

	private static int count(ServerPlayer player, Item item) {
		int total = 0;
		Inventory inventory = player.getInventory();
		for (ItemStack stack : inventory.getNonEquipmentItems()) {
			total += stack.is(item) ? stack.getCount() : 0;
		}
		total += inventory.getItem(Inventory.SLOT_OFFHAND).is(item) ? inventory.getItem(Inventory.SLOT_OFFHAND).getCount() : 0;
		return total;
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new GameTestAssertException(Component.literal(message), 0);
		}
	}

	/** Puts {@code offHand} in the off hand, the greatsword in the main hand, and lets the server tick. */
	private static void switchToGreatsword(GameTestHelper helper, ServerPlayer player, ItemStack offHand, boolean expectMoved) {
		Item item = offHand.getItem();
		int before = offHand.getCount() + count(player, item);
		player.getInventory().setItem(Inventory.SLOT_OFFHAND, offHand);
		player.getInventory().setSelectedSlot(0);
		player.getInventory().getNonEquipmentItems().set(0, greatsword());
		helper.runAfterDelay(SETTLE, () -> {
			int after = count(player, item);
			check(after == before, item + " lost: had " + before + ", now " + after);
			check(player.getMainHandItem().getItem() instanceof WeaponItem, "the greatsword left the main hand");
			if (expectMoved) {
				check(player.getOffhandItem().isEmpty(), "off hand still holds " + player.getOffhandItem());
			}
			helper.succeed();
		});
	}

	@GameTest
	public void survivalMovesStackable(GameTestHelper helper) {
		switchToGreatsword(helper, player(helper, GameType.SURVIVAL), new ItemStack(Items.TORCH, 32), true);
	}

	@GameTest
	public void survivalMovesUnstackable(GameTestHelper helper) {
		switchToGreatsword(helper, player(helper, GameType.SURVIVAL), new ItemStack(Items.TOTEM_OF_UNDYING), true);
	}

	@GameTest
	public void survivalMovesShield(GameTestHelper helper) {
		switchToGreatsword(helper, player(helper, GameType.SURVIVAL), new ItemStack(Items.SHIELD), true);
	}

	@GameTest
	public void survivalMovesDamaged(GameTestHelper helper) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.setDamageValue(50);
		switchToGreatsword(helper, player(helper, GameType.SURVIVAL), sword, true);
	}

	@GameTest
	public void survivalMergesIntoStack(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.SURVIVAL);
		player.getInventory().getNonEquipmentItems().set(5, new ItemStack(Items.TORCH, 60));
		switchToGreatsword(helper, player, new ItemStack(Items.TORCH, 32), true);
	}

	@GameTest
	public void survivalFullKeepsStackable(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.SURVIVAL);
		fill(player);
		switchToGreatsword(helper, player, new ItemStack(Items.TORCH, 32), false);
	}

	@GameTest
	public void survivalFullKeepsUnstackable(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.SURVIVAL);
		fill(player);
		switchToGreatsword(helper, player, new ItemStack(Items.TOTEM_OF_UNDYING), false);
	}

	@GameTest
	public void survivalFullKeepsDamaged(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.SURVIVAL);
		fill(player);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.setDamageValue(50);
		switchToGreatsword(helper, player, sword, false);
	}

	/** Room for only part of the stack: the rest stays in the off hand. */
	@GameTest
	public void survivalPartlyFits(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.SURVIVAL);
		fill(player);
		player.getInventory().getNonEquipmentItems().set(5, new ItemStack(Items.TORCH, 60));
		switchToGreatsword(helper, player, new ItemStack(Items.TORCH, 32), false);
	}

	@GameTest
	public void creativeMovesStackable(GameTestHelper helper) {
		switchToGreatsword(helper, player(helper, GameType.CREATIVE), new ItemStack(Items.TORCH, 32), true);
	}

	@GameTest
	public void creativeMovesUnstackable(GameTestHelper helper) {
		switchToGreatsword(helper, player(helper, GameType.CREATIVE), new ItemStack(Items.TOTEM_OF_UNDYING), true);
	}

	@GameTest
	public void creativeMovesDamaged(GameTestHelper helper) {
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.setDamageValue(50);
		switchToGreatsword(helper, player(helper, GameType.CREATIVE), sword, true);
	}

	/** Vanilla's Inventory.add deletes what doesn't fit for a player with infinite materials. */
	@GameTest
	public void creativeFullKeepsStackable(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.CREATIVE);
		fill(player);
		switchToGreatsword(helper, player, new ItemStack(Items.TORCH, 32), false);
	}

	@GameTest
	public void creativeFullKeepsUnstackable(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.CREATIVE);
		fill(player);
		switchToGreatsword(helper, player, new ItemStack(Items.TOTEM_OF_UNDYING), false);
	}

	@GameTest
	public void creativeFullKeepsDamaged(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.CREATIVE);
		fill(player);
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.setDamageValue(50);
		switchToGreatsword(helper, player, sword, false);
	}

	@GameTest
	public void creativePartlyFits(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.CREATIVE);
		fill(player);
		player.getInventory().getNonEquipmentItems().set(5, new ItemStack(Items.TORCH, 60));
		switchToGreatsword(helper, player, new ItemStack(Items.TORCH, 32), false);
	}

	/** A greatsword put in the off hand leaves it, whatever is held. */
	@GameTest
	public void greatswordInOffHandMoves(GameTestHelper helper) {
		ServerPlayer player = player(helper, GameType.SURVIVAL);
		player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.TORCH, 8));
		player.getInventory().setItem(Inventory.SLOT_OFFHAND, greatsword());
		Item sword = player.getOffhandItem().getItem();
		helper.runAfterDelay(SETTLE, () -> {
			check(player.getOffhandItem().isEmpty(), "off hand still holds " + player.getOffhandItem());
			check(count(player, sword) == 1, "the greatsword was lost");
			check(count(player, Items.TORCH) == 8, "the held torches were lost");
			helper.succeed();
		});
	}
}
