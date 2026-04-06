package com.example.module;

import com.example.context.PlayerContext;
import com.example.module.setting.NumberSetting;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FocusModeModule extends Module {
	private final ModuleManager moduleManager;
	private final NumberSetting radius;
	private final NumberSetting dimAlpha;
	private float currentAlpha;

	public FocusModeModule(ModuleManager moduleManager) {
		super("focusmode", ModuleCategory.RENDER, false);
		this.moduleManager = moduleManager;
		this.radius = addSetting(new NumberSetting("radius", 110.0, 60.0, 220.0));
		this.dimAlpha = addSetting(new NumberSetting("dimAlpha", 0.40, 0.10, 0.80));
	}

	@Override
	public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}

		float targetAlpha = isEnabled() ? dimAlpha.getValue().floatValue() : 0.0f;
		currentAlpha += (targetAlpha - currentAlpha) * 0.20f;
		if (currentAlpha <= 0.01f) {
			return;
		}

		int width = guiGraphics.guiWidth();
		int height = guiGraphics.guiHeight();
		int centerX = width / 2;
		int centerY = height / 2;
		int r = (int) Math.round(radius.getValue());
		int left = Math.max(0, centerX - r);
		int right = Math.min(width, centerX + r);
		int top = Math.max(0, centerY - r);
		int bottom = Math.min(height, centerY + r);

		int alpha = Math.max(0, Math.min(255, (int) Math.round(currentAlpha * 255.0)));
		int dimColor = (alpha << 24);

		guiGraphics.fill(0, 0, width, top, dimColor);
		guiGraphics.fill(0, bottom, width, height, dimColor);
		guiGraphics.fill(0, top, left, bottom, dimColor);
		guiGraphics.fill(right, top, width, bottom, dimColor);
	}

	@Override
	public void onContextChanged(PlayerContext context) {
		Module adaptiveModule = moduleManager.get("adaptiveui");
		if (!(adaptiveModule instanceof AdaptiveUiModule adaptiveUi) || !adaptiveUi.isAutoFocusInCombatEnabled()) {
			return;
		}
		setEnabled(context == PlayerContext.COMBAT);
	}
}
