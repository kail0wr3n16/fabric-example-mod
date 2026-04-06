package com.example.module;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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