package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** +1 knockback. Daze: full-charge hits have a 20% chance to daze the target for 2 s. */
public class ClubAbility implements WeaponAbility {
	private static final ResourceLocation KNOCKBACK_ID = Weaponry.id("weapon_knockback");
	private static final double KNOCKBACK = 1.0;
	/** Design sheet twist: stone blunt weapons hit 1 harder. */
	private static final double STONE_KNOCKBACK = 1.0;
	private static final float DAZE_CHANCE = 0.2f;
	private static final int DAZE_TICKS = 40;

	@Override
	public List<String> description() {
		return List.of("Daze: full-charge hits have a 20% chance to Daze; +1 knockback");
	}

	@Override
	public void addAttributes(ItemAttributeModifiers.Builder builder, WeaponMaterial material) {
		addKnockback(builder, KNOCKBACK + (material == WeaponMaterial.STONE ? STONE_KNOCKBACK : 0));
	}

	/** Shared with the war hammer and mace, the other blunt weapons. */
	static void addStoneKnockback(ItemAttributeModifiers.Builder builder, WeaponMaterial material) {
		if (material == WeaponMaterial.STONE) {
			addKnockback(builder, STONE_KNOCKBACK);
		}
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		float strength = stack.getItem() instanceof WeaponItem weapon ? weapon.abilityStrength() : 1.0f;
		if (AttackContext.fullCharge(attacker) && attacker.getRandom().nextFloat() < DAZE_CHANCE * strength) {
			target.addEffect(new MobEffectInstance(ModEffects.DAZE, DAZE_TICKS), attacker);
		}
	}

	/** Shared with the other knockback weapons. */
	static void addKnockback(ItemAttributeModifiers.Builder builder, double amount) {
		builder.add(Attributes.ATTACK_KNOCKBACK,
				new AttributeModifier(KNOCKBACK_ID, amount, AttributeModifier.Operation.ADD_VALUE),
				EquipmentSlotGroup.MAINHAND);
	}
}
