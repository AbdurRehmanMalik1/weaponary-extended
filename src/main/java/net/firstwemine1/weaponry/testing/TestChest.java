package net.firstwemine1.weaponry.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.item.ModItems;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.ChestType;
import net.minecraft.world.gen.Heightmap;

/**
 * Testing aid (config {@code testChest}): a row of double chests east of world spawn holding one
 * of every weapon, bow and crossbow, then one chest full of arrows for them. Refilled on every server start;
 * anything else put in them is cleared. Weapons have durability, so they stack to 1: each double chest
 * holds 54 of them, and as many chests are placed as the weapon count needs. The first keeps its old
 * name so existing worlds reuse it.
 */
public final class TestChest {
	private static final String NAME = "Weaponry Test Chest";
	private static final String ARROW_CHEST = "Weaponry Arrow Chest";
	private static final int OFFSET_EAST = 2;
	/** UPDATE_ALL in later versions: notify neighbours and send to clients. */
	private static final int UPDATE_ALL = 3;
	/** Blocks between neighbouring chests along x, leaving a one-block gap. */
	private static final int SPACING = 2;

	private TestChest() {
	}

	public static void placeOrRefill(MinecraftServer server) {
		ServerWorld level = server.overworld();
		BlockPos spawn = level.getSharedSpawnPos();
		int z = spawn.getZ();

		List<ItemStack> weapons = new ArrayList<>();
		ModItems.all().forEach(item -> weapons.add(new ItemStack(item)));
		ModBows.all().forEach(bow -> weapons.add(new ItemStack(bow)));
		ModCrossbows.all().forEach(crossbow -> weapons.add(new ItemStack(crossbow)));
		int next = 0;
		int index = 0;
		for (; next < weapons.size(); index++) {
			IInventory container = chest(level, spawn.getX() + OFFSET_EAST + index * SPACING, z, name(index));
			for (int slot = 0; next < weapons.size() && slot < container.getContainerSize(); slot++) {
				container.setItem(slot, weapons.get(next++));
			}
			container.setChanged();
		}
		Weaponry.LOGGER.info("Test chests hold {} weapons", weapons.size());

		// The next chest along: every slot a full stack of arrows.
		IInventory arrows = chest(level, spawn.getX() + OFFSET_EAST + index * SPACING, z, ARROW_CHEST);
		for (int slot = 0; slot < arrows.getContainerSize(); slot++) {
			arrows.setItem(slot, new ItemStack(Items.ARROW, Items.ARROW.getMaxStackSize()));
		}
		arrows.setChanged();
	}

	/** The named double chest at {@code x, z} (placed if it isn't there yet), emptied. */
	private static IInventory chest(ServerWorld level, int x, int z, String name) {
		BlockPos pos = findExisting(level, x, z, name);
		if (pos == null) {
			pos = place(level, x, z, name);
		}
		BlockState state = level.getBlockState(pos);
		IInventory container = ChestBlock.getContainer((ChestBlock) state.getBlock(), state, level, pos, true);
		container.clearContent();
		Weaponry.LOGGER.info("{} filled at {} {} {}", name, pos.getX(), pos.getY(), pos.getZ());
		return container;
	}

	private static String name(int index) {
		return index == 0 ? NAME : NAME + " " + (index + 1);
	}

	/** Our chest from an earlier launch, so restarts refill it instead of stacking new ones. */
	private static BlockPos findExisting(ServerWorld level, int x, int z, String name) {
		for (int y = level.getMaxBuildHeight() - 1; y >= 0; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			TileEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof ChestTileEntity && ((ChestTileEntity) blockEntity).getCustomName() != null
					&& name.equals(((ChestTileEntity) blockEntity).getCustomName().getString())) {
				return pos;
			}
		}
		return null;
	}

	private static BlockPos place(ServerWorld level, int x, int z, String name) {
		// Front faces west, toward spawn. The right half sits on the side ChestBlock expects.
		BlockState left = Blocks.CHEST.defaultBlockState()
				.setValue(ChestBlock.FACING, Direction.WEST)
				.setValue(ChestBlock.TYPE, ChestType.LEFT);
		Direction toRight = ChestBlock.getConnectedDirection(left);
		BlockState right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT);

		int y = Math.max(
				level.getHeight(Heightmap.Type.WORLD_SURFACE, x, z),
				level.getHeight(Heightmap.Type.WORLD_SURFACE, x + toRight.getStepX(), z + toRight.getStepZ()));
		BlockPos leftPos = new BlockPos(x, y, z);
		BlockPos rightPos = leftPos.relative(toRight);
		level.setBlock(leftPos, left, UPDATE_ALL);
		level.setBlock(rightPos, right, UPDATE_ALL);

		for (BlockPos pos : Arrays.asList(leftPos, rightPos)) {
			TileEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof ChestTileEntity) {
				((ChestTileEntity) blockEntity).setCustomName(new StringTextComponent(name));
				blockEntity.setChanged();
			}
		}
		return leftPos;
	}
}
