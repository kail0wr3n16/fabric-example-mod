package com.example.module;

import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public class TrailModule extends Module {
    private static final int MAX_POINTS = 300;

    private final NumberSetting recordInterval;
    private final NumberSetting mapScale;

    private final Deque<Vec3> trail = new ArrayDeque<>();
    private Vec3 lastRecorded = null;
    private int ticksSince = 0;

    public TrailModule() {
        super("trail", ModuleCategory.RENDER, false);
        this.recordInterval = addSetting(new NumberSetting("recordInterval", 2.0, 1.0, 10.0));
        this.mapScale = addSetting(new NumberSetting("mapScale", 2.0, 0.5, 8.0));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;

        ticksSince++;
        if (ticksSince < recordInterval.asInt()) return;
        ticksSince = 0;

        Vec3 pos = mc.player.position();
        if (lastRecorded == null || pos.distanceTo(lastRecorded) > 0.5) {
            trail.addLast(pos);
            if (trail.size() > MAX_POINTS) trail.removeFirst();
            lastRecorded = pos;
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || trail.size() < 2) return;

        Vec3 playerPos = mc.player.position();
        double scale = mapScale.getValue();

        // Minimap in top-right corner
        int mapSize = 80;
        int mx = guiGraphics.guiWidth() - mapSize - 8;
        int my = 8;
        int cx = mx + mapSize / 2;
        int cy = my + mapSize / 2;

        // Background
        guiGraphics.fill(mx, my, mx + mapSize, my + mapSize, 0xAA0A0C10);
        guiGraphics.fill(mx, my, mx + mapSize, my + 1, 0xFF3A7FD4);
        guiGraphics.fill(mx, my + mapSize - 1, mx + mapSize, my + mapSize, 0xFF3A7FD4);
        guiGraphics.fill(mx, my, mx + 1, my + mapSize, 0xFF3A7FD4);
        guiGraphics.fill(mx + mapSize - 1, my, mx + mapSize, my + mapSize, 0xFF3A7FD4);

        // Draw trail dots
        Vec3[] points = trail.toArray(new Vec3[0]);
        float playerYaw = mc.player.getYRot();
        double cosYaw = Math.cos(Math.toRadians(playerYaw));
        double sinYaw = Math.sin(Math.toRadians(playerYaw));

        for (int i = 0; i < points.length; i++) {
            double dx = points[i].x - playerPos.x;
            double dz = points[i].z - playerPos.z;

            // Rotate relative to player facing
            double rx = (dx * cosYaw + dz * sinYaw) * scale;
            double rz = (-dx * sinYaw + dz * cosYaw) * scale;

            int px = cx + (int)rx;
            int pz = cy - (int)rz;  // negate: forward (+Z world) = up on screen

            if (px < mx + 2 || px >= mx + mapSize - 2 || pz < my + 2 || pz >= my + mapSize - 2) continue;

            float age = (float)i / points.length;
            int alpha = (int)(80 + age * 175);
            int color = (alpha << 24) | 0x55CCFF;
            guiGraphics.fill(px, pz, px + 1, pz + 1, color);
        }

        // Player dot (center)
        guiGraphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFF55);

        // Label
        guiGraphics.text(mc.font, net.minecraft.network.chat.Component.literal("Trail"), mx + 2, my + 2, 0xAAFFFFFF, false);
    }

    @Override
    protected void onDisable() {
        trail.clear();
        lastRecorded = null;
        ticksSince = 0;
    }
}
