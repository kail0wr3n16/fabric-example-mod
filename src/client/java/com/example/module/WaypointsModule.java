package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class WaypointsModule extends Module {
    private static WaypointsModule INSTANCE;

    public static class Waypoint {
        public String name;
        public double x, y, z;
        public int color;

        public Waypoint(String name, double x, double y, double z, int color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
        }

        public double distanceTo(Vec3 pos) {
            double dx = x - pos.x, dy = y - pos.y, dz = z - pos.z;
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }
    }

    private final List<Waypoint> waypoints = new ArrayList<>();
    private final NumberSetting maxDisplay;
    private final BooleanSetting showEta;
    private final NumberSetting displayRange;

    private static final int[] WAYPOINT_COLORS = {
        0xFF55FFFF, 0xFFFF55FF, 0xFF55FF55, 0xFFFFFF55,
        0xFFFF5555, 0xFF5555FF, 0xFFFFAA00, 0xFFAAAAAA
    };
    private int colorIndex = 0;

    public WaypointsModule() {
        super("waypoints", ModuleCategory.MISC, false);
        this.maxDisplay = addSetting(new NumberSetting("maxDisplay", 5.0, 1.0, 10.0));
        this.showEta = addSetting(new BooleanSetting("showEta", true));
        this.displayRange = addSetting(new NumberSetting("displayRange", 500.0, 50.0, 5000.0));
        INSTANCE = this;
    }

    public static WaypointsModule getInstance() {
        return INSTANCE;
    }

    public Waypoint addWaypoint(String name, double x, double y, double z) {
        int color = WAYPOINT_COLORS[colorIndex % WAYPOINT_COLORS.length];
        colorIndex++;
        Waypoint wp = new Waypoint(name, x, y, z, color);
        waypoints.add(wp);
        return wp;
    }

    public boolean removeWaypoint(String name) {
        return waypoints.removeIf(w -> w.name.equalsIgnoreCase(name));
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void clearWaypoints() {
        waypoints.clear();
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || waypoints.isEmpty()) return;

        Vec3 playerPos = mc.player.position();
        double speed = mc.player.getDeltaMovement().horizontalDistance();

        // Sort by distance
        List<Waypoint> nearby = new ArrayList<>(waypoints);
        nearby.removeIf(w -> w.distanceTo(playerPos) > displayRange.getValue());
        nearby.sort((a, b) -> Double.compare(a.distanceTo(playerPos), b.distanceTo(playerPos)));

        int max = maxDisplay.asInt();
        int renderCount = Math.min(nearby.size(), max);

        int x = 5;
        int y = guiGraphics.guiHeight() / 2 - renderCount * 11;
        int rowH = 11;
        int cardW = 160;

        guiGraphics.fill(x - 2, y - 2, x + cardW + 2, y + renderCount * rowH + 2, 0x88000000);

        for (int i = 0; i < renderCount; i++) {
            Waypoint wp = nearby.get(i);
            double dist = wp.distanceTo(playerPos);

            // Bearing indicator
            double dx = wp.x - playerPos.x;
            double dz = wp.z - playerPos.z;
            float bearing = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90);
            float playerYaw = mc.player.getYRot();
            float relBearing = ((bearing - playerYaw) % 360 + 360) % 360;
            String arrow = bearingArrow(relBearing);

            String etaStr = "";
            if (showEta.isEnabled() && speed > 0.05) {
                double etaSec = dist / (speed * 20);
                if (etaSec < 60) etaStr = String.format(" (%.0fs)", etaSec);
                else etaStr = String.format(" (%.1fm)", etaSec / 60);
            }

            String label = arrow + " " + wp.name + " " + formatDist(dist) + etaStr;
            int textColor = 0xFF000000 | (wp.color & 0xFFFFFF);
            guiGraphics.text(mc.font, Component.literal(label), x, y + i * rowH, textColor, true);
        }
    }

    private String bearingArrow(float relBearing) {
        if (relBearing < 22.5 || relBearing >= 337.5) return "↑";
        if (relBearing < 67.5) return "↗";
        if (relBearing < 112.5) return "→";
        if (relBearing < 157.5) return "↘";
        if (relBearing < 202.5) return "↓";
        if (relBearing < 247.5) return "↙";
        if (relBearing < 292.5) return "←";
        return "↖";
    }

    private String formatDist(double dist) {
        if (dist < 100) return String.format("%.0fm", dist);
        return String.format("%.0fm", dist);
    }
}
