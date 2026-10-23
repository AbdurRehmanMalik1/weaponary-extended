package net.firstwemine1.weaponry.entity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final String THROWN_WEAPON_ID = "thrown_weapon";
	public static final String ROPE_DART_ID = "rope_dart";

	private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Weaponry.MOD_ID);

	/** Same size and tracking as the vanilla trident. */
	public static final DeferredHolder<EntityType<?>, EntityType<ThrownWeapon>> THROWN_WEAPON = ENTITY_TYPES.register(THROWN_WEAPON_ID,
			() -> EntityType.Builder.<ThrownWeapon>of(ThrownWeapon::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.eyeHeight(0.13f)
					.clientTrackingRange(4)
					.updateInterval(20)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Weaponry.id(THROWN_WEAPON_ID))));

	/** Small and tracked every tick: its position matters for the rope while it is anchored or tethered. */
	public static final DeferredHolder<EntityType<?>, EntityType<RopeDart>> ROPE_DART = ENTITY_TYPES.register(ROPE_DART_ID,
			() -> EntityType.Builder.<RopeDart>of(RopeDart::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.eyeHeight(0.13f)
					.clientTrackingRange(4)
					.updateInterval(1)
					.build(ResourceKey.create(Registries.ENTITY_TYPE, Weaponry.id(ROPE_DART_ID))));

	private ModEntities() {
	}

	public static void register(IEventBus modBus) {
		ENTITY_TYPES.register(modBus);
	}
}
