package net.firstwemine1.weaponry.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps long ability lines so the tooltip stays on screen. Vanilla draws each tooltip line at full
 * length, so a long one pushes the whole tooltip off the edge.
 */
public final class TooltipLines {
	/** Max line width in pixels, about 40 characters. */
	private static final int MAX_WIDTH = 200;
	private static final String INDENT = "  ";

	private TooltipLines() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, flag, lines) -> wrap(stack, lines));
	}

	/** Splits every line of a weapon's or bow's tooltip that is too wide; lines after the first are indented. */
	private static void wrap(ItemStack stack, List<Component> lines) {
		if (!(stack.getItem() instanceof WeaponItem) && !(stack.getItem() instanceof WeaponBowItem)) {
			return;
		}
		// Creative and recipe book search build their index from tooltips on worker threads, and Font may
		// only be used on the render thread. Search doesn't need the wrapping, so leave those lines alone.
		if (!RenderSystem.isOnRenderThread()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<Component> wrapped = new ArrayList<>(lines.size());
		for (Component line : lines) {
			if (font.width(line) <= MAX_WIDTH) {
				wrapped.add(line);
				continue;
			}
			List<FormattedText> parts = font.getSplitter().splitLines(line, MAX_WIDTH, line.getStyle());
			for (int i = 0; i < parts.size(); i++) {
				wrapped.add(Component.literal((i == 0 ? "" : INDENT) + parts.get(i).getString()).withStyle(line.getStyle()));
			}
		}
		lines.clear();
		lines.addAll(wrapped);
	}
}
