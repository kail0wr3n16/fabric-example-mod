package com.example.module;

import com.example.context.PlayerContext;
import com.example.module.setting.BooleanSetting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Automatically sprints when moving forward (same as OptiFine/Lunar sprint).
 * Optionally shows a small "SPR" indicator when active.
 */
public class AutoSprintModule extends Module {
    private final BooleanSetting showIndicator;
    private PlayerContext currentContext = PlayerContext.IDLE;

    public AutoSprintModule() {
        super("autosprint", ModuleCategory.QOL, false);
        this.showIndicator = addSetting(new BooleanSetting("showIndicator", true));
    }

    @Override
    public KeyMapping createDefaultKeybind(String modId) {
        return new KeyMapping(
            "key.clientmodules.toggle_autosprint",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.MISC
        );
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.player.input == null) return;
        if (currentContext == PlayerContext.IDLE) {
            client.player.setSprinting(false);
            return;
        }

        boolean hasForwardInput = client.player.input.hasForwardImpulse();
        boolean canSprint = hasForwardInput
            && !client.player.isCrouching()
            && !client.player.isUsingItem();
        client.player.setSprinting(canSprint);
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        if (!showIndicator.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isSprinting()) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        // Small badge below the crosshair area
        guiGraphics.text(mc.font, Component.literal("SPR"),
            sw / 2 + 8, sh / 2 + 6, 0xAA54C5FF, false);
    }

    @Override
    public void onContextChanged(PlayerContext context) {
        this.currentContext = context;
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.setSprinting(false);
        }
    }
}
