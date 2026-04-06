package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ComboCounterModule extends Module {
    private final BooleanSetting showOnlyActive;

    private int combo = 0;
    private int maxCombo = 0;
    private long lastHitTime = 0;
    private float displayAlpha = 0f;
    private static final long COMBO_TIMEOUT_MS = 2500;

    // Track last seen hurtTime on nearby entities
    private int lastHurtTime = 0;

    public ComboCounterModule() {
        super("combocounter", ModuleCategory.COMBAT, false);
        this.showOnlyActive = addSetting(new BooleanSetting("showOnlyActive", false));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        // Detect hit: find a nearby entity that was just hit
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !(e instanceof LivingEntity le)) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            if (mc.player.distanceTo(e) > 5.0) continue;

            if (le.hurtTime > 0 && le.hurtTime == le.hurtDuration) {
                long now = System.currentTimeMillis();
                if (now - lastHitTime < COMBO_TIMEOUT_MS || combo == 0) {
                    combo++;
                    if (combo > maxCombo) maxCombo = combo;
                } else {
                    combo = 1;
                }
                lastHitTime = now;
                break;
            }
        }

        // Reset combo on timeout
        if (combo > 0 && System.currentTimeMillis() - lastHitTime > COMBO_TIMEOUT_MS) {
            combo = 0;
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean active = combo > 0;
        float targetAlpha = (active || !showOnlyActive.isEnabled()) ? 1f : 0f;
        displayAlpha += (targetAlpha - displayAlpha) * 0.15f;
        if (displayAlpha < 0.02f) return;

        int alpha = (int)(displayAlpha * 255);

        String comboStr = combo > 0 ? combo + "x COMBO" : "0x";
        String maxStr = "Best: " + maxCombo;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int comboW = mc.font.width(comboStr);
        int x = sw / 2 - comboW / 2;
        int y = sh / 2 + 50;

        if (combo >= 5) {
            // Flashy display for high combos
            float pulse = (float)((System.currentTimeMillis() % 800) / 800.0);
            int r = (int)(255 * pulse);
            int g = (int)(200 * (1 - pulse));
            int comboColor = (alpha << 24) | (r << 16) | (g << 8) | 50;
            guiGraphics.text(mc.font, Component.literal(comboStr), x, y, comboColor, true);
        } else {
            int comboColor = (alpha << 24) | 0xFFFFAA;
            guiGraphics.text(mc.font, Component.literal(comboStr), x, y, comboColor, true);
        }

        if (maxCombo > 0) {
            int maxW = mc.font.width(maxStr);
            guiGraphics.text(mc.font, Component.literal(maxStr),
                sw / 2 - maxW / 2, y + 10, (alpha << 24) | 0x888888, false);
        }
    }

    @Override
    protected void onDisable() {
        combo = 0;
        maxCombo = 0;
    }
}
