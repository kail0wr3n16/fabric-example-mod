package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class SprintModule extends Module {
	public SprintModule() {
		super("sprint", ModuleCategory.MOVEMENT, false);
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientloaded.toggle_sprint",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			KeyMapping.Category.MISC
		);
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