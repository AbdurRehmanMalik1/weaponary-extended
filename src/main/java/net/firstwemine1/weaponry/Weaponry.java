package net.firstwemine1.weaponry;

import com.mojang.logging.LogUtils;
import net.firstwemine1.weaponry.ability.Abilities;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.client.WeaponryClient;
import net.firstwemine1.weaponry.combat.CombatEvents;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.testing.TestChest;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Forge entry point: registries on the mod bus, abilities and combat hooks on the Forge bus. */
@Mod(Weaponry.MOD_ID)
public final class Weaponry {
	public static final String MOD_ID = "weaponry";
	public static final Logger LOGGER = LogUtils.getLogger();

	private static WeaponryConfig config;

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static WeaponryConfig config() {
		return config;
	}

	public Weaponry() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		config = WeaponryConfig.load();

		ModItems.register(modBus);
		ModBows.register(modBus);
		ModEntities.register(modBus);
		ModEffects.register(modBus);
		Abilities.register();
		CombatEvents.register();

		if (config.testChest) {
			LOGGER.info("Test mode on (config/weaponry.json \"testChest\"): test chest will be placed at spawn");
			MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> TestChest.placeOrRefill(event.getServer()));
		}

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> WeaponryClient.init(modBus));
	}
}
