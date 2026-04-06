package com.example.context;

@FunctionalInterface
public interface ContextChangeListener {
	void onContextChanged(PlayerContext previous, PlayerContext current);
}
