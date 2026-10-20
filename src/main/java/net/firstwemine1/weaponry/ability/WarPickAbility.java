package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Flip: use to turn the war pick round. Flipped, it mines like a pickaxe of its tier, a bit slower,
 * at 1 durability per block. There are no item components in this version, so the state is a
 * {@code weaponry:flipped} NBT flag on the stack; {@link WeaponItem} reads it.
 */
public class WarPickAbility implements WeaponAbility {
	/** Id of the item model property the client derives from the flag. */
	public static final ResourceLocation FLIPPED_ID = Weaponry.id("flipped");
	private static final String FLIPPED_TAG = FLIPPED_ID.toString();
	/** Mining speed relative to the pickaxe of the same tier. */
	public static final float MINING_SPEED = 0.8f;
	/** Holding use repeats every 4 ticks; this stops a held click flipping it back and forth. */
	private static final int COOLDOWN = 10;

	@Override
	public List<String> description() {
		return List.of("Flip (use): turn it round to mine like a pickaxe (80% speed)");
	}

	@Override
	public boolean diggerEnchantable() {
		return true;
	}

	public static boolean isFlipped(ItemStack stack) {
		return WeaponItem.is(stack, WeaponType.WAR_PICK) && stack.hasTag() && stack.getTag().getBoolean(FLIPPED_TAG);
	}

	@Override
	public InteractionResultHolder<ItemStack> onUse(Level level, Player player, InteractionHand hand) {
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
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}
}
