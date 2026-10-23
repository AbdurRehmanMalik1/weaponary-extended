package net.firstwemine1.weaponry.ability;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Reach 4 (from stats). Shield-breaker like an axe. Hook: use on a target in reach to pull it 1-2
 * blocks toward you, drag it off its mount and yank a raised shield aside.
 */
public class HalberdAbility implements WeaponAbility {
	private static final int COOLDOWN = 40;
	/** Horizontal launch speed toward the player; about 1.5 blocks of travel on the ground. */
	private static final double PULL_SPEED = 0.6;
	private static final double PULL_LIFT = 0.25;

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Shield Breaker: disables shields",
				"Hook (use): pull a target toward you, unseating riders");
	}

	@Override
	public boolean disablesShields() {
		return true;
	}

	@Override
	public ActionResult<ItemStack> onUse(World level, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		LivingEntity target = Abilities.targetInReach(player);
		if (target == null) {
			return ActionResult.pass(stack);
		}
		if (!level.isClientSide()) {
			hook(player, target, stack.getItem() instanceof WeaponItem ? ((WeaponItem) stack.getItem()).abilityStrength() : 1.0f);
			stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
			Abilities.cooldown(player, WeaponType.HALBERD, COOLDOWN);
		}
		return ActionResult.sidedSuccess(stack, level.isClientSide());
	}

	private static void hook(PlayerEntity player, LivingEntity target, float strength) {
		target.stopRiding();
		if (target instanceof PlayerEntity && ((PlayerEntity) target).isBlocking()) {
			((PlayerEntity) target).disableShield(true);
		}
		Abilities.pull(target, player, PULL_SPEED * strength, PULL_LIFT);
		player.level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1.0f, 0.6f);
	}
}
