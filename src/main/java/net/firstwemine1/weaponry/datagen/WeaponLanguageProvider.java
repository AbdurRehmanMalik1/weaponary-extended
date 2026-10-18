package net.firstwemine1.weaponry.datagen;

import java.util.List;

import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.entity.ModEntities;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

public class WeaponLanguageProvider extends FabricLanguageProvider {
	public WeaponLanguageProvider(FabricDataGenerator generator) {
		super(generator);
	}

	@Override
	public void generateTranslations(TranslationBuilder builder) {
		builder.add(ModItems.CREATIVE_TAB_KEY, "Weaponry");
		ModItems.all().forEach(item ->
				builder.add(item, item.material().displayName() + " " + item.type().displayName()));
		builder.add(ModEntities.THROWN_WEAPON, "Thrown Weapon");
		builder.add(ModEntities.ROPE_DART, "Rope Dart");

		builder.add(ModEffects.BLEED.getDescriptionId(), "Bleed");
		builder.add(ModEffects.STAGGER.getDescriptionId(), "Stagger");
		builder.add(ModEffects.STUN.getDescriptionId(), "Stun");
		builder.add(ModEffects.DAZE.getDescriptionId(), "Daze");
		// "message_id" of data/weaponry/damage_type/bleed.json
		builder.add("death.attack.weaponry.bleed", "%1$s bled out");
		builder.add("death.attack.weaponry.bleed.player", "%1$s bled out while fighting %2$s");

		for (WeaponType type : WeaponType.values()) {
			List<String> lines = type.ability().description();
			for (int i = 0; i < lines.size(); i++) {
				builder.add(WeaponItem.tooltipKey(type, i), lines.get(i));
			}
		}
		builder.add(WeaponItem.TWO_HANDED_TOOLTIP_KEY, "Two-handed: needs both hands");
		builder.add(WeaponItem.TWO_HANDED_MESSAGE_KEY, "%s needs both hands");
		builder.add(WeaponItem.TWO_HANDED_MOVED_KEY, "%s needs both hands: %s moved to your inventory");
		builder.add(WeaponItem.TWO_HANDED_FULL_KEY, "%s needs both hands: no room for %s, off hand blocked");
	}
}
