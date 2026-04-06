package com.example.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudElement {
	private boolean enabled = true;
	private int x;
	private int y;

	protected HudElement(int defaultX, int defaultY) {
		this.x = defaultX;
		this.y = defaultY;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public abstract int getLineCount(Minecraft client);

	public abstract int getMaxWidth(Minecraft client);

	public abstract void render(GuiGraphicsExtractor guiGraphics, Minecraft client, int lineHeight, int lineGap, int textColor);
}