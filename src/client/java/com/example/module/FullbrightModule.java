package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;
import com.example.context.PlayerContext;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class FullbrightModule extends Module {
	private static final double BASE_GAMMA = 0.85;
	private static final double HIGH_GAMMA = 1.0;
	private static final double MIN_GAMMA = 0.0;
	private static final double MAX_GAMMA = 1.0;
	private static boolean active;
	private PlayerContext currentContext = PlayerContext.IDLE;
	private Double previousGamma;

	public FullbrightModule() {
		super("fullbright", ModuleCategory.RENDER, false);
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientmodules.toggle_fullbright",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			KeyMapping.Category.MISC
		);
	}

	@Override
	protected void onEnable() {
		Minecraft client = Minecraft.getInstance();
		if (client.options == null) {
			return;
		}

		active = true;

		if (previousGamma == null) {
			previousGamma = client.options.gamma().get();
		}

		client.options.gamma().set(getTargetGamma(client));
	}

	@Override
	public void onTick(Minecraft client) {
		double targetGamma = getTargetGamma(client);
		if (client.options != null && Math.abs(client.options.gamma().get() - targetGamma) > 0.0001) {
			client.options.gamma().set(targetGamma);
		}
	}

	@Override
	public void onContextChanged(PlayerContext context) {
		this.currentContext = context;
	}

	@Override
	protected void onDisable() {
		Minecraft client = Minecraft.getInstance();
		active = false;

		if (client.options == null || previousGamma == null) {
			return;
		}

		client.options.gamma().set(previousGamma);
		previousGamma = null;
	}

	public static boolean isActive() {
		return active;
	}

	private double getTargetGamma(Minecraft client) {
		if (currentContext == PlayerContext.LOW_HEALTH) {
			return clampGamma(HIGH_GAMMA);
		}
		if (client.player != null && client.player.getLightLevelDependentMagicValue() < 0.45f) {
			return clampGamma(HIGH_GAMMA);
		}
		return clampGamma(BASE_GAMMA);
	}

	private double clampGamma(double value) {
		return Math.max(MIN_GAMMA, Math.min(MAX_GAMMA, value));
	}
}