package net.firstwemine1.weaponry.ability;

import java.util.function.Consumer;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * The vanilla sword sweep, with its area, damage and range as arguments. Shared by the sweeping weapons.
 * Like this version's vanilla sweep, it applies no per-target enchantment damage.
 */
final class Sweep {
	private static final double VANILLA_KNOCKBACK = 0.4;

	private Sweep() {
	}

	/** The vanilla trigger: a full-charge, grounded, non-sprinting hit (on the ground also rules out a jump crit). */
	static boolean triggers(PlayerEntity player) {
		return AttackContext.fullCharge(player) && !AttackContext.sprintHit(player) && player.isOnGround();
	}

	/** Vanilla sweep damage: 1 + the Sweeping Edge share of the hit. */
	static float vanillaDamage(PlayerEntity player) {
		return 1.0f + EnchantmentHelper.getSweepingDamageRatio(player)
				* (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	/**
	 * Hits everything around {@code target} within {@code maxDistance} of the player with {@code damage}, then plays
	 * the sweep effects. Vanilla inflates the target's box by (1, 0.25, 1); {@code areaScale} scales that.
	 * {@code onSwept} runs for each mob the sweep damaged.
	 */
	static void sweep(ServerWorld level, PlayerEntity player, LivingEntity target, ItemStack stack, double areaScale,
			double maxDistance, float damage, Consumer<LivingEntity> onSwept) {
		DamageSource source = DamageSource.playerAttack(player);
		AxisAlignedBB area = target.getBoundingBox().inflate(areaScale, 0.25 * areaScale, areaScale);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (other == player || other == target || player.isAlliedTo(other)
					|| other instanceof ArmorStandEntity && ((ArmorStandEntity) other).isMarker()
					|| player.distanceToSqr(other) >= maxDistance * maxDistance) {
				continue;
			}
			Abilities.knockbackFrom(other, player, VANILLA_KNOCKBACK);
			if (other.hurt(source, damage)) {
				onSwept.accept(other);
			}
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0f, 1.0f);
		player.sweepAttack();
	}

	/** Server-side player and weapon for a sweep from {@code onHit}, or {@code null} if this hit can't sweep. */
	static WeaponItem weaponFor(ItemStack stack, LivingEntity attacker) {
		if (attacker instanceof PlayerEntity && attacker.level instanceof ServerWorld
				&& stack.getItem() instanceof WeaponItem && triggers((PlayerEntity) attacker)) {
			return (WeaponItem) stack.getItem();
		}
		return null;
	}
}
