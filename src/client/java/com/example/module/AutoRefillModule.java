package com.example.module;

import com.example.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class AutoRefillModule extends Module {
    private final NumberSetting refillThreshold;
    private int cooldown = 0;

    public AutoRefillModule() {
        super("autorefill", ModuleCategory.QOL, false);
        this.refillThreshold = addSetting(new NumberSetting("refillThreshold", 8.0, 1.0, 32.0));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return;
        if (cooldown > 0) { cooldown--; return; }

        Inventory inv = mc.player.getInventory();
        int threshold = refillThreshold.asInt();

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack hotbarStack = inv.getItem(hotbarSlot);
            if (hotbarStack.isEmpty()) continue;
            if (!hotbarStack.isStackable()) continue;
            if (hotbarStack.getCount() >= threshold) continue;
            if (hotbarStack.getCount() >= hotbarStack.getMaxStackSize()) continue;

            // Look for more of the same item in main inventory (slots 9-35)
            for (int invSlot = 9; invSlot < 36; invSlot++) {
                ItemStack invStack = inv.getItem(invSlot);
                if (invStack.isEmpty()) continue;
                if (!ItemStack.isSameItemSameComponents(hotbarStack, invStack)) continue;

                // Move items from invSlot to hotbarSlot
                int canMove = Math.min(invStack.getCount(),
                    hotbarStack.getMaxStackSize() - hotbarStack.getCount());
                if (canMove <= 0) continue;

                hotbarStack.grow(canMove);
                invStack.shrink(canMove);
                if (invStack.isEmpty()) inv.setItem(invSlot, ItemStack.EMPTY);

                cooldown = 4; // Small delay between refills
                return; // One refill per tick
            }
        }
    }
}
