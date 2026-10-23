package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.item.WeaponTier;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

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
		// The same rules ToolMaterial.applyToolProperties gives an axe.
		HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
		return new Tool(List.of(
				Tool.Rule.deniesDrops(blocks.getOrThrow(tier.base().incorrectBlocksForDrops())),
				Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_AXE), tier.base().speed())),
				1.0f, 1);
	}
}
