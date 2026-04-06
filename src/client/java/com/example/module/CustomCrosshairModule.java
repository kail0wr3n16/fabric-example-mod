package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class CustomCrosshairModule extends Module {
    private static CustomCrosshairModule INSTANCE;

    private final NumberSetting baseSize;
    private final NumberSetting thickness;
    private final NumberSetting gap;
    private final BooleanSetting dynamicSpread;
    private final BooleanSetting dotCenter;

    public CustomCrosshairModule() {
        super("customcrosshair", ModuleCategory.RENDER, false);
        this.baseSize = addSetting(new NumberSetting("baseSize", 5.0, 2.0, 15.0));
        this.thickness = addSetting(new NumberSetting("thickness", 1.0, 1.0, 3.0));
        this.gap = addSetting(new NumberSetting("gap", 3.0, 0.0, 8.0));
        this.dynamicSpread = addSetting(new BooleanSetting("dynamicSpread", true));
        this.dotCenter = addSetting(new BooleanSetting("dotCenter", true));
        INSTANCE = this;
    }

    public static CustomCrosshairModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return;
        if (mc.screen != null) return;

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int cx = sw / 2;
        int cy = sh / 2;

        double spread = 0;
        if (dynamicSpread.isEnabled()) {
            double speed = mc.player.getDeltaMovement().horizontalDistance();
            double cooldown = 1.0 - mc.player.getAttackStrengthScale(0.5f);
            spread = speed * 8.0 + cooldown * 3.0;
        }

        int size = baseSize.asInt();
        int t = thickness.asInt();
        int g = (int)(gap.getValue() + spread);
        int color = 0xFFFFFFFF;
        int shadow = 0x66000000;

        // Horizontal left arm (with 1px drop shadow)
        guiGraphics.fill(cx - size - g + 1, cy - t / 2 + 1, cx - g + 1, cy + t / 2 + 1, shadow);
        guiGraphics.fill(cx - size - g, cy - t / 2, cx - g, cy + t / 2, color);

        // Horizontal right arm
        guiGraphics.fill(cx + g + 1, cy - t / 2 + 1, cx + size + g + 1, cy + t / 2 + 1, shadow);
        guiGraphics.fill(cx + g, cy - t / 2, cx + size + g, cy + t / 2, color);

        // Vertical top arm
        guiGraphics.fill(cx - t / 2 + 1, cy - size - g + 1, cx + t / 2 + 1, cy - g + 1, shadow);
        guiGraphics.fill(cx - t / 2, cy - size - g, cx + t / 2, cy - g, color);

        // Vertical bottom arm
        guiGraphics.fill(cx - t / 2 + 1, cy + g + 1, cx + t / 2 + 1, cy + size + g + 1, shadow);
        guiGraphics.fill(cx - t / 2, cy + g, cx + t / 2, cy + size + g, color);

        if (dotCenter.isEnabled()) {
            guiGraphics.fill(cx, cy, cx + 1, cy + 1, color);
        }
    }
}
