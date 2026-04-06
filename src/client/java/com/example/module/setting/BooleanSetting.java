package com.example.module.setting;

public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	public boolean isEnabled() {
		return getValue();
	}
}