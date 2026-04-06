package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;

public class HitStatsModule extends Module {
    private static final int MAX_SAMPLES = 30;

    private final BooleanSetting showDistance;
    private final BooleanSetting showInterval;

    private final Deque<Double> hitDistances = new ArrayDeque<>();
    private final Deque<Long> hitTimes = new ArrayDeque<>();
    private long lastHitMs = 0;
    private int totalHits = 0;

    public HitStatsModule() {
        super("hitstats", ModuleCategory.HUD, false);
        this.showDistance = addSetting(new BooleanSetting("showDistance", true));
        this.showInterval = addSetting(new BooleanSetting("showInterval", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !(e instanceof LivingEntity le)) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            if (mc.player.distanceTo(e) > 6.0) continue;

            if (le.hurtTime > 0 && le.hurtTime == le.hurtDuration) {
                double dist = mc.player.distanceTo(e);
                long now = System.currentTimeMillis();

                hitDistances.addLast(dist);
                hitTimes.addLast(now);
                if (hitDistances.size() > MAX_SAMPLES) hitDistances.removeFirst();
                if (hitTimes.size() > MAX_SAMPLES) hitTimes.removeFirst();

                lastHitMs = now;
                totalHits++;
                break;
            }
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int x = 5;
        int y = guiGraphics.guiHeight() - 80;
        int w = 115;
        int lineH = 10;
        int lines = 1 + (showDistance.isEnabled() ? 1 : 0) + (showInterval.isEnabled() ? 1 : 0);
        int h = lines * lineH + 8;

        guiGraphics.fill(x, y, x + w, y + h, 0x88000000);
        int ty = y + 4;

        guiGraphics.text(mc.font, Component.literal("Hits: " + totalHits), x + 4, ty, 0xFFE8EEF8, false);
        ty += lineH;

        if (showDistance.isEnabled() && !hitDistances.isEmpty()) {
            double avgDist = hitDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double maxDist = hitDistances.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            String distStr = String.format("Dist: %.2f (max %.2f)", avgDist, maxDist);
            guiGraphics.text(mc.font, Component.literal(distStr), x + 4, ty, 0xFF55FFFF, false);
            ty += lineH;
        }

        if (showInterval.isEnabled() && hitTimes.size() >= 2) {
            Long[] times = hitTimes.toArray(new Long[0]);
            double totalMs = 0;
            for (int i = 1; i < times.length; i++) totalMs += times[i] - times[i - 1];
            double avgMs = totalMs / (times.length - 1);
            String ivStr = String.format("Interval: %.0fms", avgMs);
            guiGraphics.text(mc.font, Component.literal(ivStr), x + 4, ty, 0xFFFFAA55, false);
        }
    }

    @Override
    protected void onDisable() {
        hitDistances.clear();
        hitTimes.clear();
        totalHits = 0;
    }
}
