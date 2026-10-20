package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Heavy Blow: +2 knockback. */
public class BattleAxeAbility implements WeaponAbility {
	private static final double KNOCKBACK = 2.0;

	@Override
	public List<String> description() {
		return ImmutableList.of("Heavy Blow: +2 knockback");
	}

	@Override
	public void addAttributes(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
		ClubAbility.addKnockback(builder, KNOCKBACK);
	}
}
