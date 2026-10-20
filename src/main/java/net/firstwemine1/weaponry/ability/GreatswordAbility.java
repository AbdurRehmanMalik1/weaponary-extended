package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/** Broad Swing: +1 knockback, and a vanilla-sized sweep on full-charge hits, out to the greatsword's reach. */
public class GreatswordAbility implements WeaponAbility {
	private static final double KNOCKBACK = 1.0;

	@Override
	public List<String> description() {
		return ImmutableList.of("Broad Swing: +1 knockback; weak sweep on full-charge hits");
	}

	@Override
	public void addAttributes(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
		ClubAbility.addKnockback(builder, KNOCKBACK);
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		GlaiveAbility.weakSweep(stack, target, attacker);
	}
}
