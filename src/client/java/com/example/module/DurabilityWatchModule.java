package com.example.module;

import com.example.module.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DurabilityWatchModule extends Module {
    private final NumberSetting warnPercent;

    private final List<String> warnings = new ArrayList<>();
    private int scanTick = 0;

    public DurabilityWatchModule() {
        super("durabilitywatch", ModuleCategory.HUD, false);
        this.warnPercent = addSetting(new NumberSetting("warnPercent", 15.0, 5.0, 50.0));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;
        scanTick++;
        if (scanTick < 10) return;
        scanTick = 0;

        warnings.clear();
        double threshold = warnPercent.getValue() / 100.0;
        String[] armorSlotNames = {"Helmet", "Chest", "Legs", "Boots"};

        int inventorySize = mc.player.getInventory().getContainerSize();
        int startIndex = Math.max(0, inventorySize - 4);
        for (int i = 0; i < 4; i++) {
            ItemStack armor = mc.player.getInventory().getItem(startIndex + i);
            checkDurability(armor, armorSlotNames[i], threshold);
        }

        // Check held item
        ItemStack held = mc.player.getMainHandItem();
        checkDurability(held, "Tool", threshold);
    }

    private void checkDurability(ItemStack stack, String label, double threshold) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        int maxDur = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        double remaining = 1.0 - (double) damage / maxDur;
        if (remaining <= threshold) {
            int pct = (int)(remaining * 100);
            warnings.add(label + ": " + pct + "%");
        }
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        if (warnings.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();

        int x = guiGraphics.guiWidth() / 2 + 10;
        int y = guiGraphics.guiHeight() - 30 - warnings.size() * 10;
        int lineH = 10;

        for (String warning : warnings) {
            int color = warning.contains(": 0%") || warning.contains(": 1%") || warning.contains(": 2%")
                ? 0xFFFF5555 : 0xFFFFAA00;
            guiGraphics.text(mc.font, Component.literal("⚠ " + warning), x, y, color, true);
            y += lineH;
        }
    }
}
