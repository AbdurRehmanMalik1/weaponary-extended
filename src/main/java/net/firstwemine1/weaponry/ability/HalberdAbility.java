package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
		return List.of(
				"Disables shields",
				"Use: hook a target toward you, pulling riders off their mounts");
	}

	@Override
	public boolean disablesShields() {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> onUse(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		LivingEntity target = Abilities.targetInReach(player);
		if (target == null) {
			return InteractionResultHolder.pass(stack);
		}
		if (!level.isClientSide()) {
			hook(player, target, stack.getItem() instanceof WeaponItem weapon ? weapon.abilityStrength() : 1.0f);
			stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
			Abilities.cooldown(player, WeaponType.HALBERD, COOLDOWN);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	private static void hook(Player player, LivingEntity target, float strength) {
		target.stopRiding();
		if (target instanceof Player victim && victim.isBlocking()) {
			victim.disableShield();
		}
		Abilities.pull(target, player, PULL_SPEED * strength, PULL_LIFT);
		player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 0.6f);
	}
}
