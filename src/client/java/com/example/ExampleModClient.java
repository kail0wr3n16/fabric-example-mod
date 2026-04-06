package com.example;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.lwjgl.glfw.GLFW;

import com.example.module.Module;
import com.example.module.ModuleManager;
import com.example.module.OverlayModule;

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
	private static final Identifier MODULES_HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "modules_hud");
	private final ModuleManager moduleManager = new ModuleManager();

	@Override
	public void onInitializeClient() {
		moduleManager.register(new OverlayModule());

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
				Module overlayModule = moduleManager.get("overlay");
				overlayModule.toggle();

				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal("Overlay " + (overlayModule.isEnabled() ? "enabled" : "disabled")));
				}
			}

			moduleManager.tick(client);
		});

		HudElementRegistry.addLast(MODULES_HUD_ID, moduleManager::renderHud);

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
						ClientCommands.literal("modules")
							.executes(context -> {
								StringBuilder status = new StringBuilder("Modules: ");
								for (Module module : moduleManager.all()) {
									if (status.length() > "Modules: ".length()) {
										status.append(", ");
									}
									status.append(module.getName()).append("=").append(module.isEnabled() ? "on" : "off");
								}
								context.getSource().sendFeedback(Component.literal(status.toString()));
								return 1;
							})
					)
					.then(
						ClientCommands.literal("module")
							.then(
								ClientCommands.argument("name", StringArgumentType.word())
									.then(
										ClientCommands.literal("on")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												Module module = moduleManager.get(name);
												if (module == null) {
													context.getSource().sendFeedback(Component.literal("Unknown module: " + name));
													return 0;
												}

												module.setEnabled(true);
												context.getSource().sendFeedback(Component.literal(name + " enabled"));
												return 1;
											})
									)
									.then(
										ClientCommands.literal("off")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												Module module = moduleManager.get(name);
												if (module == null) {
													context.getSource().sendFeedback(Component.literal("Unknown module: " + name));
													return 0;
												}

												module.setEnabled(false);
												context.getSource().sendFeedback(Component.literal(name + " disabled"));
												return 1;
											})
									)
									.then(
										ClientCommands.literal("toggle")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												Module module = moduleManager.get(name);
												if (module == null) {
													context.getSource().sendFeedback(Component.literal("Unknown module: " + name));
													return 0;
												}

												module.toggle();
												context.getSource().sendFeedback(Component.literal(name + " " + (module.isEnabled() ? "enabled" : "disabled")));
												return 1;
											})
									)
									.then(
										ClientCommands.literal("status")
											.executes(context -> {
												String name = StringArgumentType.getString(context, "name");
												Module module = moduleManager.get(name);
												if (module == null) {
													context.getSource().sendFeedback(Component.literal("Unknown module: " + name));
													return 0;
												}

												context.getSource().sendFeedback(Component.literal(name + " is " + (module.isEnabled() ? "on" : "off")));
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