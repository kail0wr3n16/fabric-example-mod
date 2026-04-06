package com.example.input;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import com.example.gui.ClickGuiScreen;
import com.example.gui.HudEditorScreen;
import com.example.module.Module;
import com.example.module.ModuleManager;
import com.example.ui.HudManager;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class InputHandler {
	private final ModuleManager moduleManager;
	private final HudManager hudManager;
	private final KeyMapping openGuiKey;
	private final KeyMapping openHudEditorKey;
	private final Map<String, Boolean> holdModuleKeyStates = new HashMap<>();
	private boolean reconciledLegacyBindings;

	public InputHandler(ModuleManager moduleManager, HudManager hudManager) {
		this.moduleManager = moduleManager;
		this.hudManager = hudManager;
		this.openGuiKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping(
				"key.clientmodules.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KeyMapping.Category.MISC
			)
		);
		this.openHudEditorKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping(
				"key.clientmodules.open_hud_editor",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				KeyMapping.Category.MISC
			)
		);
	}

	public void handleInput(Minecraft client) {
		reconcileLegacyBindings(client);

		boolean guiPressed = openGuiKey.consumeClick();
		boolean hudEditorPressed = openHudEditorKey.consumeClick();

		if (hudEditorPressed) {
			if (client.screen instanceof HudEditorScreen) {
				client.setScreen(null);
			} else {
				client.setScreen(new HudEditorScreen(moduleManager, hudManager));
			}
		}

		if (guiPressed && !(client.screen instanceof ClickGuiScreen)) {
			client.setScreen(new ClickGuiScreen(moduleManager, hudManager));
		}

		boolean allowModuleInput = client.screen == null;

		for (Module module : moduleManager.all()) {
			KeyMapping keybind = module.getKeybind();
			if (keybind == null) {
				continue;
			}

			if (module.isHoldKeybind()) {
				boolean keyDown = allowModuleInput && module.isEnabled() && keybind.isDown();
				boolean wasDown = holdModuleKeyStates.getOrDefault(module.getName(), false);
				if (keyDown != wasDown) {
					module.onKeybindStateChanged(client, keyDown);
					holdModuleKeyStates.put(module.getName(), keyDown);
				}
				continue;
			}

			if (allowModuleInput && keybind.consumeClick()) {
				module.toggle();
			}
		}
	}

	private void reconcileLegacyBindings(Minecraft client) {
		if (reconciledLegacyBindings) {
			return;
		}

		Module overlayModule = moduleManager.get("overlay");
		if (overlayModule == null || overlayModule.getKeybind() == null) {
			reconciledLegacyBindings = true;
			return;
		}

		InputConstants.Key overlayBound = KeyMappingHelper.getBoundKeyOf(overlayModule.getKeybind());
		if (overlayBound.getType() == InputConstants.Type.KEYSYM && overlayBound.getValue() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			overlayModule.getKeybind().setKey(overlayModule.getKeybind().getDefaultKey());
			KeyMapping.resetMapping();
			if (client.options != null) {
				client.options.save();
			}
		}

		reconciledLegacyBindings = true;
	}
}