package net.firstwemine1.weaponry.client.crossbow;

import com.mojang.serialization.MapCodec;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import org.jetbrains.annotations.Nullable;

/** The {@code weaponry:crossbow/loaded} item model property: a rapid crossbow with arrows, not reloading. */
public record RapidLoaded() implements ConditionalItemModelProperty {
	public static final ResourceLocation ID = Weaponry.id("crossbow/loaded");
	public static final MapCodec<RapidLoaded> MAP_CODEC = MapCodec.unit(new RapidLoaded());

	public static void register(RegisterConditionalItemModelPropertyEvent event) {
		event.register(ID, MAP_CODEC);
	}

	@Override
	public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
		return RapidCrossbowItem.looksLoaded(stack, entity);
	}

	@Override
	public MapCodec<RapidLoaded> type() {
		return MAP_CODEC;
	}
}
