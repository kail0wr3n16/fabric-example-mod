package com.example.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClickGuiScreen extends Screen {
	public ClickGuiScreen() {
		super(Component.literal("My Client"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		guiGraphics.fill(0, 0, this.width, this.height, 0xCC111111);
		guiGraphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}