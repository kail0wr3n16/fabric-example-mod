package com.example.module;

import net.minecraft.client.Minecraft;

public class FullbrightModule extends Module {
	private static final double FULLBRIGHT_GAMMA = 16.0;
	private Double previousGamma;

	public FullbrightModule() {
		super("fullbright", false);
	}

	@Override
	protected void onEnable() {
		Minecraft client = Minecraft.getInstance();
		if (client.options == null) {
			return;
		}

		if (previousGamma == null) {
			previousGamma = client.options.gamma().get();
		}

		client.options.gamma().set(FULLBRIGHT_GAMMA);
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.options != null) {
			client.options.gamma().set(FULLBRIGHT_GAMMA);
		}
	}

	@Override
	protected void onDisable() {
		Minecraft client = Minecraft.getInstance();
		if (client.options == null || previousGamma == null) {
			return;
		}

		client.options.gamma().set(previousGamma);
		previousGamma = null;
	}
}