package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.BleedEffect;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Backstab: x1.5 damage when hitting a target from behind. Critical hits cause Bleed. Throw: a
 * short-charge throw doing melee damage +1 that also causes Bleed.
 */
public class DaggerAbility extends ThrowAbility {
	private static final float BACKSTAB_MULTIPLIER = 1.5f;
	/** cos(60 deg): the attacker must be within 60 degrees of straight behind the target. */
	private static final double BEHIND_DOT = -0.5;

	public DaggerAbility() {
		super(1.0f, 2.0f);
	}

	@Override
	public List<String> description() {
		return List.of(
				"Backstab: x1.5 damage from behind",
				"Lacerate: critical hits cause Bleed",
				"Throw (hold use): +1 damage, causes Bleed");
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		if (attacker instanceof Player player && AttackContext.vanillaCrit(player, target)) {
			AttackContext.markCrit(player);
		}
		return isBehind(attacker, target)
				? damage * (BACKSTAB_MULTIPLIER - 1.0f) * weapon.abilityStrength()
				: 0;
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(stack.getItem() instanceof WeaponItem weapon)) {
			return;
		}
		if (isBehind(attacker, target) && attacker.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 8, 0.25, 0.25, 0.25, 0.1);
		}
		if (AttackContext.crit(attacker)) {
			BleedEffect.apply(target, attacker, weapon.material());
		}
	}

	@Override
	public void onThrownHit(ItemStack stack, LivingEntity target, LivingEntity owner) {
		if (stack.getItem() instanceof WeaponItem weapon) {
			BleedEffect.apply(target, owner, weapon.material());
		}
	}

	/** The attacker stands behind the target, judged by the way the target's body faces. */
	private static boolean isBehind(LivingEntity attacker, Entity target) {
		Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1);
		if (toAttacker.lengthSqr() < 1.0e-6) {
			return false;
		}
		float bodyYaw = target instanceof LivingEntity living ? living.yBodyRot : target.getYRot();
		Vec3 facing = Vec3.directionFromRotation(0, bodyYaw);
		return toAttacker.normalize().dot(facing) < BEHIND_DOT;
	}
}
