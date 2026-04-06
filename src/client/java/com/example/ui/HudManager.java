package com.example.ui;

import java.util.List;

import com.example.module.Module;
import com.example.module.ModuleManager;
import com.example.ui.hud.CoordinatesHudElement;
import com.example.ui.hud.DirectionHudElement;
import com.example.ui.hud.FpsHudElement;
import com.example.ui.hud.HudElement;
import com.example.ui.hud.ModuleListHudElement;
import com.example.ui.hud.WatermarkHudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudManager {
	private static final int OVERLAY_PADDING = 3;
	private static final int TEXT_HEIGHT = 9;
	private static final int LINE_GAP = 2;

	private final ModuleManager moduleManager;
	private final WatermarkHudElement watermarkElement = new WatermarkHudElement();
	private final FpsHudElement fpsElement = new FpsHudElement();
	private final CoordinatesHudElement coordinatesElement = new CoordinatesHudElement();
	private final DirectionHudElement directionElement = new DirectionHudElement();
	private final ModuleListHudElement moduleListElement;

	public record HudElementBounds(String elementId, String label, int x, int y, int width, int height, boolean enabled, boolean rightAligned) {
	}

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
			moduleListElement.render(guiGraphics, client, TEXT_HEIGHT, LINE_GAP, ClientColors.PRIMARY_TEXT_ARGB);
		}
	}

	private void renderTopLeftOverlay(GuiGraphicsExtractor guiGraphics) {
		Minecraft client = Minecraft.getInstance();

		HudElement[] elements = new HudElement[] { watermarkElement, fpsElement, coordinatesElement, directionElement };
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

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (HudElement element : elements) {
			if (!element.isEnabled()) {
				continue;
			}

			int elementX = element.getX();
			int elementY = element.getY();
			int elementWidth = element.getMaxWidth(client);
			int elementHeight = (TEXT_HEIGHT * element.getLineCount(client)) + (LINE_GAP * Math.max(0, element.getLineCount(client) - 1));

			minX = Math.min(minX, elementX);
			minY = Math.min(minY, elementY);
			maxX = Math.max(maxX, elementX + elementWidth);
			maxY = Math.max(maxY, elementY + elementHeight);
		}

		if (minX <= maxX && minY <= maxY) {
			guiGraphics.fill(minX - OVERLAY_PADDING, minY - OVERLAY_PADDING, maxX + OVERLAY_PADDING, maxY + OVERLAY_PADDING, 0x90000000);
		}

		for (HudElement element : elements) {
			if (!element.isEnabled()) {
				continue;
			}
			element.render(guiGraphics, client, TEXT_HEIGHT, LINE_GAP, ClientColors.PRIMARY_TEXT_ARGB);
		}
	}

	public void setElementEnabled(String elementId, boolean enabled) {
		switch (elementId.toLowerCase()) {
			case "watermark" -> watermarkElement.setEnabled(enabled);
			case "fps" -> fpsElement.setEnabled(enabled);
			case "coordinates" -> coordinatesElement.setEnabled(enabled);
			case "direction" -> directionElement.setEnabled(enabled);
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
			case "direction" -> directionElement.isEnabled();
			case "modulelist" -> moduleListElement.isEnabled();
			default -> false;
		};
	}

	public void setElementPosition(String elementId, int x, int y) {
		switch (elementId.toLowerCase()) {
			case "watermark" -> {
				watermarkElement.setX(x);
				watermarkElement.setY(y);
			}
			case "fps" -> {
				fpsElement.setX(x);
				fpsElement.setY(y);
			}
			case "coordinates" -> {
				coordinatesElement.setX(x);
				coordinatesElement.setY(y);
			}
			case "direction" -> {
				directionElement.setX(x);
				directionElement.setY(y);
			}
			case "modulelist" -> {
				moduleListElement.setX(x);
				moduleListElement.setY(y);
			}
			default -> {
			}
		}
	}

	public int getElementX(String elementId) {
		return switch (elementId.toLowerCase()) {
			case "watermark" -> watermarkElement.getX();
			case "fps" -> fpsElement.getX();
			case "coordinates" -> coordinatesElement.getX();
			case "direction" -> directionElement.getX();
			case "modulelist" -> moduleListElement.getX();
			default -> 0;
		};
	}

	public int getElementY(String elementId) {
		return switch (elementId.toLowerCase()) {
			case "watermark" -> watermarkElement.getY();
			case "fps" -> fpsElement.getY();
			case "coordinates" -> coordinatesElement.getY();
			case "direction" -> directionElement.getY();
			case "modulelist" -> moduleListElement.getY();
			default -> 0;
		};
	}

	public List<HudElementBounds> getElementBounds(Minecraft client, int guiWidth) {
		List<HudElementBounds> result = new java.util.ArrayList<>();
		result.add(buildBounds("watermark", "Watermark", watermarkElement, client, guiWidth, false));
		result.add(buildBounds("fps", "FPS", fpsElement, client, guiWidth, false));
		result.add(buildBounds("coordinates", "Coordinates", coordinatesElement, client, guiWidth, false));
		result.add(buildBounds("direction", "Direction", directionElement, client, guiWidth, false));
		result.add(buildBounds("modulelist", "Module List", moduleListElement, client, guiWidth, true));
		return result;
	}

	private HudElementBounds buildBounds(String id, String label, HudElement element, Minecraft client, int guiWidth, boolean rightAligned) {
		int width = Math.max(20, element.getMaxWidth(client));
		int lineCount = Math.max(1, element.getLineCount(client));
		int height = (TEXT_HEIGHT * lineCount) + (LINE_GAP * Math.max(0, lineCount - 1));
		int x = rightAligned ? guiWidth - element.getX() - width : element.getX();
		int y = element.getY();
		return new HudElementBounds(id, label, x, y, width, height, element.isEnabled(), rightAligned);
	}

	public void setElementPositionFromScreen(String elementId, int screenX, int screenY, int guiWidth, int elementWidth) {
		if ("modulelist".equalsIgnoreCase(elementId)) {
			int rightMargin = Math.max(0, guiWidth - screenX - elementWidth);
			setElementPosition(elementId, rightMargin, screenY);
			return;
		}

		setElementPosition(elementId, Math.max(0, screenX), Math.max(0, screenY));
	}

	public void resetAllElementPositions() {
		watermarkElement.resetPosition();
		fpsElement.resetPosition();
		coordinatesElement.resetPosition();
		directionElement.resetPosition();
		moduleListElement.resetPosition();
	}
}