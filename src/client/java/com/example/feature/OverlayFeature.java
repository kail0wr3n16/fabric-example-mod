package com.example.feature;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class OverlayFeature extends BaseClientFeature {
	private static final Component OVERLAY_TEXT = Component.literal("Client Active");
	private static final int MARGIN = 6;
	private static final int PADDING = 3;

	public OverlayFeature() {
		super("overlay", false);
	}

	@Override
	public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
		renderTopLeftOverlay(guiGraphics);
	}

	private void renderTopLeftOverlay(GuiGraphicsExtractor guiGraphics) {
		Minecraft client = Minecraft.getInstance();
		Component fpsText = Component.literal("FPS: " + client.getFps());
		Component coordsText = buildCoordsText(client);
		int x = Math.max(MARGIN, guiGraphics.guiWidth() / 200);
		int y = Math.max(MARGIN, guiGraphics.guiHeight() / 200);
		int textWidth = Math.max(
			Math.max(client.font.width(OVERLAY_TEXT), client.font.width(fpsText)),
			client.font.width(coordsText)
		);
		int textHeight = 9;
		int lineGap = 2;
		int boxHeight = (textHeight * 3) + (lineGap * 2);

		guiGraphics.fill(x - PADDING, y - PADDING, x + textWidth + PADDING, y + boxHeight + PADDING, 0x90000000);
		guiGraphics.text(client.font, OVERLAY_TEXT, x, y, 0xFFFFFFFF, true);
		guiGraphics.text(client.font, fpsText, x, y + textHeight + lineGap, 0xFFFFFFFF, true);
		guiGraphics.text(client.font, coordsText, x, y + ((textHeight + lineGap) * 2), 0xFFFFFFFF, true);
	}

	private Component buildCoordsText(Minecraft client) {
		if (client.player == null) {
			return Component.literal("XYZ: -, -, -");
		}

		return Component.literal(String.format("XYZ: %.1f, %.1f, %.1f", client.player.getX(), client.player.getY(), client.player.getZ()));
	}
}