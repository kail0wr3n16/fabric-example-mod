package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Adds extra information to item tooltips:
 * - Food items: hunger + saturation values
 * - Armor/tools: durability as a percentage
 */
public class ItemTooltipModule extends Module {
    private final BooleanSetting foodInfo;
    private final BooleanSetting durabilityPercent;
    private boolean registered = false;

    public ItemTooltipModule() {
        super("itemtooltip", ModuleCategory.QOL, true);
        this.foodInfo = addSetting(new BooleanSetting("foodInfo", true));
        this.durabilityPercent = addSetting(new BooleanSetting("durabilityPercent", true));
    }

    @Override
    protected void onEnable() {
        if (!registered) {
            ItemTooltipCallback.EVENT.register(this::onTooltip);
            registered = true;
        }
    }

    private void onTooltip(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                           net.minecraft.world.item.TooltipFlag type,
                           java.util.List<net.minecraft.network.chat.Component> lines) {
        if (!isEnabled()) return;

        if (foodInfo.isEnabled()) {
            FoodProperties food = stack.getItem().components()
                .get(net.minecraft.core.component.DataComponents.FOOD);
            if (food != null) {
                lines.add(net.minecraft.network.chat.Component.literal(
                    String.format("  Hunger: +%d  Sat: +%.1f",
                        food.nutrition(), food.saturation())
                ).withStyle(s -> s.withColor(0xFF88BB44)));
            }
        }

        if (durabilityPercent.isEnabled() && stack.isDamageableItem()) {
            int maxDur = stack.getMaxDamage();
            int curDur = maxDur - stack.getDamageValue();
            int pct = (int) (100.0 * curDur / maxDur);
            int color = pct > 66 ? 0xFF55FF55 : pct > 33 ? 0xFFFFFF55 : 0xFFFF5555;
            lines.add(net.minecraft.network.chat.Component.literal(
                String.format("  Durability: %d%% (%d/%d)", pct, curDur, maxDur)
            ).withStyle(s -> s.withColor(color)));
        }
    }
}
