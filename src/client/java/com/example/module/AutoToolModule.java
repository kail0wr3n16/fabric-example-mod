package com.example.module;

import com.example.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Field;

public class AutoToolModule extends Module {
    private final BooleanSetting ignoreDurability;
    private int lastSelectedSlot = -1;

    public AutoToolModule() {
        super("autotool", ModuleCategory.QOL, false);
        this.ignoreDurability = addSetting(new BooleanSetting("ignoreDurability", false));
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return;
        if (blockHit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        int bestSlot = findBestToolSlot(mc, state);
        if (bestSlot >= 0 && bestSlot != getSelectedSlot(mc)) {
            setSelectedSlot(mc, bestSlot);
        }
    }

    private int findBestToolSlot(Minecraft mc, BlockState state) {
        float bestSpeed = 1.0f;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            // Skip if nearly broken and ignoreDurability is off
            if (!ignoreDurability.isEnabled() && stack.isDamageableItem()
                && stack.getDamageValue() >= stack.getMaxDamage() - 2) continue;

            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private int getSelectedSlot(Minecraft mc) {
        try {
            Field selectedField = mc.player.getInventory().getClass().getDeclaredField("selected");
            selectedField.setAccessible(true);
            return selectedField.getInt(mc.player.getInventory());
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    private void setSelectedSlot(Minecraft mc, int slot) {
        try {
            Field selectedField = mc.player.getInventory().getClass().getDeclaredField("selected");
            selectedField.setAccessible(true);
            selectedField.setInt(mc.player.getInventory(), slot);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
