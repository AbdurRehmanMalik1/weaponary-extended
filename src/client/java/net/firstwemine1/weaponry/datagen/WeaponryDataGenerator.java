package net.firstwemine1.weaponry.datagen;

import net.firstwemine1.weaponry.client.BowPull;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class WeaponryDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		// The bow item models use it; the client initializer may not have run here.
		BowPull.register();
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(WeaponModelProvider::new);
		pack.addProvider(WeaponLanguageProvider::new);
		pack.addProvider(WeaponRecipeProvider::new);
		pack.addProvider(WeaponItemTagProvider::new);
	}
}
