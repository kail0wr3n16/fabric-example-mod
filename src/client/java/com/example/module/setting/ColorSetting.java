package com.example.module.setting;

public class ColorSetting extends Setting<Integer> {
	public ColorSetting(String name, int defaultRgb) {
		super(name, defaultRgb & 0xFFFFFF);
	}

	@Override
	protected Integer sanitize(Integer value) {
		if (value == null) {
			return 0xFFFFFF;
		}
		return value & 0xFFFFFF;
	}

	public int getRgb() {
		return getValue() & 0xFFFFFF;
	}

	public int getRed() {
		return (getRgb() >> 16) & 0xFF;
	}

	public int getGreen() {
		return (getRgb() >> 8) & 0xFF;
	}

	public int getBlue() {
		return getRgb() & 0xFF;
	}

	public void setRgb(int rgb) {
		setValue(rgb & 0xFFFFFF);
	}

	public void setChannel(int channel, int channelValue) {
		int clamped = Math.max(0, Math.min(255, channelValue));
		int rgb = getRgb();
		switch (channel) {
			case 0 -> rgb = (rgb & 0x00FFFF) | (clamped << 16);
			case 1 -> rgb = (rgb & 0xFF00FF) | (clamped << 8);
			case 2 -> rgb = (rgb & 0xFFFF00) | clamped;
			default -> {
				return;
			}
		}
		setRgb(rgb);
	}
}
