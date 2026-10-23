package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.SoundEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Reach 5 (from stats). Too close: half damage against targets under 1.5 blocks away.
 * Brace: while sneaking and standing still, hits on targets charging at you (on foot or mounted)
 * do double damage and knock riders off their mounts.
 */
public class PikeAbility implements WeaponAbility {
	private static final double TOO_CLOSE = 1.5;
	private static final float TOO_CLOSE_MULTIPLIER = 0.5f;
	private static final float BRACE_MULTIPLIER = 2.0f;
	/** Blocks per tick. Brace counts as "standing still" below this. */
	private static final double STILL_SPEED = 0.03;
	/** Blocks per tick toward the braced player (2 blocks per second, a brisk mob walk). */
	private static final double CHARGE_SPEED = 0.1;

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Too Close: half damage against targets under 1.5 blocks away",
				"Brace (sneak still): charging targets take double damage and are dismounted");
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		float multiplier = 1.0f;
		if (attacker.distanceTo(target) < TOO_CLOSE) {
			multiplier *= TOO_CLOSE_MULTIPLIER;
		}
		if (bracedAgainst(attacker, target)) {
			multiplier *= 1.0f + (BRACE_MULTIPLIER - 1.0f) * weapon.abilityStrength();
		}
		return damage * (multiplier - 1.0f);
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!bracedAgainst(attacker, target)) {
			return;
		}
		target.stopRiding();
		target.level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_KNOCKBACK, attacker.getSoundSource(), 1.0f, 0.7f);
		if (target.level instanceof ServerWorld) {
			((ServerWorld) target.level).sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
		}
	}

	/** Same answer in the damage hook and in onHit: both run in the same tick, before anything moves. */
	private static boolean bracedAgainst(LivingEntity attacker, Entity target) {
		return attacker instanceof PlayerEntity
				&& !attacker.level.isClientSide()
				&& attacker.isShiftKeyDown()
				&& !attacker.isPassenger()
				&& Motion.horizontalSpeed(attacker) < STILL_SPEED
				&& Motion.closingSpeed(target, attacker) >= CHARGE_SPEED;
	}
}
