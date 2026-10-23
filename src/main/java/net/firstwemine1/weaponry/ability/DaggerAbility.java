package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.BleedEffect;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;

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
		return ImmutableList.of(
				"Backstab: x1.5 damage from behind",
				"Lacerate: critical hits cause Bleed",
				"Throw (hold use): +1 damage, causes Bleed");
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		if (attacker instanceof PlayerEntity && AttackContext.vanillaCrit((PlayerEntity) attacker, target)) {
			AttackContext.markCrit(attacker);
		}
		return isBehind(attacker, target)
				? damage * (BACKSTAB_MULTIPLIER - 1.0f) * weapon.abilityStrength()
				: 0;
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(stack.getItem() instanceof WeaponItem)) {
			return;
		}
		WeaponItem weapon = (WeaponItem) stack.getItem();
		if (isBehind(attacker, target) && attacker.level instanceof ServerWorld) {
			((ServerWorld) attacker.level).sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 8, 0.25, 0.25, 0.25, 0.1);
		}
		if (AttackContext.crit(attacker)) {
			BleedEffect.apply(target, attacker, weapon.material());
		}
	}

	@Override
	public void onThrownHit(ItemStack stack, LivingEntity target, LivingEntity owner) {
		if (stack.getItem() instanceof WeaponItem) {
			BleedEffect.apply(target, owner, ((WeaponItem) stack.getItem()).material());
		}
	}

	/** The attacker stands behind the target, judged by the way the target's body faces. */
	private static boolean isBehind(LivingEntity attacker, Entity target) {
		Vector3d toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1);
		if (toAttacker.lengthSqr() < 1.0e-6) {
			return false;
		}
		float bodyYaw = target instanceof LivingEntity ? ((LivingEntity) target).yBodyRot : target.yRot;
		// Vec3.directionFromRotation is client-only in 1.16.5; this is its result for a level look.
		float yaw = bodyYaw * ((float) Math.PI / 180.0f);
		Vector3d facing = new Vector3d(-MathHelper.sin(yaw), 0, MathHelper.cos(yaw));
		return toAttacker.normalize().dot(facing) < BEHIND_DOT;
	}
}
