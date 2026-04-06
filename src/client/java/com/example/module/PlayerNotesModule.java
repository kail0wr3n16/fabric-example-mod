package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Shows a nearby player's name and distance as a subtle HUD label.
 * Only highlights other players — no mob detection, no "Enemy" framing.
 */
public class PlayerNotesModule extends Module {
    private final NumberSetting maxRange;
    private final BooleanSetting showDistance;
    private Player nearestPlayer;

    public PlayerNotesModule() {
        super("playernotes", ModuleCategory.SOCIAL, false);
        this.maxRange = addSetting(new NumberSetting("maxRange", 16.0, 4.0, 64.0));
        this.showDistance = addSetting(new BooleanSetting("showDistance", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            nearestPlayer = null;
            return;
        }

        Player best = null;
        double bestDistSq = maxRange.getValue() * maxRange.getValue();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !(entity instanceof Player other)) continue;
            if (other.isCreative() || other.isSpectator()) continue;

            double dist = mc.player.distanceToSqr(entity);
            if (dist < bestDistSq) {
                bestDistSq = dist;
                best = other;
            }
        }

        nearestPlayer = best;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || nearestPlayer == null) return;

        String name = nearestPlayer.getName().getString();
        double distance = mc.player.distanceTo(nearestPlayer);

        String line1 = name;
        String line2 = showDistance.isEnabled() ? String.format("%.1fm away", distance) : "";

        int labelWidth = Math.max(mc.font.width(line1), mc.font.width(line2)) + 16;
        int labelHeight = showDistance.isEnabled() ? 24 : 14;
        int x = 8;
        int y = 44;

        // Subtle background panel
        guiGraphics.fill(x, y, x + labelWidth, y + labelHeight, 0xAA0D1117);
        guiGraphics.fill(x, y, x + 2, y + labelHeight, 0xFF54C5FF); // left accent bar

        guiGraphics.text(mc.font, Component.literal(line1), x + 6, y + 3, 0xFFE8EEF8, true);
        if (showDistance.isEnabled()) {
            guiGraphics.text(mc.font, Component.literal(line2), x + 6, y + 13, 0xFF8AAABB, false);
        }
    }

    @Override
    protected void onDisable() {
        nearestPlayer = null;
    }
}
