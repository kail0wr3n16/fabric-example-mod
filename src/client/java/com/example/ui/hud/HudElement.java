package com.example.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudElement {
	private boolean enabled = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public abstract int getLineCount(Minecraft client);

	public abstract int getMaxWidth(Minecraft client);

	public abstract int render(GuiGraphicsExtractor guiGraphics, Minecraft client, int x, int y, int lineHeight, int lineGap, int textColor);
}