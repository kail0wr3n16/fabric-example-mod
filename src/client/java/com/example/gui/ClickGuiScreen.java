package com.example.gui;

import com.example.module.Module;
import com.example.module.ModuleManager;
import com.example.ui.ClientColors;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClickGuiScreen extends Screen {
	private final ModuleManager moduleManager;

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Component.literal("My Client"));
		this.moduleManager = moduleManager;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		int panelWidth = 300;
		int panelHeight = Math.max(140, 42 + (moduleManager.all().size() * 14));
		int panelX = (this.width - panelWidth) / 2;
		int panelY = (this.height - panelHeight) / 2;

		// Backdrop and panel shell.
		guiGraphics.fill(0, 0, this.width, this.height, 0xAA0B0D12);
		guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0161A22);
		guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 20, 0xFF1F2633);
		guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, ClientColors.PRIMARY_TEXT_ARGB);

		guiGraphics.centeredText(this.font, this.title, panelX + (panelWidth / 2), panelY + 6, 0xFFFFFFFF);

		int lineY = panelY + 28;
		for (Module module : moduleManager.all()) {
			Component label = Component.literal(module.getName());
			Component state = Component.literal(module.isEnabled() ? "ON" : "OFF");

			guiGraphics.text(this.font, label, panelX + 12, lineY, 0xFFE6EAF2, false);
			int stateWidth = this.font.width(state);
			int stateColor = module.isEnabled() ? 0xFF77E38E : 0xFF9AA3B2;
			guiGraphics.text(this.font, state, panelX + panelWidth - 12 - stateWidth, lineY, stateColor, false);
			lineY += 14;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}
}