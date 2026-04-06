package com.example.ui;

public final class ClientColors {
	// Change this RGB value to theme all HUD text color.
	public static final int PRIMARY_RGB = rgb(84, 197, 255);
	public static final int PRIMARY_TEXT_ARGB = withAlpha(PRIMARY_RGB, 0xFF);

	private ClientColors() {
	}

	private static int rgb(int red, int green, int blue) {
		return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
	}

	private static int withAlpha(int rgb, int alpha) {
		return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
	}
}