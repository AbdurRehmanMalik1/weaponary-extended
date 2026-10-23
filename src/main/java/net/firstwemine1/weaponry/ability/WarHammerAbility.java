package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.SoundEvents;
import net.minecraft.potion.EffectInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;

/**
 * Armor Crush: ignores half the target's armor. Stagger: full-charge hits stagger the target for
 * 1 s. Overhead Smash: a full-charge hit while sneaking knocks back everything around the target
 * and stuns it all briefly.
 */
public class WarHammerAbility implements WeaponAbility {
	private static final float ARMOR_PIERCE = 0.5f;
	private static final double SMASH_RADIUS = 2.5;
	private static final double SMASH_KNOCKBACK = 0.8;
	private static final int STAGGER_TICKS = 20;
	private static final int STUN_TICKS = 15;

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Armor Crush: ignores half of the target's armor",
				"Stagger: full-charge hits Stagger the target",
				"Overhead Smash (sneak + full-charge hit): smash the ground, knocking back and Stunning nearby enemies");
	}

	@Override
	public void addAttributes(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
		ClubAbility.addStoneKnockback(builder, material);
	}

	@Override
	public float armorPierce(WeaponMaterial material) {
		// Design sheet twists: a crude wooden head crushes half as well, gold abilities are stronger.
		float pierce = ARMOR_PIERCE * material.abilityStrength();
		return material == WeaponMaterial.WOODEN ? pierce * 0.5f : pierce;
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!AttackContext.fullCharge(attacker) || !(attacker.level instanceof ServerWorld)) {
			return;
		}
		ServerWorld level = (ServerWorld) attacker.level;
		float strength = stack.getItem() instanceof WeaponItem ? ((WeaponItem) stack.getItem()).abilityStrength() : 1.0f;
		target.addEffect(new EffectInstance(ModEffects.STAGGER.get(), Math.round(STAGGER_TICKS * strength)));
		if (attacker.isShiftKeyDown()) {
			smash(level, target, attacker, strength);
		}
	}

	private static void smash(ServerWorld level, LivingEntity center, LivingEntity attacker, float strength) {
		int stunTicks = Math.round(STUN_TICKS * strength);
		center.addEffect(new EffectInstance(ModEffects.STUN.get(), stunTicks));
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(SMASH_RADIUS))) {
			if (other == attacker || other == center || attacker.isAlliedTo(other)
					|| other instanceof ArmorStandEntity || other.distanceToSqr(center) > SMASH_RADIUS * SMASH_RADIUS) {
				continue;
			}
			// knockback() pushes away from the given direction, so pass the vector toward the center.
			other.knockback((float) (SMASH_KNOCKBACK * strength), center.getX() - other.getX(), center.getZ() - other.getZ());
			other.addEffect(new EffectInstance(ModEffects.STUN.get(), stunTicks));
		}

		BlockState ground = level.getBlockState(center.blockPosition().below());
		if (!ground.isAir()) {
			level.sendParticles(new BlockParticleData(ParticleTypes.BLOCK, ground),
					center.getX(), center.getY(), center.getZ(), 40, SMASH_RADIUS * 0.4, 0.1, SMASH_RADIUS * 0.4, 0.15);
		}
		level.playSound(null, center.getX(), center.getY(), center.getZ(),
				SoundEvents.ANVIL_LAND, attacker.getSoundSource(), 0.5f, 0.6f);
	}
}
