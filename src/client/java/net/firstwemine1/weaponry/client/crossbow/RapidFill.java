package net.firstwemine1.weaponry.client.crossbow;

import com.mojang.serialization.MapCodec;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code weaponry:crossbow/fill} item model property: how full the rapid crossbow's magazine is, so
 * the string draws back as it reloads.
 */
public record RapidFill() implements RangeSelectItemModelProperty {
	public static final Identifier ID = Weaponry.id("crossbow/fill");
	public static final MapCodec<RapidFill> MAP_CODEC = MapCodec.unit(new RapidFill());

	/** Makes the property known to item model definitions. Safe to call more than once. */
	public static void register() {
		RangeSelectItemModelProperties.ID_MAPPER.put(ID, MAP_CODEC);
	}

	@Override
	public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
		return RapidCrossbowItem.magazine(stack).count() / (float) Magazine.CAPACITY;
	}

	@Override
	public MapCodec<RapidFill> type() {
		return MAP_CODEC;
	}
}
