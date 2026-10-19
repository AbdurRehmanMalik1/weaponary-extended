package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.WeaponTier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.component.Tool;

/**
 * Shield-breaker like an axe, and chops wood at axe speed for its tier. Throw: a short, spinning
 * throw doing melee damage +1.
 */
public class HatchetAbility extends ThrowAbility {
	public HatchetAbility() {
		super(1.0f, 1.5f);
	}

	@Override
	public List<String> description() {
		return List.of(
				"Shield Breaker: disables shields; chops wood like an axe",
				"Throw (hold use): +1 damage, short range");
	}

	@Override
	public boolean disablesShields() {
		return true;
	}

	@Override
	public Tool tool(WeaponTier tier) {
		return tier.createToolProperties(BlockTags.MINEABLE_WITH_AXE);
	}
}
