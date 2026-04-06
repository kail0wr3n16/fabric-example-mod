package com.example.module;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.module.setting.Setting;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class Module {
	private final String name;
	private boolean enabled;
	private KeyMapping keybind;
	private final Map<String, Setting<?>> settings = new LinkedHashMap<>();

	protected Module(String name, boolean enabledByDefault) {
		this.name = name;
		this.enabled = enabledByDefault;
	}

	public String getName() {
		return name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void toggle() {
		enabled = !enabled;
	}

	public KeyMapping getKeybind() {
		return keybind;
	}

	public void setKeybind(KeyMapping keybind) {
		this.keybind = keybind;
	}

	public KeyMapping createDefaultKeybind(String modId) {
		return null;
	}

	public <T extends Setting<?>> T addSetting(T setting) {
		settings.put(setting.getName(), setting);
		return setting;
	}

	public Setting<?> getSetting(String settingName) {
		return settings.get(settingName);
	}

	public Collection<Setting<?>> getSettings() {
		return settings.values();
	}

	public void onTick(Minecraft client) {
	}

	public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
	}
}