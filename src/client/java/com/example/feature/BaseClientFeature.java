package com.example.feature;

public abstract class BaseClientFeature implements ClientFeature {
	private final String id;
	private boolean enabled;

	protected BaseClientFeature(String id, boolean enabledByDefault) {
		this.id = id;
		this.enabled = enabledByDefault;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}

		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}
}