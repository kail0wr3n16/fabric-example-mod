package com.example.context;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.module.AdaptiveUiModule;
import com.example.module.Module;
import com.example.module.ModuleManager;
import com.example.module.OverlayModule;
import com.example.ui.ClientColors;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;

public class ContextManager {
	private static final long DEFAULT_COMBAT_TIMEOUT_MS = 3000L;
	private static final long CONTEXT_NOTIFICATION_COOLDOWN_MS = 1200L;
	private static final long CONTEXT_LABEL_DURATION_MS = 1400L;
	private static final float COLOR_BLEND_RATE = 0.18f;
	private static final EnumMap<PlayerContext, Integer> CONTEXT_COLORS = new EnumMap<>(PlayerContext.class);
	private static ContextManager INSTANCE;

	static {
		CONTEXT_COLORS.put(PlayerContext.COMBAT, 0xFF7A4A);
		CONTEXT_COLORS.put(PlayerContext.MOVING, 0x59D8FF);
		CONTEXT_COLORS.put(PlayerContext.IDLE, 0x8FA3B8);
		CONTEXT_COLORS.put(PlayerContext.LOW_HEALTH, 0xFF8A5C);
	}

	private final ModuleManager moduleManager;
	private final List<ContextChangeListener> listeners = new CopyOnWriteArrayList<>();
	private PlayerContext currentContext = PlayerContext.IDLE;
	private long lastCombatAtMs;
	private long lastContextNotificationAtMs;
	private long contextLabelStartedAtMs;
	private String activeContextLabel = "";
	private int activeThemeRgb = ClientColors.DEFAULT_PRIMARY_RGB;

