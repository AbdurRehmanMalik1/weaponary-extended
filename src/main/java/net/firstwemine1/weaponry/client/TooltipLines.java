package net.firstwemine1.weaponry.client;

import java.util.ArrayList;
import java.util.List;

import net.firstwemine1.weaponry.bow.WeaponBowItem;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;

/**
 * Wraps long ability lines so the tooltip stays on screen. Vanilla draws each tooltip line at full
 * length, so a long one pushes the whole tooltip off the edge. Called from the item tooltip event.
 */
public final class TooltipLines {
	/** Max line width in pixels, about 40 characters. */
	private static final int MAX_WIDTH = 200;
	private static final String INDENT = "  ";

	private TooltipLines() {
	}

	/** Splits every line of a weapon's or bow's tooltip that is too wide; lines after the first are indented. */
	public static void wrap(ItemStack stack, List<ITextComponent> lines) {
		if (!(stack.getItem() instanceof WeaponItem) && !(stack.getItem() instanceof WeaponBowItem)) {
			return;
		}
		FontRenderer font = Minecraft.getInstance().font;
		List<ITextComponent> wrapped = new ArrayList<>(lines.size());
		for (ITextComponent line : lines) {
			if (font.width(line) <= MAX_WIDTH) {
				wrapped.add(line);
				continue;
			}
			List<ITextProperties> parts = font.getSplitter().splitLines(line, MAX_WIDTH, line.getStyle());
			for (int i = 0; i < parts.size(); i++) {
				String text = (i == 0 ? "" : INDENT) + parts.get(i).getString();
				wrapped.add(new StringTextComponent(text).withStyle(line.getStyle()));
			}
		}
		lines.clear();
		lines.addAll(wrapped);
	}
}
