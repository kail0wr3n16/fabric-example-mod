package com.example.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.context.ContextManager;
import com.example.context.PlayerContext;
import com.example.module.AdaptiveUiModule;
import com.example.module.Module;
import com.example.module.ModuleManager;
import com.example.ui.hud.CoordinatesHudElement;
import com.example.ui.hud.DirectionHudElement;
import com.example.ui.hud.FpsHudElement;
import com.example.ui.hud.HudElement;
import com.example.ui.hud.WatermarkHudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class HudManager {
	private static final int OVERLAY_PADDING = 3;
	private static final int TEXT_HEIGHT = 9;
	private static final int LINE_GAP = 2;
	private static final float ALPHA_LERP_RATE = 0.20f;
	private static final float COLOR_LERP_RATE = 0.12f;

	private final ModuleManager moduleManager;
	private ContextManager contextManager;
	private final WatermarkHudElement watermarkElement = new WatermarkHudElement();
	private final FpsHudElement fpsElement = new FpsHudElement();
	private final CoordinatesHudElement coordinatesElement = new CoordinatesHudElement();
	private final DirectionHudElement directionElement = new DirectionHudElement();
	private final Map<String, Float> elementAlpha = new HashMap<>();
	private int activeTextColor = ClientColors.PRIMARY_TEXT_ARGB;

	public record HudElementBounds(String elementId, String label, int x, int y, int width, int height, boolean enabled, boolean rightAligned) {
	}

	public HudManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
		elementAlpha.put("watermark", 1.0f);
		elementAlpha.put("fps", 1.0f);
		elementAlpha.put("coordinates", 1.0f);
		elementAlpha.put("direction", 1.0f);
	}

	public void setContextManager(ContextManager contextManager) {
		this.contextManager = contextManager;
	}

	public void renderHud(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
		moduleManager.renderModuleHud(guiGraphics, tickCounter);

		Module overlayModule = moduleManager.get("overlay");
		if (overlayModule == null || !overlayModule.isEnabled()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		PlayerContext context = contextManager == null ? PlayerContext.IDLE : contextManager.getCurrentContext();
		boolean adaptiveMode = isAdaptiveModeEnabled();
		boolean cleanUiMode = isCleanUiModeEnabled();

		updateTextColor(context, adaptiveMode, cleanUiMode);
		updateElementAlphas(context, adaptiveMode, cleanUiMode);

		renderTopLeftOverlay(guiGraphics, client, context, adaptiveMode, cleanUiMode);

		renderContextNotification(guiGraphics, client);
	}

	private void renderContextNotification(GuiGraphicsExtractor guiGraphics, Minecraft client) {
		if (contextManager == null) {
			return;
		}

		String label = contextManager.getActiveContextLabel();
		if (label.isEmpty()) {
			return;
		}

		float alpha = contextManager.getActiveContextLabelAlpha();
		if (alpha <= 0.01f) {
			return;
		}

		int textWidth = client.font.width(label);
		int x = (guiGraphics.guiWidth() - textWidth) / 2;
		int y = 14;
		int bg = withScaledAlpha(0xAA0D1018, alpha);
		guiGraphics.fill(x - 8, y - 3, x + textWidth + 8, y + 10, bg);
		guiGraphics.text(client.font, Component.literal(label), x, y, withScaledAlpha(0xFFFFFFFF, alpha), true);
	}

	private void renderTopLeftOverlay(GuiGraphicsExtractor guiGraphics, Minecraft client, PlayerContext context, boolean adaptiveMode, boolean cleanUiMode) {
		HudElement[] elements = new HudElement[] { watermarkElement, fpsElement, coordinatesElement, directionElement };

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;

		for (HudElement element : elements) {
			String elementId = getElementId(element);
			if (!element.isEnabled() || !shouldShowElement(elementId, context, adaptiveMode, cleanUiMode)) {
				continue;
			}
			float alpha = elementAlpha.getOrDefault(elementId, 0.0f);
			if (alpha <= 0.15f) {
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
			int backgroundAlpha = cleanUiMode ? 84 : 144;
			guiGraphics.fill(minX - OVERLAY_PADDING, minY - OVERLAY_PADDING, maxX + OVERLAY_PADDING, maxY + OVERLAY_PADDING,
				(backgroundAlpha << 24));
		}

		for (HudElement element : elements) {
			String elementId = getElementId(element);
			if (!element.isEnabled() || !shouldShowElement(elementId, context, adaptiveMode, cleanUiMode)) {
				continue;
			}
			float alpha = elementAlpha.getOrDefault(elementId, 0.0f);
			if (alpha <= 0.02f) {
				continue;
			}
			int color = withScaledAlpha(activeTextColor, alpha);
			element.render(guiGraphics, client, TEXT_HEIGHT, LINE_GAP, color);
		}

		if (adaptiveMode && context == PlayerContext.LOW_HEALTH) {
			int warningColor = withScaledAlpha(0xFFFF7E7E, 0.92f);
			guiGraphics.text(client.font, Component.literal("LOW HEALTH"), 6, 6 + TEXT_HEIGHT * 5, warningColor, true);
		}
	}

	private void updateElementAlphas(PlayerContext context, boolean adaptiveMode, boolean cleanUiMode) {
		for (String elementId : new String[] { "watermark", "fps", "coordinates", "direction" }) {
			boolean userEnabled = isElementEnabled(elementId);
			boolean visibleByContext = shouldShowElement(elementId, context, adaptiveMode, cleanUiMode);
			float target = (userEnabled && visibleByContext) ? 1.0f : 0.0f;
			float current = elementAlpha.getOrDefault(elementId, target);
			current += (target - current) * ALPHA_LERP_RATE;
			elementAlpha.put(elementId, current);
		}
	}

	private boolean shouldShowElement(String elementId, PlayerContext context, boolean adaptiveMode, boolean cleanUiMode) {
		if (!adaptiveMode) {
			return true;
		}

		return switch (context) {
			case COMBAT -> switch (elementId) {
				case "watermark", "fps" -> true;
				case "coordinates", "direction" -> !cleanUiMode;
				default -> false;
			};
			case LOW_HEALTH -> switch (elementId) {
				case "watermark", "fps", "coordinates" -> true;
				case "direction" -> !cleanUiMode;
				default -> false;
			};
			case MOVING -> switch (elementId) {
				case "watermark", "coordinates" -> true;
				case "direction" -> !cleanUiMode;
				default -> false;
			};
			case IDLE -> "watermark".equals(elementId);
		};
	}

	private void updateTextColor(PlayerContext context, boolean adaptiveMode, boolean cleanUiMode) {
		int targetColor = ClientColors.PRIMARY_TEXT_ARGB;
		if (adaptiveMode) {
			targetColor = switch (context) {
				case COMBAT -> 0xFFFFC07A;
				case LOW_HEALTH -> 0xFFFF8F8F;
				case MOVING -> cleanUiMode ? 0xFFE8EEF8 : 0xFF9FDFFF;
				case IDLE -> cleanUiMode ? 0xFFD9E0EC : ClientColors.PRIMARY_TEXT_ARGB;
			};
		}
		activeTextColor = lerpColor(activeTextColor, targetColor, COLOR_LERP_RATE);
	}

	private String getElementId(HudElement element) {
		if (element == watermarkElement) {
			return "watermark";
		}
		if (element == fpsElement) {
			return "fps";
		}
		if (element == coordinatesElement) {
			return "coordinates";
		}
		if (element == directionElement) {
			return "direction";
		}
		return "unknown";
	}

	private int withScaledAlpha(int argb, float alphaMultiplier) {
		int alpha = (argb >>> 24) & 0xFF;
		if (alpha == 0) {
			alpha = 255;
		}
		int scaled = Math.max(0, Math.min(255, (int) Math.round(alpha * alphaMultiplier)));
		return (argb & 0x00FFFFFF) | (scaled << 24);
	}

	private int lerpColor(int from, int to, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int fa = (from >>> 24) & 0xFF;
		int fr = (from >>> 16) & 0xFF;
		int fg = (from >>> 8) & 0xFF;
		int fb = from & 0xFF;
		int ta = (to >>> 24) & 0xFF;
		int tr = (to >>> 16) & 0xFF;
		int tg = (to >>> 8) & 0xFF;
		int tb = to & 0xFF;

		int a = (int) (fa + ((ta - fa) * t));
		int r = (int) (fr + ((tr - fr) * t));
		int g = (int) (fg + ((tg - fg) * t));
		int b = (int) (fb + ((tb - fb) * t));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private AdaptiveUiModule getAdaptiveUiModule() {
		Module module = moduleManager.get("adaptiveui");
		if (module instanceof AdaptiveUiModule adaptiveUiModule) {
			return adaptiveUiModule;
		}
		return null;
	}

	private boolean isAdaptiveModeEnabled() {
		return false;
	}

	private boolean isCleanUiModeEnabled() {
		return true;
	}

	public void setElementEnabled(String elementId, boolean enabled) {
		switch (elementId.toLowerCase()) {
			case "watermark" -> watermarkElement.setEnabled(enabled);
			case "fps" -> fpsElement.setEnabled(enabled);
			case "coordinates" -> coordinatesElement.setEnabled(enabled);
			case "direction" -> directionElement.setEnabled(enabled);
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
			default -> 0;
		};
	}

	public int getElementY(String elementId) {
		return switch (elementId.toLowerCase()) {
			case "watermark" -> watermarkElement.getY();
			case "fps" -> fpsElement.getY();
			case "coordinates" -> coordinatesElement.getY();
			case "direction" -> directionElement.getY();
			default -> 0;
		};
	}

	public List<HudElementBounds> getElementBounds(Minecraft client, int guiWidth) {
		List<HudElementBounds> result = new java.util.ArrayList<>();
		result.add(buildBounds("watermark", "Watermark", watermarkElement, client, guiWidth, false));
		result.add(buildBounds("fps", "FPS", fpsElement, client, guiWidth, false));
		result.add(buildBounds("coordinates", "Coordinates", coordinatesElement, client, guiWidth, false));
		result.add(buildBounds("direction", "Direction", directionElement, client, guiWidth, false));
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
		setElementPosition(elementId, Math.max(0, screenX), Math.max(0, screenY));
	}

	public void resetAllElementPositions() {
		watermarkElement.resetPosition();
		fpsElement.resetPosition();
		coordinatesElement.resetPosition();
		directionElement.resetPosition();
	}
}