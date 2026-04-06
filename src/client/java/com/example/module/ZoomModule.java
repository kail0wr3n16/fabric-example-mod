package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;

import com.example.module.setting.NumberSetting;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class ZoomModule extends Module {
	private static final double SNAP_EPSILON = 0.02;
	private static final double MIN_SMOOTH_FACTOR = 0.02;
	private static final double MAX_SMOOTH_FACTOR = 0.30;
	private static final double SCROLL_STEP = 0.25;
	private static ZoomModule INSTANCE;

	private final NumberSetting zoomAmount;
	private final NumberSetting smoothness;
	private boolean zoomKeyDown;
	private double activeZoomOffset;
	private double animatedFov;

	public ZoomModule() {
		super("zoom", ModuleCategory.RENDER, true);
		this.zoomAmount = addSetting(new NumberSetting("zoomAmount", 3.0, 1.0, 10.0));
		this.smoothness = addSetting(new NumberSetting("smoothness", 0.20, 0.05, 0.45));
		this.activeZoomOffset = 0.0;
		this.animatedFov = Double.NaN;
		INSTANCE = this;
	}

	public static ZoomModule getInstance() {
		return INSTANCE;
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientmodules.hold_zoom",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_C,
			KeyMapping.Category.MISC
		);
	}

	@Override
	public boolean isHoldKeybind() {
		return true;
	}

	@Override
	public void onKeybindStateChanged(Minecraft client, boolean isDown) {
		boolean wasDown = zoomKeyDown;
		zoomKeyDown = isEnabled() && isDown;
		if (wasDown && !zoomKeyDown) {
			activeZoomOffset = 0.0;
		}
	}

	public boolean isZoomActive() {
		if (!isEnabled()) {
			return false;
		}
		boolean keyDownFallback = getKeybind() != null && getKeybind().isDown();
		return zoomKeyDown || keyDownFallback;
	}

	public boolean adjustZoomFromScroll(double scrollDelta) {
		if (!isZoomActive() || scrollDelta == 0.0) {
			return false;
		}

		double direction = Math.signum(scrollDelta);
		double currentTarget = zoomAmount.getValue() + activeZoomOffset;
		double nextTarget = currentTarget + (direction * SCROLL_STEP);
		nextTarget = Math.max(zoomAmount.getMin(), Math.min(zoomAmount.getMax(), nextTarget));
		activeZoomOffset = nextTarget - zoomAmount.getValue();
		return true;
	}

	public double applyRenderFov(double baseFov) {
		if (!isEnabled()) {
			animatedFov = Double.NaN;
			return baseFov;
		}

		boolean zoomActive = isZoomActive();

		if (Double.isNaN(animatedFov)) {
			animatedFov = baseFov;
		}

		double dynamicZoomAmount = zoomAmount.getValue() + activeZoomOffset;
		dynamicZoomAmount = Math.max(zoomAmount.getMin(), Math.min(zoomAmount.getMax(), dynamicZoomAmount));
		double targetFov = zoomActive ? (baseFov / dynamicZoomAmount) : baseFov;
		double factor = Math.max(MIN_SMOOTH_FACTOR, Math.min(MAX_SMOOTH_FACTOR, smoothness.getValue()));
		animatedFov = lerp(animatedFov, targetFov, factor);
		if (Math.abs(animatedFov - targetFov) < SNAP_EPSILON) {
			animatedFov = targetFov;
		}

		return animatedFov;
	}

	@Override
	protected void onDisable() {
		zoomKeyDown = false;
		activeZoomOffset = 0.0;
		animatedFov = Double.NaN;
	}

	private double lerp(double current, double target, double factor) {
		return current + ((target - current) * factor);
	}
}
