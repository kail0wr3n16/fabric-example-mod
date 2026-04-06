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
	private final Map<String, Boolean> previousModuleKeyStates = new HashMap<>();
	private boolean previousGuiKeyDown;
	private boolean previousHudEditorKeyDown;
	private boolean reconciledLegacyBindings;

	public InputHandler(ModuleManager moduleManager, HudManager hudManager) {
		this.moduleManager = moduleManager;
		this.hudManager = hudManager;
		this.openGuiKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping(
				"key.clientloaded.open_gui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KeyMapping.Category.MISC
			)
		);
		this.openHudEditorKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping(
				"key.clientloaded.open_hud_editor",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				KeyMapping.Category.MISC
			)
		);
	}

	public void handleInput(Minecraft client) {
		reconcileLegacyBindings(client);

		boolean guiKeyDown = isBindingDown(client, openGuiKey);
		boolean guiPressed = guiKeyDown && !previousGuiKeyDown;
		previousGuiKeyDown = guiKeyDown;

		boolean hudEditorKeyDown = isBindingDown(client, openHudEditorKey);
		boolean hudEditorPressed = hudEditorKeyDown && !previousHudEditorKeyDown;
		previousHudEditorKeyDown = hudEditorKeyDown;

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

		boolean allowModuleToggle = client.screen == null;

		for (Module module : moduleManager.all()) {
			KeyMapping keybind = module.getKeybind();
			if (keybind == null) {
				continue;
			}

			boolean keyDown = isBindingDown(client, keybind);
			boolean wasDown = previousModuleKeyStates.getOrDefault(module.getName(), false);
			if (allowModuleToggle && keyDown && !wasDown) {
				module.toggle();
			}

			previousModuleKeyStates.put(module.getName(), keyDown);
		}
	}

	private boolean isBindingDown(Minecraft client, KeyMapping keybind) {
		InputConstants.Key boundKey = KeyMappingHelper.getBoundKeyOf(keybind);
		if (boundKey.getType() == InputConstants.Type.MOUSE) {
			return keybind.isDown();
		}

		return InputConstants.isKeyDown(client.getWindow(), boundKey.getValue());
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