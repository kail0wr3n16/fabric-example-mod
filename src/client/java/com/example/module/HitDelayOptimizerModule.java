package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

public class HitDelayOptimizerModule extends Module {
    private final BooleanSetting showTiming;
    private final BooleanSetting showConsistency;

    // Track attack cooldown completion for feedback
    private final Deque<Long> hitTimestamps = new ArrayDeque<>();
    private float lastCooldown = 1.0f;
    private boolean wasFullyCharged = false;
    private int lastTargetHurtTime = 0;
    private long lastHitMs = 0;
    private float avgInterval = 0f;
    private float consistency = 100f; // 0-100%

    private static final int MAX_HITS = 20;

    public HitDelayOptimizerModule() {
        super("hitdelay", ModuleCategory.COMBAT, false);
        this.showTiming = addSetting(new BooleanSetting("showTiming", true));
        this.showConsistency = addSetting(new BooleanSetting("showConsistency", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;

        float cooldown = mc.player.getAttackStrengthScale(0.5f);
        boolean isNowFull = cooldown >= 0.9f;

        // Detect hit on target entity
        if (mc.crosshairPickEntity instanceof LivingEntity le) {
            int hurtTime = le.hurtTime;
            if (hurtTime > lastTargetHurtTime && hurtTime == le.hurtDuration) {
                // Entity was just hit
                long now = System.currentTimeMillis();
                if (lastHitMs > 0) {
                    float interval = (now - lastHitMs) / 1000f;
                    // Update rolling average
                    hitTimestamps.addLast(now);
                    if (hitTimestamps.size() > MAX_HITS) hitTimestamps.removeFirst();

                    // Compute avg interval from timestamps
                    if (hitTimestamps.size() >= 2) {
                        Long[] times = hitTimestamps.toArray(new Long[0]);
                        float totalInterval = 0;
                        for (int i = 1; i < times.length; i++) {
                            totalInterval += (times[i] - times[i-1]) / 1000f;
                        }
                        avgInterval = totalInterval / (times.length - 1);

                        // Consistency: how close each interval is to the average
                        float variance = 0;
                        for (int i = 1; i < times.length; i++) {
                            float iv = (times[i] - times[i-1]) / 1000f;
                            float diff = iv - avgInterval;
                            variance += diff * diff;
                        }
                        float stdDev = (float)Math.sqrt(variance / (times.length - 1));
                        consistency = Math.max(0, Math.min(100, 100f - (stdDev / avgInterval) * 100f));
                    }
                }
                lastHitMs = now;
            }
            lastTargetHurtTime = hurtTime;
        }

        lastCooldown = cooldown;
        wasFullyCharged = isNowFull;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int x = 5;
        int y = guiGraphics.guiHeight() - 60;
        int w = 110;
        int lineH = 10;
        int lines = 1 + (showTiming.isEnabled() ? 1 : 0) + (showConsistency.isEnabled() ? 1 : 0);
        int h = lines * lineH + 8;

        guiGraphics.fill(x, y, x + w, y + h, 0x88000000);

        int ty = y + 4;
        // Attack cooldown bar
        float cooldown = mc.player.getAttackStrengthScale(0.5f);
        int barW = w - 8;
        guiGraphics.fill(x + 4, ty, x + 4 + barW, ty + 4, 0x44FFFFFF);
        int filledW = (int)(barW * cooldown);
        int barColor = cooldown >= 0.9f ? 0xFF55FF55 : (cooldown >= 0.5f ? 0xFFFFAA00 : 0xFFFF5555);
        if (filledW > 0) guiGraphics.fill(x + 4, ty, x + 4 + filledW, ty + 4, barColor);
        ty += 6;

        if (showTiming.isEnabled() && avgInterval > 0) {
            String intervalStr = String.format("Avg: %.2fs", avgInterval);
            guiGraphics.text(mc.font, Component.literal(intervalStr), x + 4, ty, 0xFFAAAAFF, false);
            ty += lineH;
        }

        if (showConsistency.isEnabled() && hitTimestamps.size() >= 3) {
            int consistAlpha = 255;
            int cColor = consistency > 80 ? 0xFF55FF55 : (consistency > 50 ? 0xFFFFAA00 : 0xFFFF5555);
            String cStr = String.format("Cons: %.0f%%", consistency);
            guiGraphics.text(mc.font, Component.literal(cStr), x + 4, ty, cColor, false);
        }
    }

    @Override
    protected void onDisable() {
        hitTimestamps.clear();
        lastHitMs = 0;
        avgInterval = 0;
        consistency = 100;
    }
}
