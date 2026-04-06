package com.example.module;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class ModuleManager {
	private final Map<String, Module> modules = new LinkedHashMap<>();

	public void register(Module module) {
		modules.put(module.getName(), module);
	}

	public Module get(String name) {
		return modules.get(name);
	}

	public Collection<Module> all() {
		return modules.values();
	}

	public void registerKeybinds(String modId) {
		for (Module module : modules.values()) {
			KeyMapping keybind = module.createDefaultKeybind(modId);
			if (keybind != null) {
				module.setKeybind(KeyMappingHelper.registerKeyMapping(keybind));
			}
		}
	}

	public void handleKeyInput(Minecraft client) {
		for (Module module : modules.values()) {
			KeyMapping keybind = module.getKeybind();
			if (keybind == null) {
				continue;
			}

			while (keybind.consumeClick()) {
				module.toggle();
				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal(module.getName() + " " + (module.isEnabled() ? "enabled" : "disabled")));
				}
			}
		}
	}

	public void tick(Minecraft client) {
		for (Module module : modules.values()) {
			if (module.isEnabled()) {
				module.onTick(client);
			}
		}
	}

	public void renderHud(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
		for (Module module : modules.values()) {
			if (module.isEnabled()) {
				module.onHudRender(guiGraphics, tickCounter);
			}
		}
	}
}