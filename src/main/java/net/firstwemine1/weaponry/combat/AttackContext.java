package net.firstwemine1.weaponry.combat;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.potion.Effects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * State of the melee attack a player is making right now, captured from Forge's
 * {@code AttackEntityEvent} at the start of {@code Player.attack} (see {@link CombatEvents}). Abilities
 * read the charge and sprint from here. Mobs always count as fully charged.
 */
public final class AttackContext {
	/** Charge at which vanilla counts a hit as "strong" (sprint knockback, crits, sweeps). */
	public static final float FULL_CHARGE = 0.9f;

	private static final Map<PlayerEntity, State> STATES = new WeakHashMap<>();

	private static final class State {
		final float charge;
		final boolean sprintHit;
		@Nullable
		Entity target;
		boolean crit;

		State(float charge, boolean sprintHit, Entity target) {
			this.charge = charge;
			this.sprintHit = sprintHit;
			this.target = target;
		}
	}

	private AttackContext() {
	}

	@Nullable
	private static State state(LivingEntity attacker) {
		return attacker instanceof PlayerEntity ? STATES.get(attacker) : null;
	}

	/** A new melee attack starts: capture the charge before anything resets it. */
	static void begin(PlayerEntity player, Entity target) {
		STATES.put(player, new State(player.getAttackStrengthScale(0.5f), player.isSprinting(), target));
	}

	/** Whether {@code entity} is the target of the attack in progress, before {@link #takeMainHit} uses it up. */
	static boolean isMainHit(PlayerEntity player, Entity entity) {
		State state = STATES.get(player);
		return state != null && state.target == entity;
	}

	/** True once per attack, for the hurt of the attack's own target (not sweeps or other damage). */
	static boolean takeMainHit(PlayerEntity player, Entity entity) {
		State state = STATES.get(player);
		if (state == null || state.target != entity) {
			return false;
		}
		state.target = null;
		return true;
	}

	public static float charge(LivingEntity attacker) {
		State state = state(attacker);
		return state != null ? state.charge : 1.0f;
	}

	public static boolean fullCharge(LivingEntity attacker) {
		return charge(attacker) > FULL_CHARGE;
	}

	/** Sprinting when the attack started (vanilla stops the sprint before the item hooks run). */
	public static boolean sprintHit(LivingEntity attacker) {
		State state = state(attacker);
		return state != null ? state.sprintHit : attacker.isSprinting();
	}

	/** Records that this hit is a critical hit (vanilla or forced), for abilities that trigger on crits. */
	public static void markCrit(LivingEntity attacker) {
		State state = state(attacker);
		if (state != null) {
			state.crit = true;
		}
	}

	/** Set from {@code CriticalHitEvent} earlier in the same attack; mobs never crit. */
	public static boolean crit(LivingEntity attacker) {
		State state = state(attacker);
		return state != null && state.crit;
	}

	/** The vanilla jump-crit condition from {@code Player.attack}, evaluated at the same moment. */
	public static boolean vanillaCrit(PlayerEntity player, Entity target) {
		return fullCharge(player)
				&& player.fallDistance > 0.0f
				&& !player.isOnGround()
				&& !player.onClimbable()
				&& !player.isInWater()
				&& !player.hasEffect(Effects.BLINDNESS)
				&& !player.isPassenger()
				&& target instanceof LivingEntity
				&& !player.isSprinting();
	}
}
