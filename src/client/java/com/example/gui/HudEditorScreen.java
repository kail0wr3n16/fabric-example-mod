package com.example.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.example.module.ModuleManager;
import com.example.ui.ClientColors;
import com.example.ui.HudManager;
import com.example.ui.HudManager.HudElementBounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
	private static final int BOX_PADDING = 3;

	private final ModuleManager moduleManager;
	private final HudManager hudManager;
	private final List<HudElementBounds> elementBounds = new ArrayList<>();
	private String draggingElementId;
	private int dragOffsetX;
	private int dragOffsetY;

	public HudEditorScreen(ModuleManager moduleManager, HudManager hudManager) {
		super(Component.literal("HUD Editor"));
		this.moduleManager = moduleManager;
		this.hudManager = hudManager;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		elementBounds.clear();
		Minecraft client = Minecraft.getInstance();
		elementBounds.addAll(hudManager.getElementBounds(client, this.width));

		guiGraphics.fill(0, 0, this.width, this.height, 0x55080A10);
		guiGraphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
		guiGraphics.centeredText(this.font, Component.literal("Drag boxes to reposition - Press H to exit"), this.width / 2, 20, 0xFFB8C3D6);

		for (HudElementBounds bounds : elementBounds) {
			int left = bounds.x() - BOX_PADDING;
			int top = bounds.y() - BOX_PADDING;
			int right = bounds.x() + bounds.width() + BOX_PADDING;
			int bottom = bounds.y() + bounds.height() + BOX_PADDING;

			int outlineColor = bounds.enabled() ? ClientColors.PRIMARY_TEXT_ARGB : 0xFF6F7D95;
			int fillColor = bounds.enabled() ? 0x22313D55 : 0x221D232F;
			guiGraphics.fill(left, top, right, bottom, fillColor);

			guiGraphics.fill(left, top, right, top + 1, outlineColor);
			guiGraphics.fill(left, bottom - 1, right, bottom, outlineColor);
			guiGraphics.fill(left, top, left + 1, bottom, outlineColor);
			guiGraphics.fill(right - 1, top, right, bottom, outlineColor);

			guiGraphics.text(this.font, Component.literal(bounds.label()), left + 3, top - 9, outlineColor, false);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return super.mouseClicked(event, bl);
		}

		double mouseX = event.x();
		double mouseY = event.y();
		for (HudElementBounds bounds : elementBounds) {
			int left = bounds.x() - BOX_PADDING;
			int top = bounds.y() - BOX_PADDING;
			int right = bounds.x() + bounds.width() + BOX_PADDING;
			int bottom = bounds.y() + bounds.height() + BOX_PADDING;
			if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
				draggingElementId = bounds.elementId();
				dragOffsetX = (int) Math.round(mouseX) - bounds.x();
				dragOffsetY = (int) Math.round(mouseY) - bounds.y();
				return true;
			}
		}

		return super.mouseClicked(event, bl);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingElementId == null || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return super.mouseDragged(event, dragX, dragY);
		}

		HudElementBounds bounds = findBounds(draggingElementId);
		if (bounds == null) {
			return true;
		}

		int targetX = (int) Math.round(event.x()) - dragOffsetX;
		int targetY = (int) Math.round(event.y()) - dragOffsetY;

		targetX = Math.max(0, Math.min(this.width - bounds.width(), targetX));
		targetY = Math.max(0, Math.min(this.height - bounds.height(), targetY));

		hudManager.setElementPositionFromScreen(bounds.elementId(), targetX, targetY, this.width, bounds.width());
		moduleManager.notifyConfigChanged();
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			draggingElementId = null;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	private HudElementBounds findBounds(String elementId) {
		for (HudElementBounds bounds : elementBounds) {
			if (bounds.elementId().equalsIgnoreCase(elementId)) {
				return bounds;
			}
		}
		return null;
	}
}