package com.example.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class FpsHudElement extends HudElement {
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
	public int render(GuiGraphicsExtractor guiGraphics, Minecraft client, int x, int y, int lineHeight, int lineGap, int textColor) {
		Component fpsText = Component.literal("FPS: " + client.getFps());
		guiGraphics.text(client.font, fpsText, x, y, textColor, true);
		return y + lineHeight + lineGap;
	}
}