package com.example.module.setting;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;

	public NumberSetting(String name, double defaultValue, double min, double max) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		setValue(defaultValue);
	}

	@Override
	protected Double sanitize(Double value) {
		double sanitized = value;
		if (sanitized < min) {
			sanitized = min;
		}
		if (sanitized > max) {
			sanitized = max;
		}
		return sanitized;
	}

	public int asInt() {
		return (int) Math.round(getValue());
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}
}