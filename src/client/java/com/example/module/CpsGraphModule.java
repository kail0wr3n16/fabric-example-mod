package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks left-click and right-click frequency over a rolling window.
 * Useful for practice, accessibility, and performance awareness.
 * No combat framing — just interaction rate information.
 */
public class CpsGraphModule extends Module {
    private final BooleanSetting showLabel;

    private final Deque<Long> leftClickTimes = new ArrayDeque<>();
    private final Deque<Long> rightClickTimes = new ArrayDeque<>();
    private final Deque<Integer> leftHistory = new ArrayDeque<>();
    private final Deque<Integer> rightHistory = new ArrayDeque<>();
    private static final int HISTORY_TICKS = 40;

    private boolean prevLeftDown = false;
    private boolean prevRightDown = false;
    private int currentLeft = 0;
    private int currentRight = 0;

    public CpsGraphModule() {
        super("inputgraph", ModuleCategory.HUD, false);
        this.showLabel = addSetting(new BooleanSetting("showLabel", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        boolean leftDown = mc.options.keyAttack.isDown();
        if (leftDown && !prevLeftDown) leftClickTimes.addLast(now);
        prevLeftDown = leftDown;

        boolean rightDown = mc.options.keyUse.isDown();
        if (rightDown && !prevRightDown) rightClickTimes.addLast(now);
        prevRightDown = rightDown;

        // Prune clicks older than 1 second
        while (!leftClickTimes.isEmpty() && now - leftClickTimes.peekFirst() > 1000) leftClickTimes.removeFirst();
        while (!rightClickTimes.isEmpty() && now - rightClickTimes.peekFirst() > 1000) rightClickTimes.removeFirst();

        currentLeft = leftClickTimes.size();
        currentRight = rightClickTimes.size();

        leftHistory.addLast(currentLeft);
        rightHistory.addLast(currentRight);
        if (leftHistory.size() > HISTORY_TICKS) leftHistory.removeFirst();
        if (rightHistory.size() > HISTORY_TICKS) rightHistory.removeFirst();
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int gw = 64;
        int gh = 32;
        int x = guiGraphics.guiWidth() - gw - 6;
        int y = guiGraphics.guiHeight() - gh - 22;

        // Background
        guiGraphics.fill(x, y, x + gw, y + gh, 0xAA0A0C10);
        guiGraphics.fill(x, y, x + gw, y + 1, 0xFF54C5FF);

        // Draw left-click bars (top half) in blue
        if (!leftHistory.isEmpty()) {
            Integer[] samples = leftHistory.toArray(new Integer[0]);
            int maxRate = 20;
            int barW = Math.max(1, (gw - 2) / HISTORY_TICKS);
            int halfH = (gh - 2) / 2;
            for (int i = 0; i < samples.length; i++) {
                int cps = samples[i];
                if (cps == 0) continue;
                int barH = Math.max(1, (int) ((float) cps / maxRate * halfH));
                int bx = x + 1 + i * barW;
                guiGraphics.fill(bx, y + 1 + halfH - barH, bx + barW, y + 1 + halfH, 0xAA54C5FF);
            }

            // Right-click bars (bottom half) in teal
            Integer[] rSamples = rightHistory.toArray(new Integer[0]);
            for (int i = 0; i < rSamples.length; i++) {
                int cps = rSamples[i];
                if (cps == 0) continue;
                int barH = Math.max(1, (int) ((float) cps / maxRate * halfH));
                int bx = x + 1 + i * barW;
                guiGraphics.fill(bx, y + 1 + halfH, bx + barW, y + 1 + halfH + barH, 0xAA3AC5AA);
            }
        }

        if (showLabel.isEnabled()) {
            String label = currentLeft + "L  " + currentRight + "R";
            int tw = mc.font.width(label);
            guiGraphics.text(mc.font, Component.literal(label),
                x + gw / 2 - tw / 2, y + gh + 1, 0xFF8899AA, false);
        }
    }

    @Override
    protected void onDisable() {
        leftClickTimes.clear();
        rightClickTimes.clear();
        leftHistory.clear();
        rightHistory.clear();
    }
}
