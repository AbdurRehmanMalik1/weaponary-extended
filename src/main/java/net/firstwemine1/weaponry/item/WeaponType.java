package net.firstwemine1.weaponry.item;

import net.firstwemine1.weaponry.ability.ClubAbility;
import net.firstwemine1.weaponry.ability.DaggerAbility;
import net.firstwemine1.weaponry.ability.FlailAbility;
import net.firstwemine1.weaponry.ability.HalberdAbility;
import net.firstwemine1.weaponry.ability.HatchetAbility;
import net.firstwemine1.weaponry.ability.LanceAbility;
import net.firstwemine1.weaponry.ability.MorningStarAbility;
import net.firstwemine1.weaponry.ability.PikeAbility;
import net.firstwemine1.weaponry.ability.SabreAbility;
import net.firstwemine1.weaponry.ability.ScimitarAbility;
import net.firstwemine1.weaponry.ability.SpearAbility;
import net.firstwemine1.weaponry.ability.WarHammerAbility;

/**
 * Per-weapon handling stats and abilities, identical across material tiers. Values come from
 * {@code pixel-art/weaponry/docs/weapon-stats.md} sections 3 and 4.
 *
 * <ul>
 *   <li>{@code damageOffset}: added to the vanilla sword damage of the tier (sword = 0).</li>
 *   <li>{@code attackSpeed}: attacks per second at full charge (sword = 1.6).</li>
 *   <li>{@code reach}: entity interaction range in blocks (survival default = 3.0).</li>
 *   <li>{@code durabilityMultiplier}: applied to the tier's sword durability, rounded down.</li>
 *   <li>{@code twoHanded}: blocks the off hand while held, and can't be used from the off hand.</li>
 *   <li>{@code ability}: passive and active abilities, see {@link WeaponAbility}.</li>
 * </ul>
 */
public enum WeaponType {
	DAGGER("dagger", "Dagger", -2, 2.4f, 2.5, 0.75f, false, new DaggerAbility()),
	SABRE("sabre", "Sabre", -1, 2.0f, 3.0, 1.0f, false, new SabreAbility()),
	SCIMITAR("scimitar", "Scimitar", 0, 1.6f, 3.0, 1.0f, true, new ScimitarAbility()),
	SPEAR("spear", "Spear", -1, 1.4f, 4.0, 1.0f, false, new SpearAbility()),
	PIKE("pike", "Pike", 0, 1.0f, 5.0, 1.2f, true, new PikeAbility()),
	LANCE("lance", "Lance", -2, 0.8f, 4.5, 0.6f, true, new LanceAbility()),
	HALBERD("halberd", "Halberd", 2, 0.9f, 4.0, 1.2f, true, new HalberdAbility()),
	HATCHET("hatchet", "Hatchet", 1, 1.1f, 2.75, 1.0f, false, new HatchetAbility()),
	WAR_HAMMER("war_hammer", "War Hammer", 3, 0.7f, 3.0, 1.5f, true, new WarHammerAbility()),
	CLUB("club", "Club", -1, 1.2f, 3.0, 1.5f, false, new ClubAbility()),
	// Stats only for now; abilities still to come. The hook sword and rope dart abilities are
	// planned for milestone 2 (pixel-art/weaponry/docs/new-weapon-abilities.md).
	RAPIER("rapier", "Rapier", -1, 2.0f, 3.25, 0.8f, false),
	HOOK_SWORD("hook_sword", "Hook Sword", -1, 1.8f, 3.0, 1.0f, false),
	GREATSWORD("greatsword", "Greatsword", 3, 0.9f, 3.5, 1.3f, true),
	BATTLE_AXE("battle_axe", "Battle Axe", 4, 0.8f, 3.0, 1.2f, true),
	WAR_PICK("war_pick", "War Pick", 2, 0.9f, 3.0, 1.25f, false),
	GLAIVE("glaive", "Glaive", 1, 1.0f, 4.5, 1.2f, true),
	SCYTHE("scythe", "Scythe", 2, 0.8f, 4.0, 1.0f, true),
	MACE("mace", "Mace", 1, 1.1f, 3.0, 1.5f, false),
	MORNING_STAR("morning_star", "Morning Star", 2, 1.0f, 3.0, 1.25f, false, new MorningStarAbility()),
	FLAIL("flail", "Flail", 3, 0.8f, 3.0, 1.0f, false, new FlailAbility()),
	GAUNTLETS("gauntlets", "Gauntlets", -3, 2.6f, 2.5, 1.0f, false),
	ROPE_DART("rope_dart", "Rope Dart", -2, 1.6f, 4.5, 0.75f, false);

	private final String id;
	private final String displayName;
	private final int damageOffset;
	private final float attackSpeed;
	private final double reach;
	private final float durabilityMultiplier;
	private final boolean twoHanded;
	private final WeaponAbility ability;

	WeaponType(String id, String displayName, int damageOffset, float attackSpeed, double reach,
			float durabilityMultiplier, boolean twoHanded) {
		this(id, displayName, damageOffset, attackSpeed, reach, durabilityMultiplier, twoHanded, WeaponAbility.NONE);
	}

	WeaponType(String id, String displayName, int damageOffset, float attackSpeed, double reach,
			float durabilityMultiplier, boolean twoHanded, WeaponAbility ability) {
		this.id = id;
		this.displayName = displayName;
		this.damageOffset = damageOffset;
		this.attackSpeed = attackSpeed;
		this.reach = reach;
		this.durabilityMultiplier = durabilityMultiplier;
		this.twoHanded = twoHanded;
		this.ability = ability;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public int damageOffset() {
		return damageOffset;
	}

	public float attackSpeed() {
		return attackSpeed;
	}

	public double reach() {
		return reach;
	}

	public float durabilityMultiplier() {
		return durabilityMultiplier;
	}

	public boolean twoHanded() {
		return twoHanded;
	}

	public WeaponAbility ability() {
		return ability;
	}
}
