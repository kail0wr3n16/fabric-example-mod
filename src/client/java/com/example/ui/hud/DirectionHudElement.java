package com.example.ui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class DirectionHudElement extends HudElement {
	private static final int DEFAULT_X = 6;
	private static final int DEFAULT_Y = 50;

	public DirectionHudElement() {
		super(DEFAULT_X, DEFAULT_Y);
	}

	@Override
	public int getLineCount(Minecraft client) {
		return 1;
	}

	@Override
	public int getMaxWidth(Minecraft client) {
		return client.font.width(Component.literal("Facing: North"));
	}

	@Override
	public void render(GuiGraphicsExtractor guiGraphics, Minecraft client, int lineHeight, int lineGap, int textColor) {
		guiGraphics.text(client.font, buildDirectionText(client), getX(), getY(), textColor, true);
	}

	private Component buildDirectionText(Minecraft client) {
		if (client.player == null) {
			return Component.literal("Facing: -");
		}

		float yaw = client.player.getYRot();
		String dir;
		if (yaw >= -45.0f && yaw < 45.0f) {
			dir = "South";
		} else if (yaw >= 45.0f && yaw < 135.0f) {
			dir = "West";
		} else if (yaw >= -135.0f && yaw < -45.0f) {
			dir = "East";
		} else {
			dir = "North";
		}

		return Component.literal("Facing: " + dir);
	}
}
