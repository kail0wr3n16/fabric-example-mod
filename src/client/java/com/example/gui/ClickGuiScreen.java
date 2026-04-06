package com.example.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.example.module.Module;
import com.example.module.ModuleCategory;
import com.example.module.ModuleManager;
import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import com.example.module.setting.Setting;
import com.example.ui.ClientColors;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
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
	private static final int SETTING_ROW_HEIGHT = 12;
	private static final int SETTING_GAP = 3;

	private final ModuleManager moduleManager;
	private final Set<String> expandedModules = new HashSet<>();
	private final List<ModuleRowHitbox> moduleHitboxes = new ArrayList<>();
	private final List<BooleanSettingHitbox> booleanSettingHitboxes = new ArrayList<>();
	private final List<NumberSettingHitbox> numberSettingHitboxes = new ArrayList<>();

	public ClickGuiScreen(ModuleManager moduleManager) {
		super(Component.literal("My Client"));
		this.moduleManager = moduleManager;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
		moduleHitboxes.clear();
		booleanSettingHitboxes.clear();
		numberSettingHitboxes.clear();

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

		Component label = Component.literal(module.getName());
		guiGraphics.text(this.font, label, rowLeft + 4, rowY + 3, 0xFFE6EAF2, false);

		Component state = Component.literal(module.isEnabled() ? "ON" : "OFF");
		int stateWidth = this.font.width(state);
		int stateColor = module.isEnabled() ? 0xFF77E38E : 0xFF9AA3B2;
		guiGraphics.text(this.font, state, rowRight - stateWidth - 4, rowY + 3, stateColor, false);

		int nextY = rowBottom;
		if (expandedModules.contains(module.getName())) {
			for (Setting<?> setting : module.getSettings()) {
				nextY = renderSettingRow(guiGraphics, setting, rowLeft + 6, rowRight - 6, nextY);
			}
		}

		return nextY;
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
			guiGraphics.text(this.font, name, left + 3, rowY + 2, 0xFFC8D0DE, false);

			int sliderLeft = left + 65;
			int sliderRight = right - 42;
			int sliderY = rowY + 5;
			guiGraphics.fill(sliderLeft, sliderY, sliderRight, sliderY + 2, 0x664A5568);

			double progress = (numberSetting.getValue() - numberSetting.getMin()) / (numberSetting.getMax() - numberSetting.getMin());
			progress = Math.max(0.0, Math.min(1.0, progress));
			int fillRight = sliderLeft + (int) Math.round((sliderRight - sliderLeft) * progress);
			guiGraphics.fill(sliderLeft, sliderY, fillRight, sliderY + 2, ClientColors.PRIMARY_TEXT_ARGB);

			Component valueText = Component.literal(String.format(Locale.ROOT, "%.2f", numberSetting.getValue()));
			guiGraphics.text(this.font, valueText, right - 36, rowY + 2, 0xFFE6EAF2, false);

			numberSettingHitboxes.add(new NumberSettingHitbox(numberSetting, sliderLeft, rowY, sliderRight, rowBottom));
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
			for (BooleanSettingHitbox hitbox : booleanSettingHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					hitbox.setting().setValue(!hitbox.setting().isEnabled());
					return true;
				}
			}

			for (NumberSettingHitbox hitbox : numberSettingHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					double sliderWidth = hitbox.right() - hitbox.left();
					double t = (mouseX - hitbox.left()) / sliderWidth;
					t = Math.max(0.0, Math.min(1.0, t));
					double value = hitbox.setting().getMin() + ((hitbox.setting().getMax() - hitbox.setting().getMin()) * t);
					hitbox.setting().setValue(value);
					return true;
				}
			}

			for (ModuleRowHitbox hitbox : moduleHitboxes) {
				if (hitbox.contains(mouseX, mouseY)) {
					hitbox.module().toggle();
					return true;
				}
			}
		}

		return super.mouseClicked(event, bl);
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

	private record BooleanSettingHitbox(BooleanSetting setting, int left, int top, int right, int bottom) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}

	private record NumberSettingHitbox(NumberSetting setting, int left, int top, int right, int bottom) {
		private boolean contains(double x, double y) {
			return x >= left && x <= right && y >= top && y <= bottom;
		}
	}
}