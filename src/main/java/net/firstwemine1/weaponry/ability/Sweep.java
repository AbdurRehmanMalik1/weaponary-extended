package net.firstwemine1.weaponry.ability;

import java.util.function.Consumer;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** The vanilla sword sweep, with its area, damage and range as arguments. Shared by the sweeping weapons. */
final class Sweep {
	private static final double VANILLA_KNOCKBACK = 0.4;

	private Sweep() {
	}

	/** The vanilla trigger: a full-charge, grounded, non-sprinting hit (on the ground also rules out a jump crit). */
	static boolean triggers(Player player) {
		return AttackContext.fullCharge(player) && !AttackContext.sprintHit(player) && player.onGround();
	}

	/** Vanilla sweep damage: 1 + the Sweeping Edge share of the hit. */
	static float vanillaDamage(Player player) {
		return 1.0f + (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO)
				* (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	/**
	 * Hits everything around {@code target} within {@code maxDistance} of the player with {@code damage}, then plays
	 * the sweep effects. Vanilla inflates the target's box by (1, 0.25, 1); {@code areaScale} scales that.
	 * {@code onSwept} runs for each mob the sweep damaged.
	 */
	static void sweep(ServerLevel level, Player player, LivingEntity target, ItemStack stack, double areaScale,
			double maxDistance, float damage, Consumer<LivingEntity> onSwept) {
		DamageSource source = player.damageSources().playerAttack(player);
		var area = target.getBoundingBox().inflate(areaScale, 0.25 * areaScale, areaScale);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (other == player || other == target || player.isAlliedTo(other)
					|| other instanceof ArmorStand stand && stand.isMarker()
					|| player.distanceToSqr(other) >= maxDistance * maxDistance) {
				continue;
			}
			float modified = EnchantmentHelper.modifyDamage(level, stack, other, source, damage);
			Abilities.knockbackFrom(other, player, VANILLA_KNOCKBACK);
			if (other.hurt(source, modified)) {
				onSwept.accept(other);
			}
			EnchantmentHelper.doPostAttackEffects(level, other, source);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0f, 1.0f);
		player.sweepAttack();
	}

	/** Server-side player and weapon for a sweep from {@code onHit}, or {@code null} if this hit can't sweep. */
	static WeaponItem weaponFor(ItemStack stack, LivingEntity attacker) {
		if (attacker instanceof Player player && player.level() instanceof ServerLevel
				&& stack.getItem() instanceof WeaponItem weapon && triggers(player)) {
			return weapon;
		}
		return null;
	}
}
