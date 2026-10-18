package net.firstwemine1.weaponry.entity;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final String THROWN_WEAPON_ID = "thrown_weapon";
	public static final String ROPE_DART_ID = "rope_dart";

	/** Same size and tracking as the vanilla trident (arrows set their own 0.13 eye height in this version). */
	public static final EntityType<ThrownWeapon> THROWN_WEAPON = Registry.register(
			Registry.ENTITY_TYPE,
			Weaponry.id(THROWN_WEAPON_ID),
			EntityType.Builder.<ThrownWeapon>of(ThrownWeapon::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.clientTrackingRange(4)
					.updateInterval(20)
					.build(THROWN_WEAPON_ID));

	/**
	 * Small and tracked every tick: its position matters for the rope while it is anchored or tethered.
	 * A dart is only ever out while its thrower holds the rope, so it isn't saved with the chunk.
	 */
	public static final EntityType<RopeDart> ROPE_DART = Registry.register(
			Registry.ENTITY_TYPE,
			Weaponry.id(ROPE_DART_ID),
			EntityType.Builder.<RopeDart>of(RopeDart::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(4)
					.updateInterval(1)
					.noSave()
					.build(ROPE_DART_ID));

	private ModEntities() {
	}

	/** Loads the class, which registers the entity types. */
	public static void register() {
	}
}
