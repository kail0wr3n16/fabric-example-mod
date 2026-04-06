package com.example.module.setting;

public abstract class Setting<T> {
	private final String name;
	private T value;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = sanitize(value);
	}

	protected T sanitize(T value) {
		return value;
	}
}