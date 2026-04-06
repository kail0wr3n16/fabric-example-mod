package com.example.ui;

import com.example.module.Module;
import com.example.module.ModuleManager;
import com.example.ui.hud.CoordinatesHudElement;
import com.example.ui.hud.FpsHudElement;
import com.example.ui.hud.HudElement;
import com.example.ui.hud.ModuleListHudElement;
import com.example.ui.hud.WatermarkHudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudManager {
	private static final int OVERLAY_MARGIN = 6;
	private static final int OVERLAY_PADDING = 3;
	private static final int TEXT_HEIGHT = 9;
	private static final int LINE_GAP = 2;

	private final ModuleManager moduleManager;
	private final WatermarkHudElement watermarkElement = new WatermarkHudElement();
	private final FpsHudElement fpsElement = new FpsHudElement();
	private final CoordinatesHudElement coordinatesElement = new CoordinatesHudElement();
	private final ModuleListHudElement moduleListElement;

	public HudManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
		this.moduleListElement = new ModuleListHudElement(moduleManager);
	}

	public void renderHud(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
		Module overlayModule = moduleManager.get("overlay");
		if (overlayModule == null || !overlayModule.isEnabled()) {
			return;
		}

		renderTopLeftOverlay(guiGraphics);

		if (moduleListElement.isEnabled()) {
			Minecraft client = Minecraft.getInstance();
			moduleListElement.render(guiGraphics, client, 0, 0, TEXT_HEIGHT, LINE_GAP, ClientColors.PRIMARY_TEXT_ARGB);
		}
	}

	private void renderTopLeftOverlay(GuiGraphicsExtractor guiGraphics) {
		Minecraft client = Minecraft.getInstance();

		HudElement[] elements = new HudElement[] { watermarkElement, fpsElement, coordinatesElement };
		int lineCount = 0;
		int textWidth = 0;
		for (HudElement element : elements) {
			if (!element.isEnabled()) {
				continue;
			}
			lineCount += element.getLineCount(client);
			textWidth = Math.max(textWidth, element.getMaxWidth(client));
		}

		if (lineCount == 0) {
			return;
		}

		int x = Math.max(OVERLAY_MARGIN, guiGraphics.guiWidth() / 200);
		int y = Math.max(OVERLAY_MARGIN, guiGraphics.guiHeight() / 200);
		int boxHeight = (TEXT_HEIGHT * lineCount) + (LINE_GAP * (lineCount - 1));

		guiGraphics.fill(x - OVERLAY_PADDING, y - OVERLAY_PADDING, x + textWidth + OVERLAY_PADDING, y + boxHeight + OVERLAY_PADDING, 0x90000000);

		int nextY = y;
		for (HudElement element : elements) {
			if (!element.isEnabled()) {
				continue;
			}
			nextY = element.render(guiGraphics, client, x, nextY, TEXT_HEIGHT, LINE_GAP, ClientColors.PRIMARY_TEXT_ARGB);
		}
	}

	public void setElementEnabled(String elementId, boolean enabled) {
		switch (elementId.toLowerCase()) {
			case "watermark" -> watermarkElement.setEnabled(enabled);
			case "fps" -> fpsElement.setEnabled(enabled);
			case "coordinates" -> coordinatesElement.setEnabled(enabled);
			case "modulelist" -> moduleListElement.setEnabled(enabled);
			default -> {
			}
		}
	}

	public boolean isElementEnabled(String elementId) {
		return switch (elementId.toLowerCase()) {
			case "watermark" -> watermarkElement.isEnabled();
			case "fps" -> fpsElement.isEnabled();
			case "coordinates" -> coordinatesElement.isEnabled();
			case "modulelist" -> moduleListElement.isEnabled();
			default -> false;
		};
	}
}