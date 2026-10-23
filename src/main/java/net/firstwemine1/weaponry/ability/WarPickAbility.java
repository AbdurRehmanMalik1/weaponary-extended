package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;

/**
 * Flip: use to turn the war pick round. Flipped, it mines like a pickaxe of its tier, a bit slower,
 * at 1 durability per block. The state is the {@link ModComponents#FLIPPED} component plus a pickaxe
 * {@code TOOL} component on the stack, so vanilla mining picks it up with no other hooks.
 */
public class WarPickAbility implements WeaponAbility {
	/** Mining speed relative to the pickaxe of the same tier. */
	private static final float MINING_SPEED = 0.8f;
	/** Holding use repeats every 4 ticks; this stops a held click flipping it back and forth. */
	private static final int COOLDOWN = 10;

	@Override
	public List<String> description() {
		return List.of("Use: flip it to mine like a pickaxe (80% speed)");
	}

	public static boolean isFlipped(ItemStack stack) {
		return stack.has(ModComponents.FLIPPED.get());
	}

	@Override
	public InteractionResult onUse(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide() && stack.getItem() instanceof WeaponItem item) {
			if (isFlipped(stack)) {
				stack.remove(ModComponents.FLIPPED.get());
				// Setting the item's own value drops the override, back to sword-like mining.
				stack.set(DataComponents.TOOL, item.components().get(DataComponents.TOOL));
			} else {
				stack.set(ModComponents.FLIPPED.get(), Unit.INSTANCE);
				stack.set(DataComponents.TOOL, pickaxe(item.material().tier()));
			}
			Abilities.cooldown(player, WeaponType.WAR_PICK, COOLDOWN);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ARMOR_EQUIP_CHAIN, player.getSoundSource(), 1.0f, 1.2f);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * The rules {@code ToolMaterial.applyToolProperties} gives a pickaxe of {@code material}, with the mining speed
	 * scaled down. The block registry is frozen by now, so its tags are read from it directly.
	 */
	private static Tool pickaxe(ToolMaterial material) {
		return new Tool(List.of(
				Tool.Rule.deniesDrops(BuiltInRegistries.BLOCK.getOrThrow(material.incorrectBlocksForDrops())),
				Tool.Rule.minesAndDrops(BuiltInRegistries.BLOCK.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE),
						material.speed() * MINING_SPEED)), 1.0f, 1);
	}
}
