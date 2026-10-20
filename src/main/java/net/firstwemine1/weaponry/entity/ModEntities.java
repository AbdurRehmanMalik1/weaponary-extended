package net.firstwemine1.weaponry.entity;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
	public static final String THROWN_WEAPON_ID = "thrown_weapon";
	public static final String ROPE_DART_ID = "rope_dart";

	private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
			DeferredRegister.create(ForgeRegistries.ENTITIES, Weaponry.MOD_ID);

	/** Same size and tracking as the vanilla trident (arrows set their own 0.13 eye height in this version). */
	public static final RegistryObject<EntityType<ThrownWeapon>> THROWN_WEAPON = ENTITY_TYPES.register(THROWN_WEAPON_ID,
			() -> EntityType.Builder.<ThrownWeapon>of(ThrownWeapon::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.clientTrackingRange(4)
					.updateInterval(20)
					.build(THROWN_WEAPON_ID));

	/** Small and tracked every tick: its position matters for the rope while it is anchored or tethered. */
	public static final RegistryObject<EntityType<RopeDart>> ROPE_DART = ENTITY_TYPES.register(ROPE_DART_ID,
			() -> EntityType.Builder.<RopeDart>of(RopeDart::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(4)
					.updateInterval(1)
					.build(ROPE_DART_ID));

	private ModEntities() {
	}

	public static void register(IEventBus modBus) {
		ENTITY_TYPES.register(modBus);
	}
}
