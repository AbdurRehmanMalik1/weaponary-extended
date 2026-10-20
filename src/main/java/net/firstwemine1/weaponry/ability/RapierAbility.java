package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Thrust: use on a target up to 1 block past your reach for a full-strength hit +2, whatever the
 * attack charge. Resets the attack charge, so it doesn't stack with a normal hit straight after.
 */
public class RapierAbility implements WeaponAbility {
	private static final int COOLDOWN = 50;
	private static final float BONUS_DAMAGE = 2.0f;
	private static final double EXTRA_REACH = 1.0;
	private static final double KNOCKBACK = 0.3;
	/** Vanilla Fire Aspect: 4 s of fire per level. */
	private static final int FIRE_SECONDS_PER_LEVEL = 4;

	@Override
	public List<String> description() {
		return List.of("Thrust (use): hit a target up to 1 block past your reach, +2 damage");
	}

	@Override
	public InteractionResultHolder<ItemStack> onUse(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		LivingEntity target = hand == InteractionHand.MAIN_HAND ? Abilities.targetInReach(player, EXTRA_REACH) : null;
		if (target == null || !(stack.getItem() instanceof WeaponItem weapon)) {
			return InteractionResultHolder.pass(stack);
		}
		player.resetAttackStrengthTicker();
		if (!level.isClientSide()) {
			thrust(level, player, target, stack, weapon);
			stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
			Abilities.cooldown(player, WeaponType.RAPIER, COOLDOWN);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	/** What {@code Player.attack} does for a full-charge hit, minus crits and sweeps. */
	private static void thrust(Level level, Player player, LivingEntity target, ItemStack stack, WeaponItem weapon) {
		DamageSource source = DamageSource.playerAttack(player);
		float damage = weapon.attackDamage() + BONUS_DAMAGE * weapon.abilityStrength()
				+ EnchantmentHelper.getDamageBonus(stack, target.getMobType());
		int fireAspect = EnchantmentHelper.getFireAspect(player);
		if (target.hurt(source, damage)) {
			Abilities.knockbackFrom(target, player, KNOCKBACK);
			if (fireAspect > 0) {
				target.setSecondsOnFire(fireAspect * FIRE_SECONDS_PER_LEVEL);
			}
			EnchantmentHelper.doPostHurtEffects(target, player);
			EnchantmentHelper.doPostDamageEffects(player, target);
			player.setLastHurtMob(target);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_STRONG, player.getSoundSource(), 1.0f, 1.3f);
	}
}
