package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.world.World;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Cavalry Cut: +2 damage while mounted. Riposte: use to parry for half a second; parrying a melee
 * hit cancels it and makes the next hit within 3 s a guaranteed critical hit.
 */
public class SabreAbility implements WeaponAbility {
	private static final float MOUNTED_BONUS = 2.0f;
	private static final int PARRY_TICKS = 10;
	private static final int PARRY_COOLDOWN = 20;
	private static final int RIPOSTE_WINDOW = 60;

	/** Server-side: game time until which a player's next hit is a riposte. */
	private static final Map<PlayerEntity, Long> RIPOSTE_UNTIL = new WeakHashMap<>();

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Cavalry: +2 damage while mounted",
				"Riposte (use): parry melee hits; a parry makes your next hit critical");
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		// The riposte crit is forced through CriticalHitEvent (CombatEvents), so it's a real crit.
		return attacker.isPassenger() ? MOUNTED_BONUS * weapon.abilityStrength() : 0;
	}

	@Override
	public ActionResult<ItemStack> onUse(World level, PlayerEntity player, Hand hand) {
		player.startUsingItem(hand);
		return ActionResult.consume(player.getItemInHand(hand));
	}

	@Override
	public UseAction useAnimation() {
		return UseAction.BLOCK;
	}

	@Override
	public int useDuration() {
		return PARRY_TICKS;
	}

	@Override
	public void onReleaseUsing(ItemStack stack, World level, LivingEntity user, int remainingTicks) {
		endParry(user);
	}

	@Override
	public void onFinishUsing(ItemStack stack, World level, LivingEntity user) {
		endParry(user);
	}

	private static void endParry(LivingEntity user) {
		if (user instanceof PlayerEntity) {
			Abilities.cooldown((PlayerEntity) user, WeaponType.SABRE, PARRY_COOLDOWN);
		}
	}

	/** Uses up a pending riposte; true if the player parried within the last 3 s. */
	public static boolean consumeRiposte(PlayerEntity player) {
		Long until = RIPOSTE_UNTIL.remove(player);
		return until != null && player.level.getGameTime() <= until;
	}

	/**
	 * Called from {@code LivingAttackEvent} (see {@code Abilities}): a parrying player shrugs off a
	 * frontal melee hit.
	 */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof PlayerEntity) || !entity.isUsingItem() || !WeaponItem.is(entity.getUseItem(), WeaponType.SABRE)) {
			return true;
		}
		PlayerEntity player = (PlayerEntity) entity;
		// Melee only: the attacker itself is the direct source (no arrows, tridents or explosions).
		if (!(source.getDirectEntity() instanceof LivingEntity) || source.getEntity() != source.getDirectEntity()
				|| source.isBypassArmor()) {
			return true;
		}
		LivingEntity attacker = (LivingEntity) source.getDirectEntity();
		Vector3d toAttacker = attacker.position().subtract(player.position()).multiply(1, 0, 1);
		Vector3d facing = player.getViewVector(1.0f).multiply(1, 0, 1);
		if (toAttacker.dot(facing) <= 0) {
			return true;
		}

		player.stopUsingItem();
		Abilities.cooldown(player, WeaponType.SABRE, PARRY_COOLDOWN);
		RIPOSTE_UNTIL.put(player, player.level.getGameTime() + RIPOSTE_WINDOW);
		attacker.knockback(0.5f, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
		player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.6f);
		if (player.level instanceof ServerWorld) {
			Vector3d spark = player.getEyePosition(1.0f).add(player.getViewVector(1.0f).scale(0.8));
			((ServerWorld) player.level).sendParticles(ParticleTypes.CRIT, spark.x, spark.y - 0.3, spark.z, 8, 0.2, 0.2, 0.2, 0.2);
		}
		return false;
	}
}
