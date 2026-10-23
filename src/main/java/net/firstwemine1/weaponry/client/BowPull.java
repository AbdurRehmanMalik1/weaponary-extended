package net.firstwemine1.weaponry.client;

import com.mojang.serialization.MapCodec;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.bow.BowState;
import net.firstwemine1.weaponry.bow.ModBows;
import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code weaponry:bow/pull} item model property: how far a bow is drawn, measured against that bow's own
 * draw time (and any Quick Nock head start), so every bow reaches its last frame when it's fully drawn.
 * Vanilla's bow uses {@code use_duration} scaled for its fixed 1 s draw instead.
 */
public record BowPull() implements RangeSelectItemModelProperty {
	public static final ResourceLocation ID = Weaponry.id("bow/pull");
	public static final MapCodec<BowPull> MAP_CODEC = MapCodec.unit(new BowPull());

	/** Makes the property known to item model definitions. */
	static void register(RegisterRangeSelectItemModelPropertyEvent event) {
		event.register(ID, MAP_CODEC);
	}

	@Override
	public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
		WeaponBowItem bow = ModBows.of(stack);
		if (bow == null || entity == null || entity.getUseItem() != stack) {
			return 0.0f;
		}
		return (entity.getTicksUsingItem() + BowState.headStart(entity)) / (float) bow.drawTicks(entity);
	}

	@Override
	public MapCodec<BowPull> type() {
		return MAP_CODEC;
	}
}
