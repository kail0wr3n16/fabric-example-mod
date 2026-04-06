package com.example.module;

import com.example.module.setting.NumberSetting;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class EnemyHighlightModule extends Module {
    private final NumberSetting maxRange;
    private LivingEntity highlightedTarget;

    public EnemyHighlightModule() {
        super("enemyhighlight", ModuleCategory.COMBAT, false);
        this.maxRange = addSetting(new NumberSetting("maxRange", 12.0, 2.0, 32.0));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            highlightedTarget = null;
            return;
        }

        LivingEntity best = null;
        double bestDistance = maxRange.getValue() * maxRange.getValue();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.isDeadOrDying()) {
                continue;
            }
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }

            double distance = mc.player.distanceToSqr(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }

        highlightedTarget = best;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || highlightedTarget == null) {
            return;
        }

        String name = highlightedTarget.getName().getString();
        double distance = mc.player.distanceTo(highlightedTarget);
        int width = Math.max(110, mc.font.width(name) + 24);
        int x = 8;
        int y = 44;

        guiGraphics.fill(x, y, x + width, y + 22, 0xAA14080A);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFFFF5555);
        guiGraphics.text(mc.font, Component.literal("Enemy: " + name), x + 6, y + 4, 0xFFE8EEF8, true);
        guiGraphics.text(mc.font, Component.literal(String.format("Dist: %.1fm", distance)), x + 6, y + 14, 0xFFAAAAAA, true);
    }
}