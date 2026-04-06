package com.example.module;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import com.example.context.ContextManager;
import com.example.context.PlayerContext;
import com.example.module.setting.ColorSetting;
import com.example.ui.ClientColors;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class OverlayModule extends Module {
	private static OverlayModule INSTANCE;
	private final ColorSetting primaryColor;
	private PlayerContext context = PlayerContext.IDLE;

	public OverlayModule() {
		super("overlay", ModuleCategory.RENDER, false);
		INSTANCE = this;
		this.primaryColor = addSetting(new ColorSetting("primaryColor", ClientColors.DEFAULT_PRIMARY_RGB));
		this.primaryColor.addChangeListener(() -> ClientColors.setPrimaryColorRgb(primaryColor.getRgb()));
		ClientColors.setPrimaryColorRgb(primaryColor.getRgb());
	}

	public static OverlayModule getInstance() {
		return INSTANCE;
	}

	public int getPrimaryColorRgb() {
		return primaryColor.getRgb();
	}

	@Override
	public KeyMapping createDefaultKeybind(String modId) {
		return new KeyMapping(
			"key.clientmodules.toggle_overlay",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			KeyMapping.Category.MISC
		);
	}

	@Override
	public void onTick(Minecraft client) {
		ContextManager contextManager = ContextManager.getInstance();
		if (contextManager == null || context != contextManager.getCurrentContext()) {
			ClientColors.setPrimaryColorRgb(primaryColor.getRgb());
		}
	}

	@Override
	public void onContextChanged(PlayerContext context) {
		this.context = context;
	}
}