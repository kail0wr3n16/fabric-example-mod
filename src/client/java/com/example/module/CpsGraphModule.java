package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;

public class CpsGraphModule extends Module {
    private final NumberSetting graphWidth;
    private final NumberSetting graphHeight;
    private final BooleanSetting showNumber;

    // Track click timestamps in the last 1 second
    private final Deque<Long> clickTimes = new ArrayDeque<>();
    // One CPS sample per tick for graph (last N ticks)
    private final Deque<Integer> cpsHistory = new ArrayDeque<>();
    private static final int HISTORY_TICKS = 40; // ~2s of history

    private boolean prevAttackDown = false;
    private boolean prevUseDown = false;
    private int tickCps = 0;
    private int tickClickCount = 0;

    public CpsGraphModule() {
        super("cpsgraph", ModuleCategory.COMBAT, false);
        this.graphWidth = addSetting(new NumberSetting("graphWidth", 60.0, 30.0, 120.0));
        this.graphHeight = addSetting(new NumberSetting("graphHeight", 30.0, 15.0, 60.0));
        this.showNumber = addSetting(new BooleanSetting("showNumber", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();

        // Detect attack clicks (left mouse)
        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !prevAttackDown) {
            clickTimes.addLast(now);
            tickClickCount++;
        }
        prevAttackDown = attackDown;

        // Prune old clicks (> 1 second old)
        while (!clickTimes.isEmpty() && now - clickTimes.peekFirst() > 1000) {
            clickTimes.removeFirst();
        }

        // Record CPS sample every tick
        tickCps = clickTimes.size();
        cpsHistory.addLast(tickCps);
        if (cpsHistory.size() > HISTORY_TICKS) cpsHistory.removeFirst();

        tickClickCount = 0;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int gw = graphWidth.asInt();
        int gh = graphHeight.asInt();
        int x = guiGraphics.guiWidth() - gw - 6;
        int y = guiGraphics.guiHeight() - gh - 20;

        // Background
        guiGraphics.fill(x, y, x + gw, y + gh, 0xAA0A0C10);
        guiGraphics.fill(x, y, x + gw, y + 1, 0xFF3A7FD4);
        guiGraphics.fill(x, y + gh - 1, x + gw, y + gh, 0xFF3A7FD4);
        guiGraphics.fill(x, y, x + 1, y + gh, 0xFF3A7FD4);
        guiGraphics.fill(x + gw - 1, y, x + gw, y + gh, 0xFF3A7FD4);

        if (!cpsHistory.isEmpty()) {
            Integer[] samples = cpsHistory.toArray(new Integer[0]);
            int maxCps = 20; // scale

            int barW = Math.max(1, (gw - 2) / HISTORY_TICKS);
            for (int i = 0; i < samples.length; i++) {
                int cps = samples[i];
                if (cps == 0) continue;
                int barH = Math.max(1, (int)((float)cps / maxCps * (gh - 2)));
                int bx = x + 1 + i * barW;
                int by = y + gh - 1 - barH;

                float intensity = (float)cps / maxCps;
                int red = (int)(255 * Math.min(1, intensity * 2));
                int green = (int)(255 * Math.min(1, 2 - intensity * 2));
                int barColor = 0xFF000000 | (red << 16) | (green << 8) | 50;
                guiGraphics.fill(bx, by, bx + barW, y + gh - 1, barColor);
            }
        }

        if (showNumber.isEnabled()) {
            String cpsStr = tickCps + " CPS";
            int tw = mc.font.width(cpsStr);
            guiGraphics.text(mc.font, Component.literal(cpsStr), x + gw / 2 - tw / 2, y + gh + 1, 0xFFAAAAAA, false);
        }
    }

    @Override
    protected void onDisable() {
        clickTimes.clear();
        cpsHistory.clear();
    }
}
