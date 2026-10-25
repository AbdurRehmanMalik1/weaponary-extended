package net.firstwemine1.weaponry.client;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

/**
 * Wraps long ability lines so the tooltip stays on screen. Vanilla 1.16.5 draws each tooltip line
 * at full length, so a long one pushes the whole tooltip off the edge. Client only: it is called
 * from {@code appendHoverText}, which only runs on the client.
 */
public final class TooltipLines {
	/** Max line width in pixels, about 40 characters. */
	private static final int MAX_WIDTH = 200;
	private static final String INDENT = "  ";

	private TooltipLines() {
	}

	/** Adds {@code line} to {@code tooltip}, split to fit; lines after the first are indented. */
	public static void add(List<Component> tooltip, Component line, ChatFormatting color) {
		// Search builds its index from tooltips and may do so off the render thread, where Font must not be
		// used. Search doesn't need the wrapping, so add the line whole.
		if (!RenderSystem.isOnRenderThread()) {
			tooltip.add(line.copy().withStyle(color));
			return;
		}
		List<FormattedText> parts = Minecraft.getInstance().font.getSplitter()
				.splitLines(line, MAX_WIDTH, Style.EMPTY);
		for (int i = 0; i < parts.size(); i++) {
			String text = (i == 0 ? "" : INDENT) + parts.get(i).getString();
			tooltip.add(new TextComponent(text).withStyle(color));
		}
	}
}
