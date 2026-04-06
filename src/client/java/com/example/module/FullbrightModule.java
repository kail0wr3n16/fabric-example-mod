package com.example.module;

import net.minecraft.client.Minecraft;

public class FullbrightModule extends Module {
	private static final double FULLBRIGHT_GAMMA = 1.0;
	private static boolean active;
	private Double previousGamma;

	public FullbrightModule() {
		super("fullbright", ModuleCategory.RENDER, false);
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