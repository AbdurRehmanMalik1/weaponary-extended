package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Unwieldy: no critical hits. Couched Charge: a hit while riding a moving mount adds damage from
 * the mount's speed, knocks the target far back and off its own mount, and costs 3 durability
 * instead of 1 (real lances shattered).
 */
public class LanceAbility implements WeaponAbility {
	/** Blocks per tick; a walking horse is below this, a trotting one above. */
	private static final double CHARGE_SPEED = 0.2;
	/** Bonus damage per block-per-second of mount speed. */
	private static final float DAMAGE_PER_SPEED = 0.8f;
	private static final float MAX_BONUS = 15.0f;
	private static final int EXTRA_DURABILITY_COST = 2;

	@Override
	public List<String> description() {
		return List.of(
				"Stiff Grip: no critical hits",
				"Couched Charge (mounted hit): bonus damage from speed, heavy knockback, dismounts the target");
	}

	@Override
	public boolean allowsCrits() {
		return false;
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		double speed = chargeSpeed(attacker);
		if (speed < CHARGE_SPEED) {
			return 0;
		}
		float blocksPerSecond = (float) speed * 20.0f;
		return Math.min(MAX_BONUS, blocksPerSecond * DAMAGE_PER_SPEED) * weapon.abilityStrength();
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		double speed = chargeSpeed(attacker);
		if (speed < CHARGE_SPEED) {
			return;
		}
		target.stopRiding();
		Abilities.knockbackFrom(target, attacker, 1.0 + speed * 3.0);
		target.level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.PLAYER_ATTACK_KNOCKBACK, attacker.getSoundSource(), 1.2f, 0.6f);
		if (target.level instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5), target.getZ(), 16, 0.4, 0.4, 0.4, 0.4);
		}
		// postHurtEnemy takes the usual 1 afterwards.
		stack.hurtAndBreak(EXTRA_DURABILITY_COST, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
	}

	/** Mount speed in blocks per tick, or 0 on foot. */
	private static double chargeSpeed(LivingEntity attacker) {
		if (!attacker.isPassenger() || attacker.level.isClientSide()) {
			return 0;
		}
		return Motion.horizontalSpeed(attacker.getRootVehicle());
	}
}
