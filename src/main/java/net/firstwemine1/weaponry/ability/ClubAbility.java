package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import java.util.UUID;

import com.google.common.collect.ImmutableMultimap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** +1 knockback. Daze: full-charge hits have a 20% chance to daze the target for 2 s. */
public class ClubAbility implements WeaponAbility {
	private static final UUID KNOCKBACK_UUID = UUID.fromString("5f6c3a5e-7f0b-4d4e-9a63-2b1c0e9d4a71");
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
	public void addAttributes(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
		addKnockback(builder, KNOCKBACK + (material == WeaponMaterial.STONE ? STONE_KNOCKBACK : 0));
	}

	/** Shared with the war hammer and mace, the other blunt weapons. */
	static void addStoneKnockback(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
		if (material == WeaponMaterial.STONE) {
			addKnockback(builder, STONE_KNOCKBACK);
		}
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		float strength = stack.getItem() instanceof WeaponItem weapon ? weapon.abilityStrength() : 1.0f;
		if (AttackContext.fullCharge(attacker) && attacker.getRandom().nextFloat() < DAZE_CHANCE * strength) {
			target.addEffect(new MobEffectInstance(ModEffects.DAZE.get(), DAZE_TICKS), attacker);
		}
	}

	/** Shared with the other knockback weapons. */
	static void addKnockback(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, double amount) {
		builder.put(Attributes.ATTACK_KNOCKBACK,
				new AttributeModifier(KNOCKBACK_UUID, "Weapon modifier", amount, AttributeModifier.Operation.ADDITION));
	}
}
