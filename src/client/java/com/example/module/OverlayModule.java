package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import com.example.ui.ClientColors;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class OverlayModule extends Module {
	private static final Component OVERLAY_TEXT = Component.literal("Client Active");
	private static final int MARGIN = 6;
	private static final int PADDING = 3;
	private final BooleanSetting showFps;
	private final NumberSetting coordDecimals;

	public OverlayModule() {
		super("overlay", false);
		this.showFps = addSetting(new BooleanSetting("showFps", true));
		this.coordDecimals = addSetting(new NumberSetting("coordDecimals", 1.0, 0.0, 3.0));
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientloaded.toggle_overlay",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT,
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(modId, "controls"))
		);
	}

	@Override
	public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
		renderTopLeftOverlay(guiGraphics);
	}

	private void renderTopLeftOverlay(GuiGraphicsExtractor guiGraphics) {
		Minecraft client = Minecraft.getInstance();
		Component fpsText = Component.literal("FPS: " + client.getFps());
		Component coordsText = buildCoordsText(client);
		boolean renderFps = showFps.isEnabled();
		int x = Math.max(MARGIN, guiGraphics.guiWidth() / 200);
		int y = Math.max(MARGIN, guiGraphics.guiHeight() / 200);
		int textWidth = Math.max(client.font.width(OVERLAY_TEXT), client.font.width(coordsText));
		if (renderFps) {
			textWidth = Math.max(textWidth, client.font.width(fpsText));
		}
		int textHeight = 9;
		int lineGap = 2;
		int lines = renderFps ? 3 : 2;
		int boxHeight = (textHeight * lines) + (lineGap * (lines - 1));

		guiGraphics.fill(x - PADDING, y - PADDING, x + textWidth + PADDING, y + boxHeight + PADDING, 0x90000000);
		guiGraphics.text(client.font, OVERLAY_TEXT, x, y, ClientColors.PRIMARY_TEXT_ARGB, true);

		int nextY = y + textHeight + lineGap;
		if (renderFps) {
			guiGraphics.text(client.font, fpsText, x, nextY, ClientColors.PRIMARY_TEXT_ARGB, true);
			nextY += textHeight + lineGap;
		}

		guiGraphics.text(client.font, coordsText, x, nextY, ClientColors.PRIMARY_TEXT_ARGB, true);
	}

	private Component buildCoordsText(Minecraft client) {
		if (client.player == null) {
			return Component.literal("XYZ: -, -, -");
		}

		int decimals = coordDecimals.asInt();
		String format = "XYZ: %." + decimals + "f, %." + decimals + "f, %." + decimals + "f";
		return Component.literal(String.format(format, client.player.getX(), client.player.getY(), client.player.getZ()));
	}
}