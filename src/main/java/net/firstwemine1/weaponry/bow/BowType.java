package net.firstwemine1.weaponry.bow;

/**
 * Per-bow stats, from {@code pixel-art/weaponry/docs/bow-stats.md} §3. Each bow has one version only
 * (no material tiers). Vanilla's bow, for comparison: 20 draw ticks, speed 1, damage 1, spread 1.0,
 * durability 384.
 *
 * <ul>
 *   <li>{@code drawTicks}: ticks to full draw (vanilla 20). The vanilla power curve is stretched to fit.</li>
 *   <li>{@code speed}: arrow speed, x vanilla's 3.0 blocks/tick at full draw.</li>
 *   <li>{@code damage}: arrow base damage, x vanilla's 2.0.</li>
 *   <li>{@code spread}: inaccuracy (vanilla 1.0); lower is more accurate.</li>
 *   <li>{@code ability}: benefits and drawbacks, see {@link BowAbility}.</li>
 * </ul>
 */
public enum BowType {
	SELFBOW("selfbow", "Selfbow", 16, 0.9f, 0.9f, 1.25f, 256, new SelfbowAbility()),
	LONGBOW("longbow", "Longbow", 30, 1.3f, 1.0f, 0.6f, 384, new LongbowAbility()),
	RECURVE_BOW("recurve_bow", "Recurve Bow", 20, 1.15f, 1.0f, 0.9f, 432, new RecurveBowAbility()),
	HORSE_BOW("horse_bow", "Horse Bow", 12, 1.0f, 0.8f, 1.5f, 336, new HorseBowAbility());

	private final String id;
	private final String displayName;
	private final int drawTicks;
	private final float speed;
	private final float damage;
	private final float spread;
	private final int durability;
	private final BowAbility ability;

	BowType(String id, String displayName, int drawTicks, float speed, float damage, float spread, int durability,
			BowAbility ability) {
		this.id = id;
		this.displayName = displayName;
		this.drawTicks = drawTicks;
		this.speed = speed;
		this.damage = damage;
		this.spread = spread;
		this.durability = durability;
		this.ability = ability;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public int drawTicks() {
		return drawTicks;
	}

	public float speed() {
		return speed;
	}

	public float damage() {
		return damage;
	}

	public float spread() {
		return spread;
	}

	public int durability() {
		return durability;
	}

	public BowAbility ability() {
		return ability;
	}
}
