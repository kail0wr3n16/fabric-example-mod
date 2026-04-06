package com.example.module;

import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public class AutoTotemReminderModule extends Module {
    private final NumberSetting warnThreshold;
    private int totemCount = 0;
    private float flashTimer = 0f;

    public AutoTotemReminderModule() {
        super("totemreminder", ModuleCategory.COMBAT, false);
        this.warnThreshold = addSetting(new NumberSetting("warnThreshold", 3.0, 1.0, 16.0));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                count += mc.player.getInventory().getItem(i).getCount();
            }
        }
        totemCount = count;
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (totemCount >= warnThreshold.asInt()) return;

        flashTimer += tickCounter.getGameTimeDeltaPartialTick(true) * 0.08f;
        float pulse = (float)((Math.sin(flashTimer * Math.PI * 2) + 1.0) / 2.0);
        int alpha = (int)(150 + pulse * 105);

        String msg;
        int color;
        if (totemCount == 0) {
            msg = "NO TOTEMS!";
            color = (alpha << 24) | 0xFF3333;
        } else {
            msg = "Totems: " + totemCount + " (LOW)";
            color = (alpha << 24) | 0xFFAA00;
        }

        int sw = guiGraphics.guiWidth();
        int textW = mc.font.width(msg);
        int x = sw / 2 - textW / 2;
        int y = guiGraphics.guiHeight() / 2 + 30;

        guiGraphics.fill(x - 4, y - 2, x + textW + 4, y + 10, (80 << 24) | 0x000000);
        guiGraphics.text(mc.font, Component.literal(msg), x, y, color, true);
    }
}
