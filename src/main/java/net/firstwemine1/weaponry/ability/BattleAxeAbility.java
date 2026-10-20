package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Heavy Blow: +2 knockback. */
public class BattleAxeAbility implements WeaponAbility {
	private static final double KNOCKBACK = 2.0;

	@Override
	public List<String> description() {
		return List.of("Heavy Blow: +2 knockback");
	}

	@Override
	public void addAttributes(ItemAttributeModifiers.Builder builder, WeaponMaterial material) {
		ClubAbility.addKnockback(builder, KNOCKBACK);
	}
}
