package com.example.ui.hud;

import java.util.ArrayList;
import java.util.List;

import com.example.module.Module;
import com.example.module.ModuleManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ModuleListHudElement extends HudElement {
	private static final int DEFAULT_X = 6;
	private static final int DEFAULT_Y = 6;
	private static final int HUD_LINE_HEIGHT = 10;

	private final ModuleManager moduleManager;

	public ModuleListHudElement(ModuleManager moduleManager) {
		super(DEFAULT_X, DEFAULT_Y);
		this.moduleManager = moduleManager;
	}

	@Override
	public int getLineCount(Minecraft client) {
		return 0;
	}

	@Override
	public int getMaxWidth(Minecraft client) {
		return 0;
	}

	@Override
	public void render(GuiGraphicsExtractor guiGraphics, Minecraft client, int lineHeight, int lineGap, int textColor) {
		List<String> enabledModuleNames = new ArrayList<>();

		for (Module module : moduleManager.all()) {
			if (module.isEnabled()) {
				enabledModuleNames.add(module.getName());
			}
		}

		for (int i = 0; i < enabledModuleNames.size(); i++) {
			String moduleName = enabledModuleNames.get(i);
			int textWidth = client.font.width(moduleName);
			int textX = guiGraphics.guiWidth() - getX() - textWidth;
			int textY = getY() + (i * HUD_LINE_HEIGHT);
			guiGraphics.text(client.font, moduleName, textX, textY, textColor, true);
		}
	}
}