package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.BleedEffect;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/**
 * Puncture: full-charge hits cause Bleed. Mail-breaker: hits wear the target's armor twice as fast
 * (in vanilla only players' armor wears, so this matters in PvP).
 */
public class MorningStarAbility implements WeaponAbility {
	private static final float ARMOR_WEAR = 2.0f;

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Puncture: full-charge hits cause Bleed",
				"Mail-breaker: wears armor twice as fast");
	}

	@Override
	public float armorWearMultiplier(WeaponMaterial material) {
		// Gold's stronger abilities apply to the extra wear: x2.5 instead of x2.
		return 1 + (ARMOR_WEAR - 1) * material.abilityStrength();
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (AttackContext.fullCharge(attacker) && stack.getItem() instanceof WeaponItem) {
			BleedEffect.apply(target, attacker, ((WeaponItem) stack.getItem()).material());
		}
	}
}
