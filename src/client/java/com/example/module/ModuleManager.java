package com.example.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import com.example.module.setting.Setting;
import com.example.ui.HudManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class ModuleManager {
	private static final String CONFIG_DIR = "clientloaded/configs";
	private static final String CONFIG_EXTENSION = ".properties";
	private static final String DEFAULT_CONFIG_NAME = "default";
	private static final long SAVE_DEBOUNCE_MS = 350L;
	private static final String[] HUD_ELEMENT_IDS = new String[] { "watermark", "fps", "coordinates", "modulelist" };
	private static final Pattern VALID_CONFIG_NAME = Pattern.compile("[A-Za-z0-9_-]+");

	private final Map<String, Module> modules = new LinkedHashMap<>();
	private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(new ConfigSaveThreadFactory());
	private ScheduledFuture<?> pendingSaveTask;
	private HudManager hudManager;
	private boolean applyingPersistedState;
	private String activeConfigName = DEFAULT_CONFIG_NAME;

	public void register(Module module) {
		modules.put(module.getName(), module);
		module.setStateChangeListener(this::onModuleStateChanged);
		for (Setting<?> setting : module.getSettings()) {
			setting.setChangeListener(this::onSettingChanged);
		}
	}

	public void setHudManager(HudManager hudManager) {
		this.hudManager = hudManager;
	}

	public Module get(String name) {
		return modules.get(name);
	}

	public Collection<Module> all() {
		return modules.values();
	}

	public List<Module> getByCategory(ModuleCategory category) {
		List<Module> result = new ArrayList<>();
		for (Module module : modules.values()) {
			if (module.getCategory() == category) {
				result.add(module);
			}
		}
		return result;
	}

	public Map<ModuleCategory, List<Module>> getGroupedByCategory() {
		Map<ModuleCategory, List<Module>> grouped = new EnumMap<>(ModuleCategory.class);
		for (ModuleCategory category : ModuleCategory.values()) {
			grouped.put(category, new ArrayList<>());
		}

		for (Module module : modules.values()) {
			grouped.get(module.getCategory()).add(module);
		}

		return grouped;
	}

	public void registerKeybinds(String modId) {
		for (Module module : modules.values()) {
			KeyMapping keybind = module.createDefaultKeybind(modId);
			if (keybind != null) {
				KeyMapping registeredKeybind = KeyMappingHelper.registerKeyMapping(keybind);

				// Migrate legacy overlay binding that used Right Shift before GUI got it.
				if ("overlay".equals(module.getName())) {
					InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(registeredKeybind);
					if (bound.getType() == InputConstants.Type.KEYSYM && bound.getValue() == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) {
						registeredKeybind.setKey(registeredKeybind.getDefaultKey());
						KeyMapping.resetMapping();
					}
				}

				module.setKeybind(registeredKeybind);
			}
		}
	}

	public void loadEnabledStates(Minecraft client) {
		Path configFile = getConfigFile(client, activeConfigName);
		if (!Files.exists(configFile)) {
			return;
		}

		loadConfigFromFile(configFile);
	}

	public void saveEnabledStates(Minecraft client) {
		synchronized (this) {
			if (pendingSaveTask != null) {
				pendingSaveTask.cancel(false);
				pendingSaveTask = null;
			}
		}
		saveConfigNow(client, activeConfigName);
	}

	public void saveNamedConfig(Minecraft client, String configName) {
		String normalized = normalizeConfigName(configName);
		saveConfigNow(client, normalized);
	}

	public boolean loadNamedConfig(Minecraft client, String configName) {
		String normalized = normalizeConfigName(configName);
		Path configFile = getConfigFile(client, normalized);
		if (!Files.exists(configFile)) {
			return false;
		}

		loadConfigFromFile(configFile);
		activeConfigName = normalized;
		notifyConfigChanged();
		return true;
	}

	public List<String> listConfigNames(Minecraft client) {
		Path configDir = getConfigDirectory(client);
		if (!Files.exists(configDir)) {
			return Collections.emptyList();
		}

		List<String> names = new ArrayList<>();
		try (var stream = Files.list(configDir)) {
			stream
				.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(CONFIG_EXTENSION))
				.forEach(path -> {
					String fileName = path.getFileName().toString();
					names.add(fileName.substring(0, fileName.length() - CONFIG_EXTENSION.length()));
				});
		} catch (IOException e) {
			System.err.println("[clientloaded] Failed to list configs: " + e.getMessage());
			return Collections.emptyList();
		}

		Collections.sort(names);
		return names;
	}

	public String getActiveConfigName() {
		return activeConfigName;
	}

	public void notifyConfigChanged() {
		scheduleConfigSave();
	}

	private void loadConfigFromFile(Path configFile) {
		Properties props = new Properties();
		try (InputStream in = Files.newInputStream(configFile)) {
			props.load(in);
		} catch (IOException e) {
			System.err.println("[clientloaded] Failed to load config: " + e.getMessage());
			return;
		}

		applyingPersistedState = true;
		try {
			for (Module module : modules.values()) {
				String enabledRaw = props.getProperty("module.enabled." + module.getName());
				if (enabledRaw != null) {
					module.setEnabled(Boolean.parseBoolean(enabledRaw));
				}

				for (Setting<?> setting : module.getSettings()) {
					String settingRaw = props.getProperty("setting." + module.getName() + "." + setting.getName());
					if (settingRaw == null) {
						continue;
					}

					if (setting instanceof BooleanSetting booleanSetting) {
						booleanSetting.setValue(Boolean.parseBoolean(settingRaw));
					} else if (setting instanceof NumberSetting numberSetting) {
						try {
							numberSetting.setValue(Double.parseDouble(settingRaw));
						} catch (NumberFormatException ignored) {
							// Ignore invalid stored number values.
						}
					}
				}

				if (module.getKeybind() != null) {
					String keybindRaw = props.getProperty("keybind." + module.getName());
					if (keybindRaw != null) {
						try {
							module.getKeybind().setKey(InputConstants.getKey(keybindRaw));
						} catch (IllegalArgumentException ignored) {
							// Ignore invalid saved key strings.
						}
					}
				}
			}

			if (hudManager != null) {
				for (String elementId : HUD_ELEMENT_IDS) {
					String hudRaw = props.getProperty("hud." + elementId);
					if (hudRaw != null) {
						hudManager.setElementEnabled(elementId, Boolean.parseBoolean(hudRaw));
					}
				}
			}

			KeyMapping.resetMapping();
		} finally {
			applyingPersistedState = false;
		}
	}

	private void saveConfigNow(Minecraft client, String configName) {
		Path configFile = getConfigFile(client, configName);
		Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
		try {
			Files.createDirectories(configFile.getParent());
		} catch (IOException e) {
			System.err.println("[clientloaded] Failed to create config directory: " + e.getMessage());
			return;
		}

		Properties props = new Properties();
		for (Module module : modules.values()) {
			props.setProperty("module.enabled." + module.getName(), Boolean.toString(module.isEnabled()));

			for (Setting<?> setting : module.getSettings()) {
				props.setProperty("setting." + module.getName() + "." + setting.getName(), setting.getValue().toString());
			}

			if (module.getKeybind() != null) {
				props.setProperty("keybind." + module.getName(), module.getKeybind().saveString());
			}
		}

		if (hudManager != null) {
			for (String elementId : HUD_ELEMENT_IDS) {
				props.setProperty("hud." + elementId, Boolean.toString(hudManager.isElementEnabled(elementId)));
			}
		}

		try (OutputStream out = Files.newOutputStream(tempFile)) {
			props.store(out, "Clientloaded config");
			out.flush();
			Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			try {
				Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				// Best-effort fallback already failed.
			}
			System.err.println("[clientloaded] Failed to save config: " + e.getMessage());
		}
	}

	private Path getConfigDirectory(Minecraft client) {
		return client.gameDirectory.toPath().resolve("config").resolve(CONFIG_DIR);
	}

	private Path getConfigFile(Minecraft client, String configName) {
		return getConfigDirectory(client).resolve(configName + CONFIG_EXTENSION);
	}

	private String normalizeConfigName(String configName) {
		if (configName == null) {
			throw new IllegalArgumentException("Config name cannot be null");
		}

		String trimmed = configName.trim();
		if (trimmed.isEmpty() || !VALID_CONFIG_NAME.matcher(trimmed).matches()) {
			throw new IllegalArgumentException("Invalid config name. Use letters, numbers, '_' or '-'.");
		}

		return trimmed.toLowerCase();
	}

	private synchronized void scheduleConfigSave() {
		if (applyingPersistedState) {
			return;
		}

		if (pendingSaveTask != null) {
			pendingSaveTask.cancel(false);
		}

		pendingSaveTask = saveExecutor.schedule(() -> {
			Minecraft client = Minecraft.getInstance();
			if (client != null) {
				saveConfigNow(client, activeConfigName);
			}
		}, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
	}

	private void onModuleStateChanged() {
		scheduleConfigSave();
	}

	private void onSettingChanged() {
		scheduleConfigSave();
	}

	private static final class ConfigSaveThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "clientloaded-config-save");
			thread.setDaemon(true);
			return thread;
		}
	}

	public void tick(Minecraft client) {
		for (Module module : modules.values()) {
			if (module.isEnabled()) {
				module.onTick(client);
			}
		}
	}
}