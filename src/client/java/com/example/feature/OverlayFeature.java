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
		int x = Math.max(MARGIN, guiGraphics.guiWidth() / 200);
		int y = Math.max(MARGIN, guiGraphics.guiHeight() / 200);
		int textWidth = client.font.width(OVERLAY_TEXT);
		int textHeight = 9;

		guiGraphics.fill(x - PADDING, y - PADDING, x + textWidth + PADDING, y + textHeight + PADDING, 0x90000000);
		guiGraphics.text(client.font, OVERLAY_TEXT, x, y, 0xFFFFFFFF, true);
	}
}