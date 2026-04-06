package com.example.module;

import com.example.module.setting.BooleanSetting;
import com.example.module.setting.NumberSetting;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TridentItem;

public class ProjectileTrajectoryModule extends Module {
    private final BooleanSetting showCharge;
    private final NumberSetting yPos;

    public ProjectileTrajectoryModule() {
        super("projectiletrajectory", ModuleCategory.RENDER, false);
        this.showCharge = addSetting(new BooleanSetting("showCharge", true));
        this.yPos = addSetting(new NumberSetting("yPos", 80.0, 5.0, 160.0));
    }

    @Override
    public void onHudRender(GuiGraphicsExtractor guiGraphics, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty() || !isSupportedProjectile(stack)) {
            stack = mc.player.getOffhandItem();
            if (stack.isEmpty() || !isSupportedProjectile(stack)) {
                return;
            }
        }

        String itemName = stack.getHoverName().getString();
        String line = itemName;

        if (showCharge.isEnabled() && mc.player.isUsingItem()) {
            line = line + " | charge " + formatCharge(mc.player.getTicksUsingItem());
        }

        int width = Math.max(110, mc.font.width(line) + 12);
        int x = 8;
        int y = yPos.getValue().intValue();

        guiGraphics.fill(x, y, x + width, y + 22, 0xAA0B1016);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF56C6FF);
        guiGraphics.text(mc.font, Component.literal("Projectile Trajectory"), x + 6, y + 4, 0xFFE8EEF8, true);
        guiGraphics.text(mc.font, Component.literal(line), x + 6, y + 14, 0xFFAAAAAA, true);
    }

    private boolean isSupportedProjectile(ItemStack stack) {
        return stack.getItem() instanceof BowItem
            || stack.getItem() instanceof CrossbowItem
            || stack.getItem() instanceof TridentItem
            || stack.getItem() instanceof SnowballItem
            || stack.getItem() instanceof EggItem
            || stack.getItem().getClass().getSimpleName().contains("EnderPearl");
    }

    private String formatCharge(int ticksUsed) {
        int percent = Math.min(100, (int) Math.round((ticksUsed / 20.0) * 100.0));
        return percent + "%";
    }
}