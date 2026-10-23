package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/** Reach 4 (from stats). +1 damage on a sprinting hit. Throw: a javelin doing melee damage +2. */
public class SpearAbility extends ThrowAbility {
	private static final float SPRINT_BONUS = 1.0f;

	public SpearAbility() {
		super(2.0f, 2.5f);
	}

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Charge: +1 damage on sprinting hits",
				"Throw (hold use): +2 damage");
	}

	@Override
	public float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		return AttackContext.sprintHit(attacker) ? SPRINT_BONUS * weapon.abilityStrength() : 0;
	}
}
