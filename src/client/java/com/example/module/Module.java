package com.example.module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class Module {
	private final String name;
	private boolean enabled;

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

	public void onTick(Minecraft client) {
	}

	public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
	}
}