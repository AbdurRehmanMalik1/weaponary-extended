package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.server.level.ServerLevel;
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

	@Override
	public List<String> description() {
		return List.of("Use: thrust at a target up to 1 block past your reach, +2 damage");
	}

	@Override
	public InteractionResultHolder<ItemStack> onUse(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		LivingEntity target = hand == InteractionHand.MAIN_HAND ? Abilities.targetInReach(player, EXTRA_REACH) : null;
		if (target == null || !(stack.getItem() instanceof WeaponItem weapon)) {
			return InteractionResultHolder.pass(stack);
		}
		player.resetAttackStrengthTicker();
		if (level instanceof ServerLevel server) {
			thrust(server, player, target, stack, weapon);
			stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
			Abilities.cooldown(player, WeaponType.RAPIER, COOLDOWN);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	private static void thrust(ServerLevel level, Player player, LivingEntity target, ItemStack stack, WeaponItem weapon) {
		DamageSource source = player.damageSources().playerAttack(player);
		float damage = weapon.attackDamage() + BONUS_DAMAGE * weapon.abilityStrength();
		damage = EnchantmentHelper.modifyDamage(level, stack, target, source, damage);
		if (target.hurt(source, damage)) {
			Abilities.knockbackFrom(target, player, KNOCKBACK);
			EnchantmentHelper.doPostAttackEffects(level, target, source);
			player.setLastHurtMob(target);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_STRONG, player.getSoundSource(), 1.0f, 1.3f);
	}
}
