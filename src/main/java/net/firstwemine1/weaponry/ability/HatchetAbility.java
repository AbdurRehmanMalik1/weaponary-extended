package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;


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
		return ImmutableList.of(
				"Shield Breaker: disables shields; chops wood like an axe",
				"Throw (hold use): +1 damage, short range");
	}

	@Override
	public boolean disablesShields() {
		return true;
	}

	@Override
	public boolean choppingTool() {
		return true;
	}
}
