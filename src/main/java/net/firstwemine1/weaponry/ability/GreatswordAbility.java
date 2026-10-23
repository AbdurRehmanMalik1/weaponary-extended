package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Broad Swing: +1 knockback, and a vanilla-sized sweep on full-charge hits, out to the greatsword's reach. */
public class GreatswordAbility implements WeaponAbility {
	private static final double KNOCKBACK = 1.0;

	@Override
	public List<String> description() {
		return List.of("Broad Swing: +1 knockback; weak sweep on full-charge hits");
	}

	@Override
	public void addAttributes(ItemAttributeModifiers.Builder builder, WeaponMaterial material) {
		ClubAbility.addKnockback(builder, KNOCKBACK);
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		GlaiveAbility.weakSweep(stack, target, attacker);
	}
}
