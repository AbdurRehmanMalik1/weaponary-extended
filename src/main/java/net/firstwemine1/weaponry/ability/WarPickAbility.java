package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

/**
 * Flip: use to turn the war pick round. Flipped, it mines like a pickaxe of its tier, a bit slower,
 * at 1 durability per block. There are no item components in this version, so the state is a
 * {@code weaponry:flipped} NBT flag on the stack; {@link WeaponItem} and {@code ItemStackMixin} read it.
 */
public class WarPickAbility implements WeaponAbility {
	/** Id of the item model property the client derives from the flag. */
	public static final ResourceLocation FLIPPED_ID = Weaponry.id("flipped");
	private static final String FLIPPED_TAG = FLIPPED_ID.toString();
	/** Mining speed relative to the pickaxe of the same tier. */
	private static final float MINING_SPEED = 0.8f;
	/** Holding use repeats every 4 ticks; this stops a held click flipping it back and forth. */
	private static final int COOLDOWN = 10;

	@Override
	public List<String> description() {
		return ImmutableList.of("Use: flip it to mine like a pickaxe (80% speed)");
	}

	@Override
	public boolean diggerEnchantable() {
		return true;
	}

	public static boolean isFlipped(ItemStack stack) {
		return WeaponItem.is(stack, WeaponType.WAR_PICK) && stack.hasTag() && stack.getTag().getBoolean(FLIPPED_TAG);
	}

	@Override
	public ActionResult<ItemStack> onUse(World level, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			if (isFlipped(stack)) {
				stack.removeTagKey(FLIPPED_TAG);
			} else {
				stack.getOrCreateTag().putBoolean(FLIPPED_TAG, true);
			}
			Abilities.cooldown(player, WeaponType.WAR_PICK, COOLDOWN);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ARMOR_EQUIP_CHAIN, player.getSoundSource(), 1.0f, 1.2f);
		}
		return ActionResult.sidedSuccess(stack, level.isClientSide());
	}

	/**
	 * Mining speed of a flipped war pick. There is no {@code #minecraft:mineable/pickaxe} tag before
	 * 1.17, so ask the vanilla pickaxe of the same tier, which already answers with its tier speed.
	 */
	public static float destroySpeed(WeaponMaterial material, ItemStack stack, BlockState state) {
		float speed = pickaxe(material).getDestroySpeed(stack, state);
		return speed > 1.0f ? speed * MINING_SPEED : 1.0f;
	}

	/** Whether a flipped war pick gets drops from {@code state}: same as the pickaxe of its tier. */
	public static boolean isCorrectToolForDrops(WeaponMaterial material, BlockState state) {
		return pickaxe(material).isCorrectToolForDrops(state);
	}

	private static Item pickaxe(WeaponMaterial material) {
		switch (material) {
			case STONE:
				return Items.STONE_PICKAXE;
			case IRON:
				return Items.IRON_PICKAXE;
			case GOLDEN:
				return Items.GOLDEN_PICKAXE;
			case DIAMOND:
				return Items.DIAMOND_PICKAXE;
			default:
				return Items.WOODEN_PICKAXE;
		}
	}
}
