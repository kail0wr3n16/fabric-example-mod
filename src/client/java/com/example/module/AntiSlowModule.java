package com.example.module;

import com.example.module.setting.BooleanSetting;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public class AntiSlowModule extends Module {
    private static AntiSlowModule INSTANCE;

    private final BooleanSetting bowSlow;
    private final BooleanSetting eatingSlow;
    private final BooleanSetting shieldSlow;

    public AntiSlowModule() {
        super("antislow", ModuleCategory.MOVEMENT, false);
        this.bowSlow = addSetting(new BooleanSetting("bowSlow", true));
        this.eatingSlow = addSetting(new BooleanSetting("eatingSlow", true));
        this.shieldSlow = addSetting(new BooleanSetting("shieldSlow", false));
        INSTANCE = this;
    }

    public static AntiSlowModule getInstance() {
        return INSTANCE;
    }

    public boolean shouldCancelSlow() {
        return isEnabled();
    }

    public boolean shouldBypassSlowFor(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        if (bowSlow.isEnabled() && (item instanceof BowItem || item instanceof CrossbowItem)) {
            return true;
        }

        if (eatingSlow.isEnabled()) {
            String useAnim = stack.getUseAnimation().toString();
            if ("EAT".equals(useAnim) || "DRINK".equals(useAnim)) {
                return true;
            }
        }

        return shieldSlow.isEnabled() && item instanceof ShieldItem;
    }

    public boolean isBowSlowEnabled() { return bowSlow.isEnabled(); }
    public boolean isEatingSlowEnabled() { return eatingSlow.isEnabled(); }
    public boolean isShieldSlowEnabled() { return shieldSlow.isEnabled(); }
}
