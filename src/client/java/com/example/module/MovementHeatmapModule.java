package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;

public class MovementHeatmapModule extends Module {
    private final BooleanSetting showRaw;

    // Strafe buckets: -1.0 to +1.0 split into 20 buckets
    private static final int BUCKETS = 20;
    private final int[] strafeBuckets = new int[BUCKETS];
    private final int[] forwardBuckets = new int[BUCKETS];
    private int totalSamples = 0;
    private static final int MAX_SAMPLES = 600; // ~30s at 20tps

    public MovementHeatmapModule() {
        super("movementheatmap", ModuleCategory.MOVEMENT, false);
        this.showRaw = addSetting(new BooleanSetting("showRaw", false));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.player.input == null || mc.player.input.keyPresses == null) return;
        if (!mc.player.isSprinting() && mc.player.getDeltaMovement().horizontalDistance() < 0.05) return;

        float leftImpulse = readImpulse(mc.player.input.keyPresses, "leftImpulse", "strafeImpulse", "xxa");
        float forwardImpulse = readImpulse(mc.player.input.keyPresses, "forwardImpulse", "zza", "yya");

        // Map -1..1 to 0..BUCKETS-1
        int strafeBucket = Math.max(0, Math.min(BUCKETS - 1, (int)((leftImpulse + 1.0f) / 2.0f * BUCKETS)));
        int forwardBucket = Math.max(0, Math.min(BUCKETS - 1, (int)((forwardImpulse + 1.0f) / 2.0f * BUCKETS)));

        strafeBuckets[strafeBucket]++;
        forwardBuckets[forwardBucket]++;
        totalSamples++;

        // Rolling decay every ~30s
        if (totalSamples > MAX_SAMPLES) {
            for (int i = 0; i < BUCKETS; i++) {
                strafeBuckets[i] = (int)(strafeBuckets[i] * 0.95f);
                forwardBuckets[i] = (int)(forwardBuckets[i] * 0.95f);
            }
            totalSamples = (int)(totalSamples * 0.95f);
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int mapW = 80;
        int mapH = 80;
        int x = guiGraphics.guiWidth() - mapW - 8;
        int y = guiGraphics.guiHeight() / 2 - mapH / 2;

        guiGraphics.fill(x, y, x + mapW, y + mapH, 0xAA0A0C10);
        guiGraphics.fill(x, y, x + mapW, y + 1, 0xFF444444);
        guiGraphics.fill(x, y + mapH - 1, x + mapW, y + mapH, 0xFF444444);
        guiGraphics.fill(x, y, x + 1, y + mapH, 0xFF444444);
        guiGraphics.fill(x + mapW - 1, y, x + mapW, y + mapH, 0xFF444444);

        // Axes
        int cx = x + mapW / 2;
        int cy = y + mapH / 2;
        guiGraphics.fill(cx, y + 2, cx + 1, y + mapH - 2, 0x44FFFFFF);
        guiGraphics.fill(x + 2, cy, x + mapW - 2, cy + 1, 0x44FFFFFF);

        // Find max bucket for normalization
        int maxStrafe = 1, maxForward = 1;
        for (int i = 0; i < BUCKETS; i++) {
            if (strafeBuckets[i] > maxStrafe) maxStrafe = strafeBuckets[i];
            if (forwardBuckets[i] > maxForward) maxForward = forwardBuckets[i];
        }

        int cellW = (mapW - 2) / BUCKETS;
        int barMaxH = (mapH / 2) - 3;
        if (cellW < 1) cellW = 1;

        // Strafe heatmap (horizontal bar — bottom half)
        for (int i = 0; i < BUCKETS; i++) {
            float ratio = (float)strafeBuckets[i] / maxStrafe;
            int barH = (int)(ratio * barMaxH);
            if (barH <= 0) continue;
            int bx = x + 1 + i * cellW;
            int heat = heatColor(ratio);
            guiGraphics.fill(bx, cy + 1, bx + cellW, cy + 1 + barH, heat);
        }

        // Forward heatmap (vertical bar — left half, going up)
        int cellH = (mapH - 2) / BUCKETS;
        if (cellH < 1) cellH = 1;
        int barMaxW = (mapW / 2) - 3;
        for (int i = 0; i < BUCKETS; i++) {
            float ratio = (float)forwardBuckets[i] / maxForward;
            int barW2 = (int)(ratio * barMaxW);
            if (barW2 <= 0) continue;
            int by = y + mapH - 2 - (i + 1) * cellH;
            int heat = heatColor(ratio);
            guiGraphics.fill(cx + 1, by, cx + 1 + barW2, by + cellH, heat);
        }

        guiGraphics.text(mc.font, Component.literal("Heatmap"), x + 2, y - 9, 0xFFAAAAAA, false);
        guiGraphics.text(mc.font, Component.literal("L↔R"), x + 2, y + mapH + 1, 0xFF666666, false);
    }

    private int heatColor(float ratio) {
        int r = (int)(255 * Math.min(1f, ratio * 2));
        int g = (int)(255 * Math.min(1f, (1f - ratio) * 2));
        return 0xFF000000 | (r << 16) | (g << 8) | 50;
    }

    private float readImpulse(Object input, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = input.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.getFloat(input);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return 0.0f;
    }

    @Override
    protected void onDisable() {
        for (int i = 0; i < BUCKETS; i++) {
            strafeBuckets[i] = 0;
            forwardBuckets[i] = 0;
        }
        totalSamples = 0;
    }
}
