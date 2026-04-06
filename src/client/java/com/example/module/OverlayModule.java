package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;

public class OverlayModule extends Module {
	public OverlayModule() {
		super("overlay", ModuleCategory.RENDER, false);
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientloaded.toggle_overlay",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			KeyMapping.Category.MISC
		);
	}
}