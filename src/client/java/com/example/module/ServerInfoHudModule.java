package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

/**
 * HUD element showing server ping and a client-side TPS estimate.
 * Helps players see server health at a glance without any third-party tool.
 */
public class ServerInfoHudModule extends Module {
    private final BooleanSetting showPing;
    private final BooleanSetting showTps;

    // TPS estimation via tick deltas
    private long lastTickTime = System.currentTimeMillis();
    private double estimatedTps = 20.0;
    private static final double SMOOTHING = 0.1;

    public ServerInfoHudModule() {
        super("serverinfo", ModuleCategory.HUD, false);
        this.showPing = addSetting(new BooleanSetting("showPing", true));
        this.showTps = addSetting(new BooleanSetting("showTps", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        long now = System.currentTimeMillis();
        long delta = now - lastTickTime;
        lastTickTime = now;

        if (delta > 0 && delta < 2000) {
            double instantTps = Math.min(20.0, 1000.0 / delta);
            estimatedTps = estimatedTps + SMOOTHING * (instantTps - estimatedTps);
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int x = 4;
        int y = 4;

        if (showPing.isEnabled()) {
            int ping = getPing(mc);
            String pingStr = "Ping: " + (ping < 0 ? "N/A" : ping + "ms");
            int pingColor = ping < 0 ? 0xFF888888
                : ping < 80  ? 0xFF55FF55
                : ping < 150 ? 0xFFFFFF55
                : ping < 300 ? 0xFFFFAA00
                : 0xFFFF5555;
            guiGraphics.text(mc.font, Component.literal(pingStr), x, y, pingColor, true);
            y += 10;
        }

        if (showTps.isEnabled()) {
            String tpsStr = String.format("TPS: %.1f", estimatedTps);
            int tpsColor = estimatedTps > 18 ? 0xFF55FF55
                : estimatedTps > 14 ? 0xFFFFFF55
                : estimatedTps > 8  ? 0xFFFFAA00
                : 0xFFFF5555;
            guiGraphics.text(mc.font, Component.literal(tpsStr), x, y, tpsColor, true);
        }
    }

    private int getPing(Minecraft mc) {
        try {
            ClientPacketListener conn = mc.getConnection();
            if (conn == null || mc.player == null) return -1;
            PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
            return info != null ? info.getLatency() : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}
