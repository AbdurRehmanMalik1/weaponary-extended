package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableMultimap;
import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Between the club and the war hammer. Flanged Head: ignores a quarter of the target's armor. Daze: full-charge
 * hits have a 15% chance to daze the target for 2 s.
 */
public class MaceAbility implements WeaponAbility {
	private static final float ARMOR_PIERCE = 0.25f;
	private static final float DAZE_CHANCE = 0.15f;
	private static final int DAZE_TICKS = 40;

	@Override
	public List<String> description() {
		return List.of(
				"Flanged Head: ignores a quarter of the target's armor",
				"Daze: full-charge hits have a 15% chance to Daze");
	}

	@Override
	public void addAttributes(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
		ClubAbility.addStoneKnockback(builder, material);
	}

	@Override
	public float armorPierce(WeaponMaterial material) {
		// Same twists as the war hammer's Armor Crush: a wooden head crushes half as well, gold is stronger.
		float pierce = ARMOR_PIERCE * material.abilityStrength();
		return material == WeaponMaterial.WOODEN ? pierce * 0.5f : pierce;
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		float strength = stack.getItem() instanceof WeaponItem weapon ? weapon.abilityStrength() : 1.0f;
		if (AttackContext.fullCharge(attacker) && attacker.getRandom().nextFloat() < DAZE_CHANCE * strength) {
			target.addEffect(new MobEffectInstance(ModEffects.DAZE.get(), DAZE_TICKS));
		}
	}
}
