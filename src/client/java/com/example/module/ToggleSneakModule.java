package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class ToggleSneakModule extends Module {
	public ToggleSneakModule() {
		super("toggleSneak", ModuleCategory.MOVEMENT, false);
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientloaded.toggle_sneak",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			KeyMapping.Category.MISC
		);
	}

	@Override
	public void onTick(Minecraft client) {
		if (client.options != null) {
			client.options.keyShift.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		Minecraft client = Minecraft.getInstance();
		if (client.options != null) {
			client.options.keyShift.setDown(false);
		}
	}
}
