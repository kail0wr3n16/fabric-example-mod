package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import com.example.module.setting.ColorSetting;
import com.example.ui.ClientColors;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class OverlayModule extends Module {
	private final ColorSetting primaryColor;

	public OverlayModule() {
		super("overlay", ModuleCategory.RENDER, false);
		this.primaryColor = addSetting(new ColorSetting("primaryColor", ClientColors.DEFAULT_PRIMARY_RGB));
		this.primaryColor.addChangeListener(() -> ClientColors.setPrimaryColorRgb(primaryColor.getRgb()));
		ClientColors.setPrimaryColorRgb(primaryColor.getRgb());
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

	@Override
	public void onTick(Minecraft client) {
		ClientColors.setPrimaryColorRgb(primaryColor.getRgb());
	}
}