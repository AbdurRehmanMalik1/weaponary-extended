package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.effect.BleedEffect;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Draw Cut: the vanilla sword sweep with about 1.5x the area. Same trigger as vanilla: a full-charge,
 * grounded, non-sprinting, non-critical hit. Everything the sweep cuts, the main target included,
 * starts to Bleed.
 */
public class ScimitarAbility implements WeaponAbility {
	/** Vanilla inflates the target's box by (1, 0.25, 1) and caps at 3 blocks from the player. */
	private static final double AREA_SCALE = 1.5;
	private static final double MAX_DISTANCE = Math.sqrt(9.0 * AREA_SCALE);

	@Override
	public List<String> description() {
		return ImmutableList.of("Draw Cut: wide sweep on full-charge hits; the sweep causes Bleed");
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		WeaponItem weapon = Sweep.weaponFor(stack, attacker);
		if (weapon == null) {
			return;
		}
		PlayerEntity player = (PlayerEntity) attacker;
		Sweep.sweep((ServerWorld) player.level, player, target, stack, AREA_SCALE, MAX_DISTANCE,
				Sweep.vanillaDamage(player) * weapon.abilityStrength(),
				other -> BleedEffect.apply(other, player, weapon.material()));
		BleedEffect.apply(target, player, weapon.material());
	}
}
