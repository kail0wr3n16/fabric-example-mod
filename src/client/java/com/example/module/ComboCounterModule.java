package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Tracks how many times in a row you've landed hits without a long gap.
 * Rebranded as "Hit Streak" — useful for gauging your combat rhythm, not for exploiting.
 */
public class ComboCounterModule extends Module {
    private final BooleanSetting showBest;

    private int streak = 0;
    private int bestStreak = 0;
    private long lastHitTime = 0;
    private float displayAlpha = 0f;
    private static final long STREAK_TIMEOUT_MS = 3000;

    public ComboCounterModule() {
        super("hitstreak", ModuleCategory.HUD, false);
        this.showBest = addSetting(new BooleanSetting("showBest", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !(e instanceof LivingEntity le)) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            if (mc.player.distanceTo(e) > 5.0) continue;

            if (le.hurtTime > 0 && le.hurtTime == le.hurtDuration) {
                long now = System.currentTimeMillis();
                if (streak == 0 || now - lastHitTime < STREAK_TIMEOUT_MS) {
                    streak++;
                    if (streak > bestStreak) bestStreak = streak;
                } else {
                    streak = 1;
                }
                lastHitTime = now;
                break;
            }
        }

        if (streak > 0 && System.currentTimeMillis() - lastHitTime > STREAK_TIMEOUT_MS) {
            streak = 0;
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float targetAlpha = streak > 0 ? 1f : 0f;
        displayAlpha += (targetAlpha - displayAlpha) * 0.15f;
        if (displayAlpha < 0.02f) return;

        int alpha = (int) (displayAlpha * 200);
        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();

        String streakStr = streak + "x streak";
        int comboW = mc.font.width(streakStr);
        int x = sw / 2 - comboW / 2;
        int y = sh / 2 + 52;

        // Steady color — no flashy pulse
        guiGraphics.text(mc.font, Component.literal(streakStr), x, y,
            (alpha << 24) | 0x54C5FF, true);

        if (showBest.isEnabled() && bestStreak > 0) {
            String bestStr = "Best: " + bestStreak;
            int bestW = mc.font.width(bestStr);
            guiGraphics.text(mc.font, Component.literal(bestStr),
                sw / 2 - bestW / 2, y + 10, (alpha << 24) | 0x667788, false);
        }
    }

    @Override
    protected void onDisable() {
        streak = 0;
        bestStreak = 0;
    }
}
