package com.example.mixin.client;

import com.example.module.SafeWalkModule;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

@Mixin(LocalPlayer.class)
public class SafeWalkMixin {
    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void clientmodules$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        SafeWalkModule module = SafeWalkModule.getInstance();
        if (module == null || !module.isEnabled()) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!player.onGround() || player.isShiftKeyDown()) {
            return;
        }

        if (isOverEdge(player)) {
            cir.setReturnValue(true);
        }
    }

    private boolean isOverEdge(LocalPlayer player) {
        double y = player.getY() - 0.05;
        double offset = 0.30;
        return isAirBelow(player, player.getX() - offset, y, player.getZ() - offset)
            || isAirBelow(player, player.getX() - offset, y, player.getZ() + offset)
            || isAirBelow(player, player.getX() + offset, y, player.getZ() - offset)
            || isAirBelow(player, player.getX() + offset, y, player.getZ() + offset);
    }

    private boolean isAirBelow(LocalPlayer player, double x, double y, double z) {
        return player.level().getBlockState(BlockPos.containing(x, y, z)).isAir();
    }
}