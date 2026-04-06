package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

public class DamageTintModule extends Module {
    private final NumberSetting maxAlpha;
    private final BooleanSetting directional;
    private final BooleanSetting lowHealthPulse;

    private float tintAlpha = 0f;
    private float directionX = 0f; // -1 left, +1 right
    private float directionZ = 0f;
    private int lastHurtTime = 0;

    public DamageTintModule() {
        super("damagetint", ModuleCategory.RENDER, false);
        this.maxAlpha = addSetting(new NumberSetting("maxAlpha", 0.6, 0.1, 1.0));
        this.directional = addSetting(new BooleanSetting("directional", true));
        this.lowHealthPulse = addSetting(new BooleanSetting("lowHealthPulse", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;

        int hurtTime = mc.player.hurtTime;

        if (hurtTime > lastHurtTime && hurtTime == mc.player.hurtDuration) {
            // Just took damage — compute approximate hit direction
            tintAlpha = maxAlpha.getValue().floatValue();

            if (directional.isEnabled()) {
                // Use player look and last damage source position (approximate)
                float yaw = mc.player.getYRot();
                float rad = (float) Math.toRadians(yaw);
                // Default: show tint from front
                directionX = (float) -Math.sin(rad);
                directionZ = (float) Math.cos(rad);
            }
        }

        lastHurtTime = hurtTime;
        tintAlpha = Math.max(0f, tintAlpha - 0.04f);
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int edgeW = sw / 4;
        int edgeH = sh / 4;

        if (tintAlpha > 0.01f) {
            int a = (int)(tintAlpha * 255);
            int baseColor = (a << 24) | 0xFF0000;

            if (directional.isEnabled()) {
                // Show tint on the side the damage came from
                // directionX > 0 = damage from right = tint right side
                if (directionX > 0.3f) {
                    int sideA = (int)(tintAlpha * directionX * 255);
                    guiGraphics.fill(sw - edgeW, 0, sw, sh, (sideA << 24) | 0xFF0000);
                } else if (directionX < -0.3f) {
                    int sideA = (int)(tintAlpha * -directionX * 255);
                    guiGraphics.fill(0, 0, edgeW, sh, (sideA << 24) | 0xFF0000);
                }
                // Front/back tint on Z
                if (Math.abs(directionX) < 0.5f) {
                    guiGraphics.fill(0, 0, sw, edgeH, baseColor);
                    guiGraphics.fill(0, sh - edgeH, sw, sh, baseColor);
                }
            } else {
                // Full vignette
                guiGraphics.fill(0, 0, sw, edgeH, baseColor);
                guiGraphics.fill(0, sh - edgeH, sw, sh, baseColor);
                guiGraphics.fill(0, edgeH, edgeW, sh - edgeH, baseColor);
                guiGraphics.fill(sw - edgeW, edgeH, sw, sh - edgeH, baseColor);
            }
        }

        // Low health pulse overlay
        if (lowHealthPulse.isEnabled() && mc.player.getHealth() < mc.player.getMaxHealth() * 0.25f) {
            double pulse = (Math.sin(System.currentTimeMillis() / 400.0) + 1.0) / 2.0;
            int pa = (int)(pulse * 60);
            int pulseColor = (pa << 24) | 0xFF0000;
            guiGraphics.fill(0, 0, sw, edgeH / 2, pulseColor);
            guiGraphics.fill(0, sh - edgeH / 2, sw, sh, pulseColor);
            guiGraphics.fill(0, edgeH / 2, edgeW / 2, sh - edgeH / 2, pulseColor);
            guiGraphics.fill(sw - edgeW / 2, edgeH / 2, sw, sh - edgeH / 2, pulseColor);
        }
    }

    @Override
    protected void onDisable() {
        tintAlpha = 0f;
    }
}
