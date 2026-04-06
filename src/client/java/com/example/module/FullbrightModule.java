package com.example.module;

import com.example.context.PlayerContext;
import com.example.module.setting.NumberSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Boosts screen brightness (gamma) beyond the vanilla slider maximum.
 * This is equivalent to dragging the vanilla Brightness slider — just more convenient.
 */
public class FullbrightModule extends Module {
    private final NumberSetting gammaLevel;

    private static final double BASE_GAMMA = 0.85;
    private static boolean active;
    private PlayerContext currentContext = PlayerContext.IDLE;
    private Double previousGamma;

    public FullbrightModule() {
        super("brightnessboost", ModuleCategory.RENDER, false);
        this.gammaLevel = addSetting(new NumberSetting("gamma", 1.0, 0.5, 5.0));
    }

    @Override
    public KeyMapping createDefaultKeybind(String modId) {
        return new KeyMapping(
            "key.clientmodules.toggle_brightnessboost",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.MISC
        );
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null) return;
        active = true;
        if (previousGamma == null) {
            previousGamma = client.options.gamma().get();
        }
        client.options.gamma().set(getTargetGamma(client));
    }

    @Override
    public void onTick(Minecraft client) {
        double target = getTargetGamma(client);
        if (client.options != null && Math.abs(client.options.gamma().get() - target) > 0.0001) {
            client.options.gamma().set(target);
        }
    }

    @Override
    public void onContextChanged(PlayerContext context) {
        this.currentContext = context;
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        active = false;
        if (client.options == null || previousGamma == null) return;
        client.options.gamma().set(previousGamma);
        previousGamma = null;
    }

    public static boolean isActive() {
        return active;
    }

    private double getTargetGamma(Minecraft client) {
        double configured = gammaLevel.getValue();
        // In low-light or low-health, always apply the configured level
        if (currentContext == PlayerContext.LOW_HEALTH) return configured;
        if (client.player != null && client.player.getLightLevelDependentMagicValue() < 0.45f) return configured;
        // In well-lit areas, blend toward the configured level from a base
        return Math.max(BASE_GAMMA, configured);
    }
}