package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;

public class AdaptiveUiModule extends Module {
	private final BooleanSetting adaptiveMode;
	private final BooleanSetting cleanUiMode;
	private final BooleanSetting showContextNotifications;
	private final NumberSetting combatTimeoutSeconds;
	private final NumberSetting movementSpeedThreshold;
	private final NumberSetting lowHealthThreshold;
	private final BooleanSetting enableCombatContext;
	private final BooleanSetting enableLowHealthContext;
	private final BooleanSetting enableMovingContext;
	private final BooleanSetting enableIdleContext;
	private final NumberSetting combatPriority;
	private final NumberSetting lowHealthPriority;
	private final NumberSetting movingPriority;
	private final NumberSetting idlePriority;
	private final BooleanSetting adaptiveProfiles;
	private final BooleanSetting autoFocusInCombat;

	public AdaptiveUiModule() {
		super("adaptiveui", ModuleCategory.MISC, true);
		this.adaptiveMode = addSetting(new BooleanSetting("adaptiveMode", true));
		this.cleanUiMode = addSetting(new BooleanSetting("cleanUiMode", false));
		this.showContextNotifications = addSetting(new BooleanSetting("showContextNotifications", true));
		this.combatTimeoutSeconds = addSetting(new NumberSetting("combatTimeoutSeconds", 3.0, 1.0, 12.0));
		this.movementSpeedThreshold = addSetting(new NumberSetting("movementSpeedThreshold", 0.08, 0.01, 0.40));
		this.lowHealthThreshold = addSetting(new NumberSetting("lowHealthThreshold", 8.0, 1.0, 20.0));
		this.enableCombatContext = addSetting(new BooleanSetting("enableCombatContext", true));
		this.enableLowHealthContext = addSetting(new BooleanSetting("enableLowHealthContext", true));
		this.enableMovingContext = addSetting(new BooleanSetting("enableMovingContext", true));
		this.enableIdleContext = addSetting(new BooleanSetting("enableIdleContext", true));
		this.combatPriority = addSetting(new NumberSetting("combatPriority", 100.0, 1.0, 200.0));
		this.lowHealthPriority = addSetting(new NumberSetting("lowHealthPriority", 80.0, 1.0, 200.0));
		this.movingPriority = addSetting(new NumberSetting("movingPriority", 60.0, 1.0, 200.0));
		this.idlePriority = addSetting(new NumberSetting("idlePriority", 20.0, 1.0, 200.0));
		this.adaptiveProfiles = addSetting(new BooleanSetting("adaptiveProfiles", true));
		this.autoFocusInCombat = addSetting(new BooleanSetting("autoFocusInCombat", true));
	}

	public boolean isAdaptiveModeEnabled() {
		return adaptiveMode.isEnabled();
	}

	public boolean isCleanUiModeEnabled() {
		return cleanUiMode.isEnabled();
	}

	public boolean shouldShowContextNotifications() {
		return showContextNotifications.isEnabled();
	}

	public double getCombatTimeoutSeconds() {
		return combatTimeoutSeconds.getValue();
	}

	public double getMovementSpeedThreshold() {
		return movementSpeedThreshold.getValue();
	}

	public double getLowHealthThreshold() {
		return lowHealthThreshold.getValue();
	}

	public boolean isContextEnabled(String contextId) {
		return switch (contextId.toLowerCase()) {
			case "combat" -> enableCombatContext.isEnabled();
			case "low_health" -> enableLowHealthContext.isEnabled();
			case "moving" -> enableMovingContext.isEnabled();
			case "idle" -> enableIdleContext.isEnabled();
			default -> true;
		};
	}

	public int getContextPriority(String contextId) {
		return switch (contextId.toLowerCase()) {
			case "combat" -> (int) Math.round(combatPriority.getValue());
			case "low_health" -> (int) Math.round(lowHealthPriority.getValue());
			case "moving" -> (int) Math.round(movingPriority.getValue());
			case "idle" -> (int) Math.round(idlePriority.getValue());
			default -> 0;
		};
	}

	public boolean isAdaptiveProfilesEnabled() {
		return adaptiveProfiles.isEnabled();
	}

	public boolean isAutoFocusInCombatEnabled() {
		return autoFocusInCombat.isEnabled();
	}
}
