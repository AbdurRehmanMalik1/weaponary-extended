package net.firstwemine1.weaponry.client;

import com.mojang.serialization.MapCodec;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code weaponry:flail/spin} item model property: which wind-up frame a flail shows while you hold use
 * (0, 1, 2, 0, ...), {@value #TICKS_PER_FRAME} ticks each. 0 when it isn't being used.
 */
public record FlailSpin() implements RangeSelectItemModelProperty {
	public static final ResourceLocation ID = Weaponry.id("flail/spin");
	public static final MapCodec<FlailSpin> MAP_CODEC = MapCodec.unit(new FlailSpin());
	public static final int FRAMES = 3;
	private static final int TICKS_PER_FRAME = 2;

	/** Makes the property known to item model definitions. */
	static void register(RegisterRangeSelectItemModelPropertyEvent event) {
		event.register(ID, MAP_CODEC);
	}

	@Override
	public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
		if (entity == null || entity.getUseItem() != stack
				|| !(stack.getItem() instanceof WeaponItem weapon) || weapon.type() != WeaponType.FLAIL) {
			return 0.0f;
		}
		return entity.getTicksUsingItem() / TICKS_PER_FRAME % FRAMES;
	}

	@Override
	public MapCodec<FlailSpin> type() {
		return MAP_CODEC;
	}
}
