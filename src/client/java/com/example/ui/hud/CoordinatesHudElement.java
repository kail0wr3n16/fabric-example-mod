package com.example.ui.hud;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class CoordinatesHudElement extends HudElement {
	private static final int DEFAULT_X = 6;
	private static final int DEFAULT_Y = 28;

	public CoordinatesHudElement() {
		super(DEFAULT_X, DEFAULT_Y);
	}

	@Override
	public int getLineCount(Minecraft client) {
		return 2;
	}

	@Override
	public int getMaxWidth(Minecraft client) {
		Component coordsText = buildCoordsText(client);
		Component speedText = buildSpeedText(client);
		return Math.max(client.font.width(coordsText), client.font.width(speedText));
	}

	@Override
	public void render(GuiGraphicsExtractor guiGraphics, Minecraft client, int lineHeight, int lineGap, int textColor) {
		Component coordsText = buildCoordsText(client);
		int x = getX();
		int y = getY();
		guiGraphics.text(client.font, coordsText, x, y, textColor, true);
		y += lineHeight + lineGap;

		Component speedText = buildSpeedText(client);
		guiGraphics.text(client.font, speedText, x, y, textColor, true);
	}

	private Component buildCoordsText(Minecraft client) {
		if (client.player == null) {
			return Component.literal("XYZ: -, -, -");
		}

		return Component.literal(String.format(Locale.ROOT, "XYZ: %.1f, %.1f, %.1f", client.player.getX(), client.player.getY(), client.player.getZ()));
	}

	private Component buildSpeedText(Minecraft client) {
		if (client.player == null) {
			return Component.literal("Speed: -");
		}

		double blocksPerSecond = client.player.getDeltaMovement().horizontalDistance() * 20.0;
		return Component.literal(String.format(Locale.ROOT, "Speed: %.2f b/s", blocksPerSecond));
	}
}