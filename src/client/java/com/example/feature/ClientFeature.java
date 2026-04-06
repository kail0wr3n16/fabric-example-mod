package com.example.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface ClientFeature {
	String id();

	boolean isEnabled();

	void setEnabled(boolean enabled);

	default void toggle() {
		setEnabled(!isEnabled());
	}

	default void onTick(Minecraft client) {
	}

	default void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
	}
}