	public ContextManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
		INSTANCE = this;
	}

	public static ContextManager getInstance() {
		return INSTANCE;
	}

	public ModuleManager getModuleManager() {
		return moduleManager;
	}

	public PlayerContext getCurrentContext() {
		return currentContext;
	}

	public String getActiveContextLabel() {
		long elapsed = System.currentTimeMillis() - contextLabelStartedAtMs;
		if (elapsed >= CONTEXT_LABEL_DURATION_MS || activeContextLabel.isEmpty()) {
			return "";
		}
		return activeContextLabel;
	}

	public float getActiveContextLabelAlpha() {
		long elapsed = System.currentTimeMillis() - contextLabelStartedAtMs;
		if (elapsed >= CONTEXT_LABEL_DURATION_MS || activeContextLabel.isEmpty()) {
			return 0.0f;
		}

		float progress = elapsed / (float) CONTEXT_LABEL_DURATION_MS;
		if (progress < 0.2f) {
			return progress / 0.2f;
		}
		if (progress > 0.75f) {
			return Math.max(0.0f, (1.0f - progress) / 0.25f);
		}
		return 1.0f;
	}

	public void addListener(ContextChangeListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ContextChangeListener listener) {
		listeners.remove(listener);
	}

	public void tick(Minecraft client) {
		AdaptiveUiModule adaptiveUi = getAdaptiveModule();
		if (adaptiveUi == null || !adaptiveUi.isAdaptiveModeEnabled()) {
			activeThemeRgb = lerpRgb(activeThemeRgb, getBaseColor(), COLOR_BLEND_RATE);
			ClientColors.setPrimaryColorRgb(activeThemeRgb);
			return;
		}

		if (client == null || client.player == null) {
			setContext(PlayerContext.IDLE);
			applyContextTheme(PlayerContext.IDLE);
			return;
		}

		long combatTimeoutMs = adaptiveUi == null ? DEFAULT_COMBAT_TIMEOUT_MS
			: (long) Math.max(500L, Math.round(adaptiveUi.getCombatTimeoutSeconds() * 1000.0));
		double movementThreshold = adaptiveUi == null ? 0.08 : adaptiveUi.getMovementSpeedThreshold();
		double lowHealthThreshold = adaptiveUi == null ? 8.0 : adaptiveUi.getLowHealthThreshold();

		long nowMs = System.currentTimeMillis();
		if (client.player.hurtTime > 0) {
			lastCombatAtMs = nowMs;
		}
		if (client.options != null && client.options.keyAttack.isDown() && client.hitResult instanceof EntityHitResult) {
			lastCombatAtMs = nowMs;
		}

		boolean inCombat = (nowMs - lastCombatAtMs) <= combatTimeoutMs;
		boolean lowHealth = client.player.getHealth() <= lowHealthThreshold;
		double horizontalSpeed = client.player.getDeltaMovement().horizontalDistance();
		boolean moving = horizontalSpeed >= movementThreshold;

		PlayerContext nextContext = chooseContext(inCombat, lowHealth, moving, adaptiveUi);

		setContext(nextContext);
		applyContextTheme(nextContext);
	}

	private void applyContextTheme(PlayerContext context) {
		int target = CONTEXT_COLORS.getOrDefault(context, ClientColors.DEFAULT_PRIMARY_RGB);
		activeThemeRgb = lerpRgb(activeThemeRgb, target, COLOR_BLEND_RATE);
		ClientColors.setPrimaryColorRgb(activeThemeRgb);
	}

	private PlayerContext chooseContext(boolean inCombat, boolean lowHealth, boolean moving, AdaptiveUiModule adaptiveUi) {
		PlayerContext selected = PlayerContext.IDLE;
		int selectedPriority = Integer.MIN_VALUE;

		if (inCombat && adaptiveUi.isContextEnabled("combat")) {
			int priority = adaptiveUi.getContextPriority("combat");
			if (priority > selectedPriority) {
				selected = PlayerContext.COMBAT;
				selectedPriority = priority;
			}
		}

		if (lowHealth && adaptiveUi.isContextEnabled("low_health")) {
			int priority = adaptiveUi.getContextPriority("low_health");
			if (priority > selectedPriority) {
				selected = PlayerContext.LOW_HEALTH;
				selectedPriority = priority;
			}
		}

		if (moving && adaptiveUi.isContextEnabled("moving")) {
			int priority = adaptiveUi.getContextPriority("moving");
			if (priority > selectedPriority) {
				selected = PlayerContext.MOVING;
				selectedPriority = priority;
			}
		}

		if (adaptiveUi.isContextEnabled("idle")) {
			int priority = adaptiveUi.getContextPriority("idle");
			if (priority > selectedPriority) {
				selected = PlayerContext.IDLE;
			}
		}

		return selected;
	}

	private AdaptiveUiModule getAdaptiveModule() {
		Module module = moduleManager.get("adaptiveui");
		if (module instanceof AdaptiveUiModule adaptiveUiModule) {
			return adaptiveUiModule;
		}
		return null;
	}

	private void setContext(PlayerContext next) {
		if (next == currentContext) {
			return;
		}

		PlayerContext previous = currentContext;
		currentContext = next;
		pushContextNotification(next);
		for (ContextChangeListener listener : listeners) {
			listener.onContextChanged(previous, next);
		}
	}

	private void pushContextNotification(PlayerContext context) {
		AdaptiveUiModule adaptiveUi = getAdaptiveModule();
		if (adaptiveUi == null || !adaptiveUi.shouldShowContextNotifications()) {
			return;
		}

		long nowMs = System.currentTimeMillis();
		if (nowMs - lastContextNotificationAtMs < CONTEXT_NOTIFICATION_COOLDOWN_MS) {
			return;
		}

		activeContextLabel = context.name() + " MODE";
		contextLabelStartedAtMs = nowMs;
		lastContextNotificationAtMs = nowMs;
	}

	private int getBaseColor() {
		OverlayModule overlay = OverlayModule.getInstance();
		if (overlay == null) {
			return ClientColors.DEFAULT_PRIMARY_RGB;
		}
		return overlay.getPrimaryColorRgb();
	}

	private int lerpRgb(int fromRgb, int toRgb, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int fr = (fromRgb >> 16) & 0xFF;
		int fg = (fromRgb >> 8) & 0xFF;
		int fb = fromRgb & 0xFF;
		int tr = (toRgb >> 16) & 0xFF;
		int tg = (toRgb >> 8) & 0xFF;
		int tb = toRgb & 0xFF;

		int r = (int) (fr + ((tr - fr) * t));
		int g = (int) (fg + ((tg - fg) * t));
		int b = (int) (fb + ((tb - fb) * t));
		return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}
}
