package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.example.input.InputHandler;
import com.example.module.Module;
import com.example.module.FullbrightModule;
import com.example.module.ModuleManager;
import com.example.module.OverlayModule;
import com.example.module.SprintModule;
import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import com.example.module.setting.Setting;
import com.example.ui.HudManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ExampleModClient implements ClientModInitializer {
	private static final String MOD_ID = "clientloaded";
	private static final Identifier MODULES_HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "modules_hud");
	private final ModuleManager moduleManager = new ModuleManager();
	private final HudManager hudManager = new HudManager(moduleManager);

	@Override
	public void onInitializeClient() {
		moduleManager.setHudManager(hudManager);
		moduleManager.register(new OverlayModule());
		moduleManager.register(new SprintModule());
		moduleManager.register(new FullbrightModule());
		moduleManager.registerKeybinds(MOD_ID);
		InputHandler inputHandler = new InputHandler(moduleManager, hudManager);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			inputHandler.handleInput(client);
			moduleManager.tick(client);
		});

		HudElementRegistry.addLast(MODULES_HUD_ID, hudManager::renderHud);

		ClientLifecycleEvents.CLIENT_STARTED.register(moduleManager::loadEnabledStates);
		ClientLifecycleEvents.CLIENT_STOPPING.register(moduleManager::saveEnabledStates);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(
				ClientCommands.literal("clientloaded")
					.executes(context -> {
						context.getSource().sendFeedback(Component.literal("Usage: /clientloaded ping|modules|module ..."));
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
						ClientCommands.literal("config")
							.then(
								ClientCommands.literal("save")
									.then(
										ClientCommands.argument("name", StringArgumentType.word())
											.executes(context -> {
												String configName = StringArgumentType.getString(context, "name");
												Minecraft client = Minecraft.getInstance();
												if (client == null) {
													context.getSource().sendFeedback(Component.literal("Client not ready"));
													return 0;
												}

												try {
													moduleManager.saveNamedConfig(client, configName);
													context.getSource().sendFeedback(Component.literal("Saved config: " + configName));
													return 1;
												} catch (IllegalArgumentException e) {
													context.getSource().sendFeedback(Component.literal(e.getMessage()));
													return 0;
												}
											})
									)
							)
							.then(
								ClientCommands.literal("load")
									.then(
										ClientCommands.argument("name", StringArgumentType.word())
											.suggests(this::suggestConfigNames)
											.executes(context -> {
												String configName = StringArgumentType.getString(context, "name");
												Minecraft client = Minecraft.getInstance();
												if (client == null) {
													context.getSource().sendFeedback(Component.literal("Client not ready"));
													return 0;
												}

												try {
													boolean loaded = moduleManager.loadNamedConfig(client, configName);
													if (!loaded) {
														context.getSource().sendFeedback(Component.literal("Config not found: " + configName));
														return 0;
													}
													context.getSource().sendFeedback(Component.literal("Loaded config: " + configName));
													return 1;
												} catch (IllegalArgumentException e) {
													context.getSource().sendFeedback(Component.literal(e.getMessage()));
													return 0;
												}
											})
									)
							)
							.then(
								ClientCommands.literal("list")
									.executes(context -> {
										Minecraft client = Minecraft.getInstance();
										if (client == null) {
											context.getSource().sendFeedback(Component.literal("Client not ready"));
											return 0;
										}

										List<String> configs = moduleManager.listConfigNames(client);
										if (configs.isEmpty()) {
											context.getSource().sendFeedback(Component.literal("No configs found"));
											return 1;
										}

										context.getSource().sendFeedback(Component.literal("Configs: " + String.join(", ", configs)));
										return 1;
									})
							)
					)
					.then(
						ClientCommands.literal("module")
							.then(
								ClientCommands.argument("name", StringArgumentType.word())
									.suggests(this::suggestModuleNames)
									.then(
										ClientCommands.literal("setting")
											.then(
												ClientCommands.argument("setting", StringArgumentType.word())
													.suggests(this::suggestModuleSettingNames)
													.then(
														ClientCommands.argument("value", StringArgumentType.word())
															.suggests(this::suggestSettingValues)
															.executes(context -> {
																String moduleName = StringArgumentType.getString(context, "name");
																String settingName = StringArgumentType.getString(context, "setting");
																String value = StringArgumentType.getString(context, "value");

																Module module = moduleManager.get(moduleName);
																if (module == null) {
																	context.getSource().sendFeedback(Component.literal("Unknown module: " + moduleName));
																	return 0;
																}

																Setting<?> setting = module.getSetting(settingName);
																if (setting == null) {
																	context.getSource().sendFeedback(Component.literal("Unknown setting: " + settingName));
																	return 0;
																}

																boolean applied = applySettingValue(setting, value);
																if (!applied) {
																	context.getSource().sendFeedback(Component.literal("Invalid value for setting " + settingName + ": " + value));
																	return 0;
																}

																context.getSource().sendFeedback(Component.literal("Set " + moduleName + "." + settingName + " = " + setting.getValue()));
																return 1;
															})
													)
											)
									)
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

	private static boolean applySettingValue(Setting<?> setting, String rawValue) {
		if (setting instanceof BooleanSetting booleanSetting) {
			if ("toggle".equalsIgnoreCase(rawValue)) {
				booleanSetting.setValue(!booleanSetting.isEnabled());
				return true;
			}

			if ("on".equalsIgnoreCase(rawValue) || "true".equalsIgnoreCase(rawValue)) {
				booleanSetting.setValue(true);
				return true;
			}

			if ("off".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
				booleanSetting.setValue(false);
				return true;
			}

			return false;
		}

		if (setting instanceof NumberSetting numberSetting) {
			try {
				numberSetting.setValue(Double.parseDouble(rawValue));
				return true;
			} catch (NumberFormatException ignored) {
				return false;
			}
		}

		return false;
	}

	private CompletableFuture<Suggestions> suggestModuleNames(CommandContext<?> context, SuggestionsBuilder builder) {
		List<String> names = new ArrayList<>();
		for (Module module : moduleManager.all()) {
			names.add(module.getName());
		}
		return SharedSuggestionProvider.suggest(names, builder);
	}

	private CompletableFuture<Suggestions> suggestModuleSettingNames(CommandContext<?> context, SuggestionsBuilder builder) {
		String moduleName = StringArgumentType.getString(context, "name");
		Module module = moduleManager.get(moduleName);
		if (module == null) {
			return Suggestions.empty();
		}

		List<String> settingNames = new ArrayList<>();
		for (Setting<?> setting : module.getSettings()) {
			settingNames.add(setting.getName());
		}

		return SharedSuggestionProvider.suggest(settingNames, builder);
	}

	private CompletableFuture<Suggestions> suggestSettingValues(CommandContext<?> context, SuggestionsBuilder builder) {
		String moduleName = StringArgumentType.getString(context, "name");
		String settingName = StringArgumentType.getString(context, "setting");

		Module module = moduleManager.get(moduleName);
		if (module == null) {
			return Suggestions.empty();
		}

		Setting<?> setting = module.getSetting(settingName);
		if (setting instanceof BooleanSetting) {
			return SharedSuggestionProvider.suggest(List.of("on", "off", "toggle", "true", "false"), builder);
		}

		if (setting instanceof NumberSetting) {
			return SharedSuggestionProvider.suggest(List.of("0", "1", "2", "3"), builder);
		}

		return Suggestions.empty();
	}

	private CompletableFuture<Suggestions> suggestConfigNames(CommandContext<?> context, SuggestionsBuilder builder) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return Suggestions.empty();
		}

		return SharedSuggestionProvider.suggest(moduleManager.listConfigNames(client), builder);
	}
}