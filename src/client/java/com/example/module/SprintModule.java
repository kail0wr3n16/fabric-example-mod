package com.example.module;

import net.minecraft.client.Minecraft;

public class SprintModule extends Module {
	public SprintModule() {
		super("sprint", false);
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.player == null || client.player.input == null) {
			return;
		}

		boolean isMoving = client.player.input.getMoveVector().lengthSquared() > 0.0F;
		client.player.setSprinting(isMoving);
	}
}