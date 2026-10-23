package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.world.World;

/**
 * Over the Shield: full-charge hits swing around a raised shield and deal half damage through it.
 * Unwieldy: hits below half charge do nothing, because a slack chain doesn't strike. Wind-up: hold
 * use to spin the head; every 0.5 s adds +1 damage to the next hit, up to +3.
 */
public class FlailAbility implements WeaponAbility {
	private static final float SHIELD_PASS = 0.5f;
	private static final float MIN_CHARGE = 0.5f;
	private static final int TICKS_PER_LEVEL = 10;
	private static final int MAX_LEVEL = 3;
	private static final float DAMAGE_PER_LEVEL = 1.0f;
	/** A wind-up not spent on a hit within 2 s of letting go is lost. */
	private static final int KEEP_TICKS = 40;
	private static final int SPIN_TICKS = 72000;

	private static final class WindUp {
		final int level;
		final long until;

		WindUp(int level, long until) {
			this.level = level;
			this.until = until;
		}
	}

	/** Server-side: the wind-up each player is carrying into their next hit. */
	private static final Map<PlayerEntity, WindUp> WOUND_UP = new WeakHashMap<>();

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Full-charge hits swing around shields for half damage",
				"Hits below half charge do no damage",
				"Hold use: spin the head; your next hit deals up to +3 damage");
	}

	@Override
	public float minimumCharge() {
		return MIN_CHARGE;
	}

	@Override
	public float shieldPassThrough(LivingEntity attacker) {
		if (!AttackContext.fullCharge(attacker)) {
			return 0;
		}
		float strength = attacker.getMainHandItem().getItem() instanceof WeaponItem
				? ((WeaponItem) attacker.getMainHandItem().getItem()).abilityStrength()
				: 1.0f;
		return SHIELD_PASS * strength;
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		if (!(attacker instanceof PlayerEntity) || attacker.level.isClientSide()
				|| AttackContext.charge(attacker) < MIN_CHARGE) {
			return 0;
		}
		PlayerEntity player = (PlayerEntity) attacker;
		WindUp windUp = WOUND_UP.remove(player);
		if (windUp == null || player.level.getGameTime() > windUp.until) {
			return 0;
		}
		player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_KNOCKBACK, player.getSoundSource(), 1.0f, 0.8f);
		return windUp.level * DAMAGE_PER_LEVEL * weapon.abilityStrength();
	}

	@Override
	public ActionResult<ItemStack> onUse(World level, PlayerEntity player, Hand hand) {
		// The bonus goes to the next main-hand hit, so only a main-hand flail spins.
		if (hand != Hand.MAIN_HAND) {
			return ActionResult.pass(player.getItemInHand(hand));
		}
		player.startUsingItem(hand);
		return ActionResult.consume(player.getItemInHand(hand));
	}

	@Override
	public UseAction useAnimation() {
		return UseAction.SPEAR;
	}

	@Override
	public int useDuration() {
		return SPIN_TICKS;
	}

	@Override
	public void onUseTick(ItemStack stack, World level, LivingEntity user, int remainingTicks) {
		int spun = SPIN_TICKS - remainingTicks;
		if (!level.isClientSide() && spun > 0 && spun % TICKS_PER_LEVEL == 0 && spun / TICKS_PER_LEVEL <= MAX_LEVEL) {
			// A rising chain rattle for each level gained.
			level.playSound(null, user.getX(), user.getY(), user.getZ(),
					SoundEvents.CHAIN_HIT, user.getSoundSource(), 1.0f, 0.8f + 0.2f * (spun / TICKS_PER_LEVEL));
		}
	}

	@Override
	public void onReleaseUsing(ItemStack stack, World level, LivingEntity user, int remainingTicks) {
		int windUp = Math.min(MAX_LEVEL, (SPIN_TICKS - remainingTicks) / TICKS_PER_LEVEL);
		if (!level.isClientSide() && user instanceof PlayerEntity && windUp > 0) {
			WOUND_UP.put((PlayerEntity) user, new WindUp(windUp, level.getGameTime() + KEEP_TICKS));
		}
	}
}
