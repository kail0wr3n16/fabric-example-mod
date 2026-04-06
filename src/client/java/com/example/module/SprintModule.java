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

		boolean hasForwardInput = client.player.input.hasForwardImpulse();
		boolean canSprint = hasForwardInput && !client.player.isCrouching() && !client.player.isUsingItem();
		client.player.setSprinting(canSprint);
	}
}