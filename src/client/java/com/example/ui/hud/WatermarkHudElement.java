package com.example.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class WatermarkHudElement extends HudElement {
	private static final Component WATERMARK_TEXT = Component.literal("Client Active");

	@Override
	public int getLineCount(Minecraft client) {
		return 1;
	}

	@Override
	public int getMaxWidth(Minecraft client) {
		return client.font.width(WATERMARK_TEXT);
	}

	@Override
	public int render(GuiGraphicsExtractor guiGraphics, Minecraft client, int x, int y, int lineHeight, int lineGap, int textColor) {
		guiGraphics.text(client.font, WATERMARK_TEXT, x, y, textColor, true);
		return y + lineHeight + lineGap;
	}
}