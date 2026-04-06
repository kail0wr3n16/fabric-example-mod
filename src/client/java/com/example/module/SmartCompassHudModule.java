package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class SmartCompassHudModule extends Module {
    private final BooleanSetting showWaypoint;
    private final NumberSetting yPos;

    public SmartCompassHudModule() {
        super("smartcompass", ModuleCategory.MISC, false);
        this.showWaypoint = addSetting(new BooleanSetting("showWaypoint", true));
        this.yPos = addSetting(new NumberSetting("yPos", 10.0, 5.0, 60.0));
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float yaw = ((mc.player.getYRot() % 360) + 360) % 360;
        int sw = guiGraphics.guiWidth();
        int compassW = 180;
        int cx = sw / 2;
        int y = yPos.getValue().intValue();

        // Background bar
        guiGraphics.fill(cx - compassW / 2, y, cx + compassW / 2, y + 13, 0x88000000);

        // Draw compass ticks
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        float[] dirAngles = {0, 45, 90, 135, 180, 225, 270, 315};

        for (int i = 0; i < dirs.length; i++) {
            float dirYaw = dirAngles[i];
            float delta = wrapDegrees(dirYaw - yaw);

            if (Math.abs(delta) > 90) continue;

            int xOff = (int)((delta / 90f) * (compassW / 2));
            int tickX = cx + xOff;

            boolean isCardinal = i % 2 == 0;
            int color = isCardinal ? (i == 0 ? 0xFFFF5555 : 0xFFE8EEF8) : 0xFF888888;
            int tickH = isCardinal ? 5 : 3;

            guiGraphics.fill(tickX, y + 1, tickX + 1, y + 1 + tickH, color);
            if (isCardinal) {
                String label = dirs[i];
                int lw = mc.font.width(label);
                guiGraphics.text(mc.font, Component.literal(label), tickX - lw / 2, y + 4, color, false);
            }
        }

        // Center indicator
        guiGraphics.fill(cx, y, cx + 1, y + 13, 0xFFFFFFFF);

        // Coordinates row
        String coords = String.format("%.0f / %.0f / %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
        int coordW = mc.font.width(coords);
        guiGraphics.text(mc.font, Component.literal(coords), cx - coordW / 2, y + 14, 0xFFAAAAAA, false);

        // Waypoint bearing
        if (showWaypoint.isEnabled()) {
            WaypointsModule wm = WaypointsModule.getInstance();
            if (wm != null && wm.isEnabled() && !wm.getWaypoints().isEmpty()) {
                WaypointsModule.Waypoint nearest = null;
                double bestDist = Double.MAX_VALUE;
                Vec3 pp = mc.player.position();
                for (WaypointsModule.Waypoint wp : wm.getWaypoints()) {
                    double d = wp.distanceTo(pp);
                    if (d < bestDist) { bestDist = d; nearest = wp; }
                }
                if (nearest != null) {
                    double dx = nearest.x - pp.x;
                    double dz = nearest.z - pp.z;
                    float wpBearing = (float)(Math.toDegrees(Math.atan2(dz, dx)) + 90);
                    float delta = wrapDegrees(wpBearing - yaw);
                    if (Math.abs(delta) <= 90) {
                        int xOff = (int)((delta / 90f) * (compassW / 2));
                        int wpX = cx + xOff;
                        int color = 0xFF000000 | (nearest.color & 0xFFFFFF);
                        guiGraphics.fill(wpX - 1, y, wpX + 2, y + 13, color);
                        String wpLabel = nearest.name.length() > 6 ? nearest.name.substring(0, 6) : nearest.name;
                        guiGraphics.text(mc.font, Component.literal(wpLabel), wpX - mc.font.width(wpLabel) / 2, y + 14, color, false);
                    }
                }
            }
        }
    }

    private float wrapDegrees(float d) {
        d = d % 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }
}
