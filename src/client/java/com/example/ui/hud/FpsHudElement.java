package com.example.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class FpsHudElement extends HudElement {
	private static final int DEFAULT_X = 6;
	private static final int DEFAULT_Y = 17;

	public FpsHudElement() {
		super(DEFAULT_X, DEFAULT_Y);
	}

	@Override
	public int getLineCount(Minecraft client) {
		return 1;
	}

	@Override
	public int getMaxWidth(Minecraft client) {
		Component fpsText = Component.literal("FPS: " + client.getFps());
		return client.font.width(fpsText);
	}

	@Override
	public void render(GuiGraphicsExtractor guiGraphics, Minecraft client, int lineHeight, int lineGap, int textColor) {
		Component fpsText = Component.literal("FPS: " + client.getFps());
		guiGraphics.text(client.font, fpsText, getX(), getY(), textColor, true);
	}
}