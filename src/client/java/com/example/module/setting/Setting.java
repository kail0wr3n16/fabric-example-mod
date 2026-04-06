package com.example.module.setting;

import java.util.Objects;

public abstract class Setting<T> {
	private final String name;
	private T value;
	private Runnable changeListener;

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
		T sanitized = sanitize(value);
		if (Objects.equals(this.value, sanitized)) {
			return;
		}

		this.value = sanitized;
		if (changeListener != null) {
			changeListener.run();
		}
	}

	public void setChangeListener(Runnable changeListener) {
		this.changeListener = changeListener;
	}

	protected T sanitize(T value) {
		return value;
	}
}