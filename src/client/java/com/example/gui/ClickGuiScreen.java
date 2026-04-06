package com.example.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
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
import com.example.module.setting.ColorSetting;
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
	private static final int PANEL_MIN_CONTENT_HEIGHT = 70;
	private static final double SCROLL_STEP = 12.0;
	private static final long PANEL_FADE_MS = 180L;
	private static final long CLICK_FEEDBACK_MS = 140L;

	private final ModuleManager moduleManager;
	private final HudManager hudManager;
	private final Map<ModuleCategory, Double> panelScrollOffsets = new EnumMap<>(ModuleCategory.class);
	private final Map<String, Float> moduleTransition = new HashMap<>();
	private final Map<String, Long> moduleClickTimes = new HashMap<>();
	private final Set<String> expandedModules = new HashSet<>();
	private final List<PanelScrollHitbox> panelScrollHitboxes = new ArrayList<>();
	private final List<ModuleRowHitbox> moduleHitboxes = new ArrayList<>();
	private final List<BindHitbox> bindHitboxes = new ArrayList<>();
	private final List<HudToggleHitbox> hudToggleHitboxes = new ArrayList<>();
	private final List<ColorAdjustHitbox> colorAdjustHitboxes = new ArrayList<>();
	private final List<BooleanSettingHitbox> booleanSettingHitboxes = new ArrayList<>();
	private final List<NumberAdjustHitbox> numberAdjustHitboxes = new ArrayList<>();
	private final List<NumberValueHitbox> numberValueHitboxes = new ArrayList<>();
	private NumberSetting editingNumberSetting;
	private String numberInputBuffer = "";
	private Module listeningBindModule;
	private final long openTimeMs;

	public ClickGuiScreen(ModuleManager moduleManager, HudManager hudManager) {
		super(Component.literal("My Client"));
		this.moduleManager = moduleManager;
		this.hudManager = hudManager;
		this.openTimeMs = System.currentTimeMillis();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		panelScrollHitboxes.clear();
		moduleHitboxes.clear();
		bindHitboxes.clear();
		hudToggleHitboxes.clear();
		colorAdjustHitboxes.clear();
		booleanSettingHitboxes.clear();
		numberAdjustHitboxes.clear();
		numberValueHitboxes.clear();

		long nowMs = System.currentTimeMillis();
		float fadeT = Math.min(1.0f, (nowMs - openTimeMs) / (float) PANEL_FADE_MS);
		boolean cleanUiMode = isCleanUiMode();

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

		guiGraphics.fill(0, 0, this.width, this.height, withAlpha(cleanUiMode ? 0x0A0C10 : 0x0B0D12, (int) Math.round(170 * fadeT)));
		guiGraphics.centeredText(this.font, this.title, this.width / 2, OUTER_MARGIN, 0xFFFFFFFF);

		Map<ModuleCategory, List<Module>> grouped = moduleManager.getGroupedByCategory();
		int maxPanelHeight = Math.max(120, this.height - panelY - OUTER_MARGIN);
		for (ModuleCategory category : ModuleCategory.values()) {
			List<Module> modules = grouped.get(category);
			int contentHeight = calculatePanelContentHeight(modules);
			int panelHeight = Math.min(maxPanelHeight, PANEL_HEADER_HEIGHT + (PANEL_PADDING * 2) + contentHeight);
			int contentTop = panelY + PANEL_HEADER_HEIGHT + PANEL_PADDING;
			int contentBottom = panelY + panelHeight - PANEL_PADDING;

			double maxScroll = Math.max(0.0, contentHeight - (contentBottom - contentTop));
			double scroll = clampScroll(panelScrollOffsets.getOrDefault(category, 0.0), maxScroll);
			panelScrollOffsets.put(category, scroll);
			panelScrollHitboxes.add(new PanelScrollHitbox(category, panelX, contentTop, panelX + panelWidth, contentBottom, maxScroll));

			guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
				withAlpha(cleanUiMode ? 0x141821 : 0x161A22, (int) Math.round(224 * fadeT)));
			guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEADER_HEIGHT,
				withAlpha(cleanUiMode ? 0x1A2230 : 0x1F2633, (int) Math.round(255 * fadeT)));
			guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, ClientColors.PRIMARY_TEXT_ARGB);

			guiGraphics.centeredText(this.font, Component.literal(category.name()), panelX + (panelWidth / 2), panelY + 6, 0xFFFFFFFF);

			int rowY = contentTop - (int) Math.round(scroll);
			guiGraphics.enableScissor(panelX + 1, contentTop, panelX + panelWidth - 1, contentBottom);
			for (Module module : modules) {
				rowY = renderModuleRow(guiGraphics, module, panelX, panelWidth, rowY, contentTop, contentBottom, mouseX, mouseY, nowMs);
			}
			guiGraphics.disableScissor();

			panelX += panelWidth + panelGap;
		}
	}

	private int calculatePanelContentHeight(List<Module> modules) {
		int height = 0;
		for (Module module : modules) {
			height += MODULE_ROW_HEIGHT;
			if (expandedModules.contains(module.getName())) {
				height += SETTING_ROW_HEIGHT + SETTING_GAP;
				if ("adaptiveui".equals(module.getName())) {
					height += SETTING_ROW_HEIGHT + SETTING_GAP;
				}
				if ("overlay".equals(module.getName())) {
					height += (SETTING_ROW_HEIGHT + SETTING_GAP) * 5;
				}
				for (Setting<?> setting : module.getSettings()) {
					height += SETTING_ROW_HEIGHT + SETTING_GAP;
				}
			}
		}
		return Math.max(PANEL_MIN_CONTENT_HEIGHT, height);
	}

	private int renderModuleRow(GuiGraphicsExtractor guiGraphics, Module module, int panelX, int panelWidth, int rowY, int clipTop, int clipBottom,
			int mouseX, int mouseY, long nowMs) {
		boolean cleanUiMode = isCleanUiMode();
		int rowLeft = panelX + PANEL_PADDING;
		int rowRight = panelX + panelWidth - PANEL_PADDING;
		int rowBottom = rowY + MODULE_ROW_HEIGHT;

		if (intersectsClip(rowY, rowBottom, clipTop, clipBottom)) {
			ModuleRowHitbox rowHitbox = new ModuleRowHitbox(module, rowLeft, rowY, rowRight, rowBottom);
			moduleHitboxes.add(rowHitbox);

			float currentAnim = moduleTransition.getOrDefault(module.getName(), module.isEnabled() ? 1.0f : 0.0f);
			currentAnim += ((module.isEnabled() ? 1.0f : 0.0f) - currentAnim) * 0.25f;
			moduleTransition.put(module.getName(), currentAnim);

			boolean hovered = rowHitbox.contains(mouseX, mouseY);
			boolean clicked = (nowMs - moduleClickTimes.getOrDefault(module.getName(), 0L)) < CLICK_FEEDBACK_MS;

			int bgColor = blendArgb(0x1AFFFFFF, withAlpha(ClientColors.PRIMARY_RGB, 0x52), currentAnim);
			if (hovered) {
				bgColor = blendArgb(bgColor, 0x2AFFFFFF, 0.55f);
			}
			if (clicked) {
				bgColor = blendArgb(bgColor, withAlpha(ClientColors.PRIMARY_RGB, 0x66), 0.65f);
			}
			guiGraphics.fill(rowLeft, rowY, rowRight, rowBottom, bgColor);

			String keyLabel = getModuleKeyLabel(module);
			String moduleLabel = cleanUiMode ? module.getName() : module.getName() + " [" + keyLabel + "]";
			guiGraphics.text(this.font, Component.literal(moduleLabel), rowLeft + 4, rowY + 3, 0xFFF0F3F8, false);

			if (cleanUiMode) {
				int indicatorColor = module.isEnabled() ? ClientColors.PRIMARY_TEXT_ARGB : 0xFF4C586C;
				guiGraphics.fill(rowRight - 6, rowY + 4, rowRight - 3, rowY + 10, indicatorColor);
			} else {
				Component state = Component.literal(module.isEnabled() ? "ON" : "OFF");
				int stateWidth = this.font.width(state);
				int stateColor = module.isEnabled() ? ClientColors.PRIMARY_TEXT_ARGB : 0xFF9AA3B2;
				guiGraphics.text(this.font, state, rowRight - stateWidth - 4, rowY + 3, stateColor, false);
			}
		}

		int nextY = rowBottom;
		if (expandedModules.contains(module.getName())) {
			nextY = renderBindRow(guiGraphics, module, rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
			if ("adaptiveui".equals(module.getName())) {
				nextY = renderSectionRow(guiGraphics, "Context Settings", rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
			}
			if ("overlay".equals(module.getName())) {
				nextY = renderHudToggleRow(guiGraphics, "watermark", "watermark", rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
				nextY = renderHudToggleRow(guiGraphics, "fps", "fps", rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
				nextY = renderHudToggleRow(guiGraphics, "coordinates", "coordinates", rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
				nextY = renderHudToggleRow(guiGraphics, "direction", "direction", rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
				nextY = renderHudToggleRow(guiGraphics, "modulelist", "moduleList", rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
			}
			for (Setting<?> setting : module.getSettings()) {
				nextY = renderSettingRow(guiGraphics, setting, rowLeft + 6, rowRight - 6, nextY, clipTop, clipBottom);
			}
		}

		return nextY;
	}

	private int renderBindRow(GuiGraphicsExtractor guiGraphics, Module module, int left, int right, int rowY, int clipTop, int clipBottom) {
		int rowBottom = rowY + SETTING_ROW_HEIGHT;
		if (intersectsClip(rowY, rowBottom, clipTop, clipBottom)) {
			guiGraphics.fill(left, rowY, right, rowBottom, 0x22000000);
			guiGraphics.text(this.font, Component.literal(isCleanUiMode() ? "key" : "bind"), left + 3, rowY + 2, 0xFFADB5C0, false);

			boolean listening = listeningBindModule == module;
			String bindText = listening ? "PRESS KEY" : getModuleKeyLabel(module);
			Component bindValue = Component.literal(bindText);
			int bindWidth = this.font.width(bindValue);
			int bindColor = listening ? ClientColors.PRIMARY_TEXT_ARGB : 0xFFE6EAF2;
			guiGraphics.text(this.font, bindValue, right - bindWidth - 6, rowY + 2, bindColor, false);
			bindHitboxes.add(new BindHitbox(module, left, rowY, right, rowBottom));
		}
		return rowBottom + SETTING_GAP;
	}

	private int renderSectionRow(GuiGraphicsExtractor guiGraphics, String label, int left, int right, int rowY, int clipTop, int clipBottom) {
		int rowBottom = rowY + SETTING_ROW_HEIGHT;
		if (intersectsClip(rowY, rowBottom, clipTop, clipBottom)) {
			guiGraphics.fill(left, rowY, right, rowBottom, 0x1E000000);
			guiGraphics.text(this.font, Component.literal(label), left + 3, rowY + 2, 0xFFBFD2EA, false);
		}
		return rowBottom + SETTING_GAP;
	}

	private int renderHudToggleRow(GuiGraphicsExtractor guiGraphics, String elementId, String label, int left, int right, int rowY, int clipTop, int clipBottom) {
		int rowBottom = rowY + SETTING_ROW_HEIGHT;
		if (intersectsClip(rowY, rowBottom, clipTop, clipBottom)) {
			guiGraphics.fill(left, rowY, right, rowBottom, 0x22000000);
			guiGraphics.text(this.font, Component.literal(label), left + 3, rowY + 2, 0xFFC8D0DE, false);

			boolean enabled = hudManager.isElementEnabled(elementId);
			Component value = Component.literal(enabled ? "ON" : "OFF");
			int valueWidth = this.font.width(value);
			int valueLeft = right - valueWidth - 6;
			int valueColor = enabled ? ClientColors.PRIMARY_TEXT_ARGB : 0xFF9AA3B2;
			guiGraphics.text(this.font, value, valueLeft, rowY + 2, valueColor, false);
			hudToggleHitboxes.add(new HudToggleHitbox(elementId, valueLeft - 3, rowY, right, rowBottom));
		}
		return rowBottom + SETTING_GAP;
	}

	private int renderSettingRow(GuiGraphicsExtractor guiGraphics, Setting<?> setting, int left, int right, int rowY, int clipTop, int clipBottom) {
		int rowBottom = rowY + SETTING_ROW_HEIGHT;
		if (!intersectsClip(rowY, rowBottom, clipTop, clipBottom)) {
			return rowBottom + SETTING_GAP;
		}

		guiGraphics.fill(left, rowY, right, rowBottom, 0x22000000);
		if (setting instanceof BooleanSetting booleanSetting) {
			guiGraphics.text(this.font, Component.literal(setting.getName()), left + 3, rowY + 2, 0xFFC8D0DE, false);

			int switchWidth = 24;
			int switchHeight = 10;
			int switchLeft = right - switchWidth - 5;
			int switchTop = rowY + 6;
			int switchRight = switchLeft + switchWidth;
			int switchBottom = switchTop + switchHeight;
			guiGraphics.fill(switchLeft, switchTop, switchRight, switchBottom,
				booleanSetting.isEnabled() ? withAlpha(ClientColors.PRIMARY_RGB, 0xAA) : 0x663A4252);
			int knobSize = 8;
			int knobLeft = booleanSetting.isEnabled() ? switchRight - knobSize - 1 : switchLeft + 1;
			guiGraphics.fill(knobLeft, switchTop + 1, knobLeft + knobSize, switchTop + 1 + knobSize, 0xFFE6EAF2);
			booleanSettingHitboxes.add(new BooleanSettingHitbox(booleanSetting, switchLeft - 3, rowY, right, rowBottom));
		} else if (setting instanceof ColorSetting colorSetting) {
			guiGraphics.text(this.font, Component.literal(setting.getName()), left + 3, rowY + 2, 0xFFADB5C0, false);

			int valueWidth = 58;
			int controlTop = rowY + 5;
			int controlBottom = controlTop + 10;
			int valueRight = right - 15;
			int valueLeft = valueRight - valueWidth;
			int decLeft = valueLeft - 12;
			int decRight = valueLeft - 3;
			int incLeft = valueRight + 3;
			int incRight = valueRight + 12;

			guiGraphics.fill(decLeft, controlTop, decRight, controlBottom, 0x334A5568);
			guiGraphics.fill(incLeft, controlTop, incRight, controlBottom, 0x334A5568);
			guiGraphics.fill(valueLeft, controlTop, valueRight, controlBottom, 0x334A5568);
			guiGraphics.fill(valueLeft + 1, controlTop + 1, valueLeft + 11, controlBottom - 1, withAlpha(colorSetting.getRgb(), 0xFF));

			guiGraphics.text(this.font, Component.literal(String.format(Locale.ROOT, "%06X", colorSetting.getRgb())), valueLeft + 14, controlTop + 1,
				0xFFE6EAF2, false);
			guiGraphics.text(this.font, Component.literal("<"), decLeft + 2, controlTop + 1, 0xFFE6EAF2, false);
			guiGraphics.text(this.font, Component.literal(">"), incLeft + 2, controlTop + 1, 0xFFE6EAF2, false);

			colorAdjustHitboxes.add(new ColorAdjustHitbox(colorSetting, decLeft, controlTop, decRight, controlBottom, -12));
			colorAdjustHitboxes.add(new ColorAdjustHitbox(colorSetting, incLeft, controlTop, incRight, controlBottom, 12));
		} else if (setting instanceof NumberSetting numberSetting) {
			guiGraphics.text(this.font, Component.literal(setting.getName()), left + 3, rowY + 2, 0xFFADB5C0, false);

			int buttonWidth = 10;
			int valueWidth = 46;
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
			guiGraphics.fill(valueLeft, controlTop, valueRight, controlBottom, (editingNumberSetting == numberSetting) ? 0x553A4A62 : 0x334A5568);

			guiGraphics.text(this.font, Component.literal("-"), decLeft + 3, controlTop + 1, 0xFFE6EAF2, false);
			guiGraphics.text(this.font, Component.literal("+"), incLeft + 3, controlTop + 1, 0xFFE6EAF2, false);

			String shownValue = editingNumberSetting == numberSetting ? (numberInputBuffer.isEmpty() ? "_" : numberInputBuffer + "_")
				: formatNumberForDisplay(numberSetting, numberSetting.getValue());
			Component valueText = Component.literal(shownValue);
			int valueTextX = valueLeft + ((valueWidth - this.font.width(valueText)) / 2);
			guiGraphics.text(this.font, valueText, valueTextX, controlTop + 1, 0xFFE6EAF2, false);

			numberAdjustHitboxes.add(new NumberAdjustHitbox(numberSetting, decLeft, controlTop, decRight, controlBottom, -1));
			numberAdjustHitboxes.add(new NumberAdjustHitbox(numberSetting, incLeft, controlTop, incRight, controlBottom, 1));
			numberValueHitboxes.add(new NumberValueHitbox(numberSetting, valueLeft, controlTop, valueRight, controlBottom));
		}

		return rowBottom + SETTING_GAP;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		for (PanelScrollHitbox panelHitbox : panelScrollHitboxes) {
			if (!panelHitbox.contains(mouseX, mouseY)) {
				continue;
			}
			double current = panelScrollOffsets.getOrDefault(panelHitbox.category(), 0.0);
			double next = clampScroll(current - (scrollY * SCROLL_STEP), panelHitbox.maxScroll());
			panelScrollOffsets.put(panelHitbox.category(), next);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

			for (ColorAdjustHitbox hitbox : colorAdjustHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					hitbox.setting().setRgb(rotateHue(hitbox.setting().getRgb(), hitbox.deltaHue()));
					return true;
				}
			}

			for (HudToggleHitbox hitbox : hudToggleHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					hudManager.setElementEnabled(hitbox.elementId(), !hudManager.isElementEnabled(hitbox.elementId()));
					moduleManager.notifyConfigChanged();
					return true;
				}
			}

			for (NumberAdjustHitbox hitbox : numberAdjustHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					if (editingNumberSetting != hitbox.setting()) {
						commitNumberInput();
					}
					adjustNumberSetting(hitbox.setting(), hitbox.delta());
					return true;
				}
			}

			for (NumberValueHitbox hitbox : numberValueHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					if (editingNumberSetting != hitbox.setting()) {
						commitNumberInput();
						editingNumberSetting = hitbox.setting();
						numberInputBuffer = formatNumberForInput(hitbox.setting(), hitbox.setting().getValue());
					}
					return true;
				}
			}

			for (ModuleRowHitbox hitbox : moduleHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					commitNumberInput();
					listeningBindModule = null;
					hitbox.module().toggle();
					moduleClickTimes.put(hitbox.module().getName(), System.currentTimeMillis());
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
		if (codePoint == '.' && isDecimalSetting(editingNumberSetting) && !numberInputBuffer.contains(".")) {
			if (numberInputBuffer.isEmpty() || "-".equals(numberInputBuffer)) {
				numberInputBuffer += "0";
			}
			numberInputBuffer += ".";
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
		double stepSize = isDecimalSetting(setting) ? 0.01 : 1.0;
		double steppedValue = setting.getValue() + (delta * stepSize);
		if (!isDecimalSetting(setting)) {
			steppedValue = Math.round(steppedValue);
		}
		steppedValue = Math.max(setting.getMin(), Math.min(setting.getMax(), steppedValue));
		setting.setValue(steppedValue);
	}

	private void commitNumberInput() {
		if (editingNumberSetting == null) {
			return;
		}
		if (!numberInputBuffer.isEmpty() && !"-".equals(numberInputBuffer)) {
			try {
				double parsed = Double.parseDouble(numberInputBuffer);
				if (!isDecimalSetting(editingNumberSetting)) {
					parsed = Math.round(parsed);
				}
				parsed = Math.max(editingNumberSetting.getMin(), Math.min(editingNumberSetting.getMax(), parsed));
				editingNumberSetting.setValue(parsed);
			} catch (NumberFormatException ignored) {
			}
		}
		editingNumberSetting = null;
		numberInputBuffer = "";
	}

	private void cancelNumberInput() {
		editingNumberSetting = null;
		numberInputBuffer = "";
	}

	private String getModuleKeyLabel(Module module) {
		KeyMapping keybind = module.getKeybind();
		if (keybind == null || keybind.isUnbound()) {
			return "NONE";
		}
		return keybind.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
	}

	private double clampScroll(double value, double maxScroll) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > maxScroll) {
			return maxScroll;
		}
		return value;
	}

	private boolean intersectsClip(int top, int bottom, int clipTop, int clipBottom) {
		return bottom > clipTop && top < clipBottom;
	}

	private int withAlpha(int rgb, int alpha) {
		return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
	}

	private int blendArgb(int from, int to, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t));
		int r = (int) (((from >>> 16) & 0xFF) + ((((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t));
		int g = (int) (((from >>> 8) & 0xFF) + ((((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t));
		int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * t));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private int rotateHue(int rgb, int deltaHue) {
		float[] hsv = java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
		float hueDegrees = (hsv[0] * 360.0f) + deltaHue;
		while (hueDegrees < 0.0f) {
			hueDegrees += 360.0f;
		}
		while (hueDegrees >= 360.0f) {
			hueDegrees -= 360.0f;
		}
		int packed = java.awt.Color.HSBtoRGB(hueDegrees / 360.0f, Math.max(0.6f, hsv[1]), Math.max(0.75f, hsv[2]));
		return packed & 0xFFFFFF;
	}

	private boolean isDecimalSetting(NumberSetting setting) {
		if (setting == null) {
			return false;
		}
		return hasFractionalPart(setting.getMin()) || hasFractionalPart(setting.getMax()) || hasFractionalPart(setting.getValue());
	}

	private boolean hasFractionalPart(double value) {
		return Math.abs(value - Math.rint(value)) > 0.000001;
	}

	private String formatNumberForDisplay(NumberSetting setting, double value) {
		if (!isDecimalSetting(setting)) {
			return Long.toString(Math.round(value));
		}
		String text = String.format(Locale.ROOT, "%.2f", value);
		if (text.contains(".")) {
			while (text.endsWith("0")) {
				text = text.substring(0, text.length() - 1);
			}
			if (text.endsWith(".")) {
				text = text.substring(0, text.length() - 1);
			}
		}
		return text;
	}

	private String formatNumberForInput(NumberSetting setting, double value) {
		return formatNumberForDisplay(setting, value);
	}

	private boolean isCleanUiMode() {
		return true;
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

	private record ColorAdjustHitbox(ColorSetting setting, int left, int top, int right, int bottom, int deltaHue) {
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

	private record PanelScrollHitbox(ModuleCategory category, int left, int top, int right, int bottom, double maxScroll) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}
}
