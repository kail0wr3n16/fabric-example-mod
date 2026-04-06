package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class FullbrightModule extends Module {
	private static final double FULLBRIGHT_GAMMA = 1.0;
	private static boolean active;
	private Double previousGamma;

	public FullbrightModule() {
		super("fullbright", ModuleCategory.RENDER, false);
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientloaded.toggle_fullbright",
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

		client.options.gamma().set(FULLBRIGHT_GAMMA);
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.options != null && client.options.gamma().get() != FULLBRIGHT_GAMMA) {
			client.options.gamma().set(FULLBRIGHT_GAMMA);
		}
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
}