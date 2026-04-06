package com.example.feature;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FeatureManager {
	private final Map<String, ClientFeature> features = new LinkedHashMap<>();

	public void register(ClientFeature feature) {
		features.put(feature.id(), feature);
	}

	public ClientFeature get(String id) {
		return features.get(id);
	}

	public Collection<ClientFeature> all() {
		return features.values();
	}

	public void tick(Minecraft client) {
		for (ClientFeature feature : features.values()) {
			if (feature.isEnabled()) {
				feature.onTick(client);
			}
		}
	}

	public void renderHud(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
		for (ClientFeature feature : features.values()) {
			if (feature.isEnabled()) {
				feature.onHudRender(guiGraphics, tickCounter);
			}
		}
	}
}