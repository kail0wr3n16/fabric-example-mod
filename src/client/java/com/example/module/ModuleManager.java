package com.example.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import com.example.ui.ClientColors;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ModuleManager {
	private static final int HUD_MARGIN = 6;
	private static final int HUD_LINE_HEIGHT = 10;

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

	public List<Module> getByCategory(ModuleCategory category) {
		List<Module> result = new ArrayList<>();
		for (Module module : modules.values()) {
			if (module.getCategory() == category) {
				result.add(module);
			}
		}
		return result;
	}

	public Map<ModuleCategory, List<Module>> getGroupedByCategory() {
		Map<ModuleCategory, List<Module>> grouped = new EnumMap<>(ModuleCategory.class);
		for (ModuleCategory category : ModuleCategory.values()) {
			grouped.put(category, new ArrayList<>());
		}

		for (Module module : modules.values()) {
			grouped.get(module.getCategory()).add(module);
		}

		return grouped;
	}

	public void registerKeybinds(String modId) {
		for (Module module : modules.values()) {
			KeyMapping keybind = module.createDefaultKeybind(modId);
			if (keybind != null) {
				KeyMapping registeredKeybind = KeyMappingHelper.registerKeyMapping(keybind);

				// Migrate legacy overlay binding that used Right Shift before GUI got it.
				if ("overlay".equals(module.getName())) {
					InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(registeredKeybind);
					if (bound.getType() == InputConstants.Type.KEYSYM && bound.getValue() == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) {
						registeredKeybind.setKey(registeredKeybind.getDefaultKey());
						KeyMapping.resetMapping();
					}
				}

				module.setKeybind(registeredKeybind);
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

		renderEnabledModulesList(guiGraphics);
	}

	private void renderEnabledModulesList(GuiGraphicsExtractor guiGraphics) {
		Minecraft client = Minecraft.getInstance();
		List<String> enabledModuleNames = new ArrayList<>();

		for (Module module : modules.values()) {
			if (module.isEnabled()) {
				enabledModuleNames.add(module.getName());
			}
		}

		for (int i = 0; i < enabledModuleNames.size(); i++) {
			String moduleName = enabledModuleNames.get(i);
			int textWidth = client.font.width(moduleName);
			int x = guiGraphics.guiWidth() - HUD_MARGIN - textWidth;
			int y = HUD_MARGIN + (i * HUD_LINE_HEIGHT);
			guiGraphics.text(client.font, moduleName, x, y, ClientColors.PRIMARY_TEXT_ARGB, true);
		}
	}
}