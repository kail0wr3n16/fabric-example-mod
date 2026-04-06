package com.example.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.example.module.Module;
import com.example.module.ModuleCategory;
import com.example.module.ModuleManager;
import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import com.example.module.setting.Setting;
import com.example.ui.ClientColors;
import com.example.ui.HudManager;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ClickGuiScreen extends Screen {
	private static final int OUTER_MARGIN = 18;
	private static final int PANEL_GAP = 12;
	private static final int PANEL_GAP_COMPACT = 4;
	private static final int PANEL_MIN_WIDTH = 60;
	private static final int PANEL_HEADER_HEIGHT = 20;
	private static final int PANEL_PADDING = 8;
	private static final int MODULE_ROW_HEIGHT = 14;
	private static final int SETTING_ROW_HEIGHT = 22;
	private static final int SETTING_GAP = 3;

	private final ModuleManager moduleManager;
	private final HudManager hudManager;
	private final Set<String> expandedModules = new HashSet<>();
	private final List<ModuleRowHitbox> moduleHitboxes = new ArrayList<>();
	private final List<BindHitbox> bindHitboxes = new ArrayList<>();
	private final List<HudToggleHitbox> hudToggleHitboxes = new ArrayList<>();
	private final List<BooleanSettingHitbox> booleanSettingHitboxes = new ArrayList<>();
	private final List<NumberAdjustHitbox> numberAdjustHitboxes = new ArrayList<>();
	private final List<NumberValueHitbox> numberValueHitboxes = new ArrayList<>();
	private NumberSetting editingNumberSetting = null;
	private String numberInputBuffer = "";
	private Module listeningBindModule = null;

	public ClickGuiScreen(ModuleManager moduleManager, HudManager hudManager) {
		super(Component.literal("My Client"));
		this.moduleManager = moduleManager;
		this.hudManager = hudManager;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		moduleHitboxes.clear();
		bindHitboxes.clear();
		hudToggleHitboxes.clear();
		booleanSettingHitboxes.clear();
		numberAdjustHitboxes.clear();
		numberValueHitboxes.clear();

		int availableWidth = Math.max(100, this.width - (OUTER_MARGIN * 2));
		int panelCount = ModuleCategory.values().length;

		int panelGap = PANEL_GAP;
		int panelWidth = (availableWidth - (panelGap * (panelCount - 1))) / panelCount;
		if (panelWidth < 120) {
			panelGap = PANEL_GAP_COMPACT;
			panelWidth = (availableWidth - (panelGap * (panelCount - 1))) / panelCount;
		}
		panelWidth = Math.max(PANEL_MIN_WIDTH, panelWidth);

		int totalPanelsWidth = (panelWidth * panelCount) + (panelGap * (panelCount - 1));
		if (totalPanelsWidth > this.width - 4) {
			panelWidth = Math.max(PANEL_MIN_WIDTH, (this.width - 4 - (panelGap * (panelCount - 1))) / panelCount);
			totalPanelsWidth = (panelWidth * panelCount) + (panelGap * (panelCount - 1));
		}

		int panelX = Math.max(2, (this.width - totalPanelsWidth) / 2);
		int panelY = OUTER_MARGIN + 24;

		// Existing dark backdrop style.
		guiGraphics.fill(0, 0, this.width, this.height, 0xAA0B0D12);
		guiGraphics.centeredText(this.font, this.title, this.width / 2, OUTER_MARGIN, 0xFFFFFFFF);

		Map<ModuleCategory, List<Module>> grouped = moduleManager.getGroupedByCategory();
		for (ModuleCategory category : ModuleCategory.values()) {
			List<Module> modules = grouped.get(category);
			int panelHeight = calculatePanelHeight(modules);

			guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0161A22);
			guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEADER_HEIGHT, 0xFF1F2633);
			guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, ClientColors.PRIMARY_TEXT_ARGB);

			Component categoryTitle = Component.literal(category.name());
			guiGraphics.centeredText(this.font, categoryTitle, panelX + (panelWidth / 2), panelY + 6, 0xFFFFFFFF);

			int rowY = panelY + PANEL_HEADER_HEIGHT + PANEL_PADDING;
			for (Module module : modules) {
				rowY = renderModuleRow(guiGraphics, module, panelX, panelWidth, rowY);
			}

			panelX += panelWidth + panelGap;
		}
	}

	private int calculatePanelHeight(List<Module> modules) {
		int height = PANEL_HEADER_HEIGHT + PANEL_PADDING + PANEL_PADDING;
		for (Module module : modules) {
			height += MODULE_ROW_HEIGHT;
			if (expandedModules.contains(module.getName())) {
				height += SETTING_ROW_HEIGHT + SETTING_GAP;
				if ("overlay".equals(module.getName())) {
					height += (SETTING_ROW_HEIGHT + SETTING_GAP) * 4;
				}
				for (Setting<?> setting : module.getSettings()) {
					height += SETTING_ROW_HEIGHT + SETTING_GAP;
				}
			}
		}
		return Math.max(120, height);
	}

	private int renderModuleRow(GuiGraphicsExtractor guiGraphics, Module module, int panelX, int panelWidth, int rowY) {
		int rowLeft = panelX + PANEL_PADDING;
		int rowRight = panelX + panelWidth - PANEL_PADDING;
		int rowBottom = rowY + MODULE_ROW_HEIGHT;

		moduleHitboxes.add(new ModuleRowHitbox(module, rowLeft, rowY, rowRight, rowBottom));

		int bgColor = module.isEnabled() ? 0x2D54C5FF : 0x1AFFFFFF;
		guiGraphics.fill(rowLeft, rowY, rowRight, rowBottom, bgColor);

		String keyLabel = getModuleKeyLabel(module);
		Component label = Component.literal(module.getName() + " [" + keyLabel + "]");
		guiGraphics.text(this.font, label, rowLeft + 4, rowY + 3, 0xFFE6EAF2, false);

		Component state = Component.literal(module.isEnabled() ? "ON" : "OFF");
		int stateWidth = this.font.width(state);
		int stateColor = module.isEnabled() ? 0xFF77E38E : 0xFF9AA3B2;
		guiGraphics.text(this.font, state, rowRight - stateWidth - 4, rowY + 3, stateColor, false);

		int nextY = rowBottom;
		if (expandedModules.contains(module.getName())) {
			nextY = renderBindRow(guiGraphics, module, rowLeft + 6, rowRight - 6, nextY);
			if ("overlay".equals(module.getName())) {
				nextY = renderHudToggleRow(guiGraphics, "watermark", "watermark", rowLeft + 6, rowRight - 6, nextY);
				nextY = renderHudToggleRow(guiGraphics, "fps", "fps", rowLeft + 6, rowRight - 6, nextY);
				nextY = renderHudToggleRow(guiGraphics, "coordinates", "coordinates", rowLeft + 6, rowRight - 6, nextY);
				nextY = renderHudToggleRow(guiGraphics, "modulelist", "moduleList", rowLeft + 6, rowRight - 6, nextY);
			}
			for (Setting<?> setting : module.getSettings()) {
				nextY = renderSettingRow(guiGraphics, setting, rowLeft + 6, rowRight - 6, nextY);
			}
		}

		return nextY;
	}

	private int renderBindRow(GuiGraphicsExtractor guiGraphics, Module module, int left, int right, int rowY) {
		int rowBottom = rowY + SETTING_ROW_HEIGHT;
		guiGraphics.fill(left, rowY, right, rowBottom, 0x22000000);

		guiGraphics.text(this.font, Component.literal("bind"), left + 3, rowY + 2, 0xFFADB5C0, false);

		boolean listening = listeningBindModule == module;
		String bindText = listening ? "PRESS KEY" : getModuleKeyLabel(module);
		Component bindValue = Component.literal(bindText);
		int bindWidth = this.font.width(bindValue);
		int bindColor = listening ? ClientColors.PRIMARY_TEXT_ARGB : 0xFFE6EAF2;
		guiGraphics.text(this.font, bindValue, right - bindWidth - 6, rowY + 2, bindColor, false);

		bindHitboxes.add(new BindHitbox(module, left, rowY, right, rowBottom));
		return rowBottom + SETTING_GAP;
	}

	private int renderHudToggleRow(GuiGraphicsExtractor guiGraphics, String elementId, String label, int left, int right, int rowY) {
		int rowBottom = rowY + SETTING_ROW_HEIGHT;
		guiGraphics.fill(left, rowY, right, rowBottom, 0x22000000);

		guiGraphics.text(this.font, Component.literal(label), left + 3, rowY + 2, 0xFFC8D0DE, false);

		boolean enabled = hudManager.isElementEnabled(elementId);
		Component value = Component.literal(enabled ? "ON" : "OFF");
		int valueWidth = this.font.width(value);
		int valueLeft = right - valueWidth - 6;
		int valueColor = enabled ? 0xFF77E38E : 0xFF9AA3B2;
		guiGraphics.text(this.font, value, valueLeft, rowY + 2, valueColor, false);

		hudToggleHitboxes.add(new HudToggleHitbox(elementId, valueLeft - 3, rowY, right, rowBottom));
		return rowBottom + SETTING_GAP;
	}

	private String getModuleKeyLabel(Module module) {
		KeyMapping keybind = module.getKeybind();
		if (keybind == null || keybind.isUnbound()) {
			return "NONE";
		}
		return keybind.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
	}

	private int renderSettingRow(GuiGraphicsExtractor guiGraphics, Setting<?> setting, int left, int right, int rowY) {
		int rowBottom = rowY + SETTING_ROW_HEIGHT;
		guiGraphics.fill(left, rowY, right, rowBottom, 0x22000000);

		if (setting instanceof BooleanSetting booleanSetting) {
			Component name = Component.literal(setting.getName());
			guiGraphics.text(this.font, name, left + 3, rowY + 2, 0xFFC8D0DE, false);

			Component value = Component.literal(booleanSetting.isEnabled() ? "ON" : "OFF");
			int valueWidth = this.font.width(value);
			int valueLeft = right - valueWidth - 6;
			int valueColor = booleanSetting.isEnabled() ? 0xFF77E38E : 0xFF9AA3B2;
			guiGraphics.text(this.font, value, valueLeft, rowY + 2, valueColor, false);

			booleanSettingHitboxes.add(new BooleanSettingHitbox(booleanSetting, valueLeft - 3, rowY, right, rowBottom));
		} else if (setting instanceof NumberSetting numberSetting) {
			Component name = Component.literal(setting.getName());
			guiGraphics.text(this.font, name, left + 3, rowY + 2, 0xFFADB5C0, false);

			int buttonWidth = 10;
			int valueWidth = 28;
			int gap = 3;
			int controlTop = rowY + 5;
			int controlBottom = controlTop + 10;

			int valueRight = right - 3;
			int valueLeft = valueRight - valueWidth;
			int incRight = valueLeft - gap;
			int incLeft = incRight - buttonWidth;
			int decRight = incLeft - gap;
			int decLeft = decRight - buttonWidth;

			guiGraphics.fill(decLeft, controlTop, decRight, controlBottom, 0x334A5568);
			guiGraphics.fill(incLeft, controlTop, incRight, controlBottom, 0x334A5568);

			int valueBoxColor = (editingNumberSetting == numberSetting) ? 0x553A4A62 : 0x334A5568;
			guiGraphics.fill(valueLeft, controlTop, valueRight, controlBottom, valueBoxColor);

			guiGraphics.text(this.font, Component.literal("-"), decLeft + 3, controlTop + 1, 0xFFE6EAF2, false);
			guiGraphics.text(this.font, Component.literal("+"), incLeft + 3, controlTop + 1, 0xFFE6EAF2, false);

			String shownValue;
			if (editingNumberSetting == numberSetting) {
				shownValue = numberInputBuffer.isEmpty() ? "_" : numberInputBuffer + "_";
			} else {
				shownValue = Long.toString(Math.round(numberSetting.getValue()));
			}
			Component valueText = Component.literal(shownValue);
			int valueTextWidth = this.font.width(valueText);
			int valueTextX = valueLeft + ((valueWidth - valueTextWidth) / 2);
			guiGraphics.text(this.font, valueText, valueTextX, controlTop + 1, 0xFFE6EAF2, false);

			numberAdjustHitboxes.add(new NumberAdjustHitbox(numberSetting, decLeft, controlTop, decRight, controlBottom, -1));
			numberAdjustHitboxes.add(new NumberAdjustHitbox(numberSetting, incLeft, controlTop, incRight, controlBottom, 1));
			numberValueHitboxes.add(new NumberValueHitbox(numberSetting, valueLeft, controlTop, valueRight, controlBottom));
		}

		return rowBottom + SETTING_GAP;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
		double mouseX = event.x();
		double mouseY = event.y();

		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			for (ModuleRowHitbox hitbox : moduleHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					String moduleName = hitbox.module().getName();
					if (expandedModules.contains(moduleName)) {
						expandedModules.remove(moduleName);
					} else {
						expandedModules.add(moduleName);
					}
					return true;
				}
			}
		}

		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			for (BindHitbox hitbox : bindHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					commitNumberInput();
					listeningBindModule = hitbox.module();
					return true;
				}
			}

			for (BooleanSettingHitbox hitbox : booleanSettingHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					commitNumberInput();
					listeningBindModule = null;
					hitbox.setting().setValue(!hitbox.setting().isEnabled());
					return true;
				}
			}

			for (HudToggleHitbox hitbox : hudToggleHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					boolean next = !hudManager.isElementEnabled(hitbox.elementId());
					hudManager.setElementEnabled(hitbox.elementId(), next);
					moduleManager.notifyConfigChanged();
					return true;
				}
			}

			for (NumberAdjustHitbox hitbox : numberAdjustHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					if (editingNumberSetting != hitbox.setting()) {
						commitNumberInput();
					}
					listeningBindModule = null;
					adjustNumberSetting(hitbox.setting(), hitbox.delta());
					return true;
				}
			}

			for (NumberValueHitbox hitbox : numberValueHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					if (editingNumberSetting != hitbox.setting()) {
						commitNumberInput();
						editingNumberSetting = hitbox.setting();
						numberInputBuffer = Long.toString(Math.round(hitbox.setting().getValue()));
					}
					listeningBindModule = null;
					return true;
				}
			}

			for (ModuleRowHitbox hitbox : moduleHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					commitNumberInput();
					listeningBindModule = null;
					hitbox.module().toggle();
					return true;
				}
			}

			commitNumberInput();
			listeningBindModule = null;
		}

		return super.mouseClicked(event, bl);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (editingNumberSetting == null) {
			return super.charTyped(event);
		}

		char codePoint = (char) event.codepoint();

		if (Character.isDigit(codePoint)) {
			numberInputBuffer += codePoint;
			return true;
		}

		if (codePoint == '-' && numberInputBuffer.isEmpty()) {
			numberInputBuffer = "-";
			return true;
		}

		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listeningBindModule != null) {
			int keyCode = event.key();
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				listeningBindModule = null;
				return true;
			}

			KeyMapping moduleKeybind = listeningBindModule.getKeybind();
			if (moduleKeybind == null) {
				listeningBindModule = null;
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				moduleKeybind.setKey(InputConstants.UNKNOWN);
				KeyMapping.resetMapping();
				moduleManager.notifyConfigChanged();
				listeningBindModule = null;
				return true;
			}

			InputConstants.Key pressedKey = InputConstants.getKey(event);
			if (pressedKey.getType() == InputConstants.Type.KEYSYM && pressedKey.getValue() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
				return true;
			}

			moduleKeybind.setKey(pressedKey);
			KeyMapping.resetMapping();
			moduleManager.notifyConfigChanged();
			listeningBindModule = null;
			return true;
		}

		if (editingNumberSetting == null) {
			return super.keyPressed(event);
		}

		int keyCode = event.key();

		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			commitNumberInput();
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			cancelNumberInput();
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if (!numberInputBuffer.isEmpty()) {
				numberInputBuffer = numberInputBuffer.substring(0, numberInputBuffer.length() - 1);
			}
			return true;
		}

		return true;
	}

	private void adjustNumberSetting(NumberSetting setting, int delta) {
		double steppedValue = Math.round(setting.getValue()) + delta;
		steppedValue = Math.max(setting.getMin(), Math.min(setting.getMax(), steppedValue));
		setting.setValue(steppedValue);
	}

	private void commitNumberInput() {
		if (editingNumberSetting == null) {
			return;
		}

		if (!numberInputBuffer.isEmpty() && !"-".equals(numberInputBuffer)) {
			try {
				double parsed = Integer.parseInt(numberInputBuffer);
				parsed = Math.max(editingNumberSetting.getMin(), Math.min(editingNumberSetting.getMax(), parsed));
				editingNumberSetting.setValue(parsed);
			} catch (NumberFormatException ignored) {
				// Ignore invalid input and keep existing value.
			}
		}

		editingNumberSetting = null;
		numberInputBuffer = "";
	}

	private void cancelNumberInput() {
		editingNumberSetting = null;
		numberInputBuffer = "";
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	private record ModuleRowHitbox(Module module, int left, int top, int right, int bottom) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}

	private record BindHitbox(Module module, int left, int top, int right, int bottom) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}

	private record HudToggleHitbox(String elementId, int left, int top, int right, int bottom) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}

	private record BooleanSettingHitbox(BooleanSetting setting, int left, int top, int right, int bottom) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}

	private record NumberAdjustHitbox(NumberSetting setting, int left, int top, int right, int bottom, int delta) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}

	private record NumberValueHitbox(NumberSetting setting, int left, int top, int right, int bottom) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}
}