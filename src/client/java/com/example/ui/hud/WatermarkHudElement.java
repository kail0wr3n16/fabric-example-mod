package com.example.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class WatermarkHudElement extends HudElement {
	private static final Component WATERMARK_TEXT = Component.literal("Client Active");
	private static final int DEFAULT_X = 6;
	private static final int DEFAULT_Y = 6;

	public WatermarkHudElement() {
		super(DEFAULT_X, DEFAULT_Y);
	}

	@Override
	public int getLineCount(Minecraft client) {
		return 1;
	}

	@Override
	public int getMaxWidth(Minecraft client) {
		return client.font.width(WATERMARK_TEXT);
	}

	@Override
	public void render(GuiGraphicsExtractor guiGraphics, Minecraft client, int lineHeight, int lineGap, int textColor) {
		guiGraphics.text(client.font, WATERMARK_TEXT, getX(), getY(), textColor, true);
	}
}