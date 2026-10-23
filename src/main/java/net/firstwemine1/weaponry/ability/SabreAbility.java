package net.firstwemine1.weaponry.ability;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Cavalry Cut: +2 damage while mounted. Riposte: use to parry for half a second; parrying a melee
 * hit cancels it and makes the next hit within 3 s a guaranteed critical hit.
 */
public class SabreAbility implements WeaponAbility {
	private static final float MOUNTED_BONUS = 2.0f;
	private static final int PARRY_TICKS = 10;
	private static final int PARRY_COOLDOWN = 20;
	private static final int RIPOSTE_WINDOW = 60;
	private static final float CRIT_MULTIPLIER = 1.5f;

	/** Server-side: game time until which a player's next hit is a riposte. */
	private static final Map<Player, Long> RIPOSTE_UNTIL = new WeakHashMap<>();

	@Override
	public List<String> description() {
		return List.of(
				"Cavalry: +2 damage while mounted",
				"Riposte (use): parry melee hits; a parry makes your next hit critical");
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		float bonus = attacker.isPassenger() ? MOUNTED_BONUS * weapon.abilityStrength() : 0;
		if (attacker instanceof Player player && !player.level().isClientSide() && target instanceof LivingEntity
				&& consumeRiposte(player) && !AttackContext.vanillaCrit(player, target)) {
			// Vanilla multiplies by 1.5 after this bonus; adding half of the total does the same.
			bonus += (damage + bonus) * (CRIT_MULTIPLIER - 1);
			AttackContext.forceCrit(player);
		}
		return bonus;
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker instanceof Player player && AttackContext.forcedCrit(player)) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.PLAYER_ATTACK_CRIT, player.getSoundSource(), 1.0f, 1.0f);
			player.crit(target);
		}
	}

	@Override
	public InteractionResult onUse(Level level, Player player, InteractionHand hand) {
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public ItemUseAnimation useAnimation() {
		return ItemUseAnimation.BLOCK;
	}

	@Override
	public int useDuration() {
		return PARRY_TICKS;
	}

	@Override
	public boolean onReleaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
		endParry(user);
		return true;
	}

	@Override
	public void onFinishUsing(ItemStack stack, Level level, LivingEntity user) {
		endParry(user);
	}

	private static void endParry(LivingEntity user) {
		if (user instanceof Player player) {
			Abilities.cooldown(player, WeaponType.SABRE, PARRY_COOLDOWN);
		}
	}

	private static boolean consumeRiposte(Player player) {
		Long until = RIPOSTE_UNTIL.remove(player);
		return until != null && player.level().getGameTime() <= until;
	}

	/** {@code LivingIncomingDamageEvent} (see {@code Abilities}): a parrying player shrugs off a frontal melee hit. */
	static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof Player player) || !player.isUsingItem() || !WeaponItem.is(player.getUseItem(), WeaponType.SABRE)) {
			return true;
		}
		// Melee only: the attacker itself is the direct source (no arrows, tridents or explosions).
		if (!(source.getDirectEntity() instanceof LivingEntity attacker) || source.getEntity() != attacker
				|| source.is(DamageTypeTags.BYPASSES_SHIELD)) {
			return true;
		}
		Vec3 toAttacker = attacker.position().subtract(player.position()).multiply(1, 0, 1);
		Vec3 facing = player.getViewVector(1.0f).multiply(1, 0, 1);
		if (toAttacker.dot(facing) <= 0) {
			return true;
		}

		player.stopUsingItem();
		Abilities.cooldown(player, WeaponType.SABRE, PARRY_COOLDOWN);
		RIPOSTE_UNTIL.put(player, player.level().getGameTime() + RIPOSTE_WINDOW);
		Abilities.knockback(attacker, player, 0.5, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.6f);
		if (player.level() instanceof ServerLevel level) {
			Vec3 spark = player.getEyePosition().add(player.getViewVector(1.0f).scale(0.8));
			level.sendParticles(ParticleTypes.CRIT, spark.x, spark.y - 0.3, spark.z, 8, 0.2, 0.2, 0.2, 0.2);
		}
		return false;
	}
}
