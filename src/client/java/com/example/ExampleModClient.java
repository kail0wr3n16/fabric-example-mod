package com.example;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.lwjgl.glfw.GLFW;

import com.example.feature.ClientFeature;
import com.example.feature.FeatureManager;
import com.example.feature.OverlayFeature;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ExampleModClient implements ClientModInitializer {
	private static final String MOD_ID = "clientloaded";
	private static final Identifier FEATURES_HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "features_hud");
	private final FeatureManager featureManager = new FeatureManager();

	@Override
	public void onInitializeClient() {
		featureManager.register(new OverlayFeature());

		KeyMapping toggleOverlayKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping(
				"key.clientloaded.toggle_overlay",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "controls"))
			)
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleOverlayKey.consumeClick()) {
				ClientFeature overlayFeature = featureManager.get("overlay");
				overlayFeature.toggle();

				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal("Overlay " + (overlayFeature.isEnabled() ? "enabled" : "disabled")));
				}
			}

			featureManager.tick(client);
		});

		HudElementRegistry.addLast(FEATURES_HUD_ID, featureManager::renderHud);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(
				ClientCommands.literal("clientloaded")
					.executes(context -> {
						context.getSource().sendFeedback(Component.literal("Usage: /clientloaded ping"));
						return 1;
					})
					.then(
						ClientCommands.literal("ping")
							.executes(context -> {
								context.getSource().sendFeedback(Component.literal("Pong from clientloaded"));
								return 1;
							})
					)
					.then(
						ClientCommands.literal("features")
							.executes(context -> {
								StringBuilder status = new StringBuilder("Features: ");
								for (ClientFeature feature : featureManager.all()) {
									if (status.length() > "Features: ".length()) {
										status.append(", ");
									}
									status.append(feature.id()).append("=").append(feature.isEnabled() ? "on" : "off");
								}
								context.getSource().sendFeedback(Component.literal(status.toString()));
								return 1;
							})
					)
					.then(
						ClientCommands.literal("feature")
							.then(
								ClientCommands.argument("name", StringArgumentType.word())
									.then(
										ClientCommands.literal("on")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												ClientFeature feature = featureManager.get(name);
												if (feature == null) {
													context.getSource().sendFeedback(Component.literal("Unknown feature: " + name));
													return 0;
												}

												feature.setEnabled(true);
												context.getSource().sendFeedback(Component.literal(name + " enabled"));
												return 1;
											})
									)
									.then(
										ClientCommands.literal("off")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												ClientFeature feature = featureManager.get(name);
												if (feature == null) {
													context.getSource().sendFeedback(Component.literal("Unknown feature: " + name));
													return 0;
												}

												feature.setEnabled(false);
												context.getSource().sendFeedback(Component.literal(name + " disabled"));
												return 1;
											})
									)
									.then(
										ClientCommands.literal("toggle")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												ClientFeature feature = featureManager.get(name);
												if (feature == null) {
													context.getSource().sendFeedback(Component.literal("Unknown feature: " + name));
													return 0;
												}

												feature.toggle();
												context.getSource().sendFeedback(Component.literal(name + " " + (feature.isEnabled() ? "enabled" : "disabled")));
												return 1;
											})
									)
									.then(
										ClientCommands.literal("status")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												ClientFeature feature = featureManager.get(name);
												if (feature == null) {
													context.getSource().sendFeedback(Component.literal("Unknown feature: " + name));
													return 0;
												}

												context.getSource().sendFeedback(Component.literal(name + " is " + (feature.isEnabled() ? "on" : "off")));
												return 1;
											})
									)
							)
					)
			)
		);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.player != null) {
				client.player.sendSystemMessage(Component.literal("Client loaded successfully"));
			}
		});
	}
}