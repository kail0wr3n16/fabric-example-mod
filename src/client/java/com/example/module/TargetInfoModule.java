package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TargetInfoModule extends Module {
    private final NumberSetting maxRange;
    private final BooleanSetting showArmor;
    private final BooleanSetting preferCrosshair;

    private LivingEntity currentTarget;
    private float displayAlpha = 0f;

    public TargetInfoModule() {
        super("targetinfo", ModuleCategory.COMBAT, false);
        this.maxRange = addSetting(new NumberSetting("maxRange", 8.0, 1.0, 20.0));
        this.showArmor = addSetting(new BooleanSetting("showArmor", true));
        this.preferCrosshair = addSetting(new BooleanSetting("preferCrosshair", true));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            currentTarget = null;
            return;
        }

        LivingEntity found = null;

        if (preferCrosshair.isEnabled() && mc.crosshairPickEntity instanceof LivingEntity le && !le.isDeadOrDying()) {
            found = le;
        }

        if (found == null) {
            double range = maxRange.getValue();
            double bestDist = range * range;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player || !(e instanceof LivingEntity le)) continue;
                if (le.isDeadOrDying()) continue;
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
                double d = mc.player.distanceToSqr(e);
                if (d < bestDist) {
                    bestDist = d;
                    found = le;
                }
            }
        }

        currentTarget = found;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float targetAlpha = (currentTarget != null) ? 1f : 0f;
        displayAlpha += (targetAlpha - displayAlpha) * 0.15f;

        if (displayAlpha < 0.02f) return;

        LivingEntity t = currentTarget;
        String name = t != null ? t.getName().getString() : "---";
        float hp = t != null ? t.getHealth() : 0;
        float maxHp = t != null ? t.getMaxHealth() : 20;
        double dist = t != null ? mc.player.distanceTo(t) : 0;
        int armor = t != null ? t.getArmorValue() : 0;

        int cardW = 130;
        int lineH = 10;
        int padding = 4;
        int cardH = padding * 2 + lineH * (showArmor.isEnabled() ? 4 : 3);
        int x = guiGraphics.guiWidth() / 2 - cardW / 2;
        int y = guiGraphics.guiHeight() - 60 - cardH;

        int alpha = (int)(displayAlpha * 180);
        int bg = (alpha << 24) | 0x0A0C10;
        int border = (alpha << 24) | 0x3A7FD4;
        int textAlpha = (int)(displayAlpha * 255);

        guiGraphics.fill(x, y, x + cardW, y + cardH, bg);
        guiGraphics.fill(x, y, x + cardW, y + 1, border);
        guiGraphics.fill(x, y + cardH - 1, x + cardW, y + cardH, border);
        guiGraphics.fill(x, y, x + 1, y + cardH, border);
        guiGraphics.fill(x + cardW - 1, y, x + cardW, y + cardH, border);

        int tx = x + padding;
        int ty = y + padding;
        int nameColor = (textAlpha << 24) | 0xE8EEF8;
        int hpColor = (textAlpha << 24) | 0x55FF55;
        int lowHpColor = (textAlpha << 24) | 0xFF5555;
        int infoColor = (textAlpha << 24) | 0xAAAAAA;

        guiGraphics.text(mc.font, Component.literal(name), tx, ty, nameColor, true);
        ty += lineH + 1;

        // HP bar
        float hpFraction = maxHp > 0 ? hp / maxHp : 0;
        int barW = cardW - padding * 2 - 36;
        int barH = 5;
        guiGraphics.fill(tx, ty, tx + barW, ty + barH, (alpha << 24) | 0x333333);
        int filled = (int)(barW * hpFraction);
        int barColor = hpFraction > 0.5f ? 0x55FF55 : (hpFraction > 0.25f ? 0xFFAA00 : 0xFF5555);
        if (filled > 0) guiGraphics.fill(tx, ty, tx + filled, ty + barH, (alpha << 24) | barColor);
        String hpStr = String.format("%.0f/%.0f", hp, maxHp);
        guiGraphics.text(mc.font, Component.literal(hpStr), tx + barW + 3, ty - 1, hp / maxHp > 0.3f ? hpColor : lowHpColor, true);
        ty += barH + 3;

        if (showArmor.isEnabled()) {
            guiGraphics.text(mc.font, Component.literal("Armor: " + armor), tx, ty, infoColor, true);
            ty += lineH;
        }

        guiGraphics.text(mc.font, Component.literal(String.format("Dist: %.1fm", dist)), tx, ty, infoColor, true);
    }
}
