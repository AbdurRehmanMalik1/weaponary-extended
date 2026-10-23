package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.World;

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
		return ImmutableList.of("Use: thrust at a target up to 1 block past your reach, +2 damage");
	}

	@Override
	public ActionResult<ItemStack> onUse(World level, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		LivingEntity target = hand == Hand.MAIN_HAND ? Abilities.targetInReach(player, EXTRA_REACH) : null;
		if (target == null || !(stack.getItem() instanceof WeaponItem)) {
			return ActionResult.pass(stack);
		}
		player.resetAttackStrengthTicker();
		if (!level.isClientSide()) {
			thrust(level, player, target, stack, (WeaponItem) stack.getItem());
			stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
			Abilities.cooldown(player, WeaponType.RAPIER, COOLDOWN);
		}
		return ActionResult.sidedSuccess(stack, level.isClientSide());
	}

	/** What {@code PlayerEntity.attack} does for a full-charge hit, minus crits and sweeps. */
	private static void thrust(World level, PlayerEntity player, LivingEntity target, ItemStack stack, WeaponItem weapon) {
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
