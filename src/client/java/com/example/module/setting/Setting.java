package com.example.module.setting;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public abstract class Setting<T> {
	private final String name;
	private T value;
	private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

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
		for (Runnable listener : changeListeners) {
			listener.run();
		}
	}

	public void setChangeListener(Runnable changeListener) {
		changeListeners.clear();
		if (changeListener != null) {
			changeListeners.add(changeListener);
		}
	}

	public void addChangeListener(Runnable changeListener) {
		if (changeListener != null) {
			changeListeners.add(changeListener);
		}
	}

	protected T sanitize(T value) {
		return value;
	}
}