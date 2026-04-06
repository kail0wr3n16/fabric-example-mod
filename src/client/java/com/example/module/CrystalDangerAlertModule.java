package com.example.module;

import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.ArrayList;
import java.util.List;

public class CrystalDangerAlertModule extends Module {
    private final NumberSetting dangerRange;
    private final NumberSetting tntRange;

    private int dangerLevel = 0; // 0=safe, 1=crystal nearby, 2=very close
    private float flashTimer = 0f;

    public CrystalDangerAlertModule() {
        super("crystaldanger", ModuleCategory.HUD, false);
        this.dangerRange = addSetting(new NumberSetting("dangerRange", 6.0, 2.0, 15.0));
        this.tntRange = addSetting(new NumberSetting("tntRange", 8.0, 2.0, 20.0));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            dangerLevel = 0;
            return;
        }

        double crystalRange = dangerRange.getValue();
        double tntScanRange = tntRange.getValue();
        int level = 0;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue;
            double dist = mc.player.distanceTo(e);

            // End crystals
            if (e instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal) {
                if (dist < crystalRange / 2.0) {
                    level = Math.max(level, 2);
                } else if (dist < crystalRange) {
                    level = Math.max(level, 1);
                }
            }

            // TNT
            if (e instanceof PrimedTnt && dist < tntScanRange) {
                level = Math.max(level, 2);
            }
        }

        dangerLevel = level;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        if (dangerLevel == 0) {
            flashTimer = 0f;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        flashTimer += tickCounter.getGameTimeDeltaPartialTick(true) * (dangerLevel == 2 ? 0.18f : 0.08f);
        float pulse = (float)((Math.sin(flashTimer * Math.PI * 2) + 1.0) / 2.0);
        int alpha = (int)(120 + pulse * 135);

        String msg = dangerLevel == 2 ? "CRYSTAL DANGER!" : "Crystal Nearby";
        int color = dangerLevel == 2 ? ((alpha << 24) | 0xFF3333) : ((alpha << 24) | 0xFFAA00);

        int sw = guiGraphics.guiWidth();
        int sh = guiGraphics.guiHeight();
        int textW = mc.font.width(msg);
        int x = sw / 2 - textW / 2;
        int y = sh / 2 - 40;

        guiGraphics.fill(x - 5, y - 3, x + textW + 5, y + 11, (90 << 24));
        guiGraphics.text(mc.font, Component.literal(msg), x, y, color, true);

        // Edge vignette on danger level 2
        if (dangerLevel == 2) {
            int edgeAlpha = (int)(pulse * 60);
            int edgeColor = (edgeAlpha << 24) | 0xFF0000;
            guiGraphics.fill(0, 0, sw, 3, edgeColor);
            guiGraphics.fill(0, sh - 3, sw, sh, edgeColor);
            guiGraphics.fill(0, 0, 3, sh, edgeColor);
            guiGraphics.fill(sw - 3, 0, sw, sh, edgeColor);
        }
    }
}
