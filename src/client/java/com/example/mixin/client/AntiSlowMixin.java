package com.example.mixin.client;

import com.example.module.AntiSlowModule;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
public class AntiSlowMixin {
    @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
    private void clientmodules$antiSlowState(CallbackInfoReturnable<Boolean> cir) {
        AntiSlowModule module = AntiSlowModule.getInstance();
        if (module == null || !module.isEnabled()) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        if (module.shouldBypassSlowFor(player.getUseItem())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "itemUseSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void clientmodules$antiSlowMultiplier(CallbackInfoReturnable<Float> cir) {
        AntiSlowModule module = AntiSlowModule.getInstance();
        if (module == null || !module.isEnabled()) {
            return;
        }

        LocalPlayer player = (LocalPlayer) (Object) this;
        if (module.shouldBypassSlowFor(player.getUseItem())) {
            cir.setReturnValue(1.0f);
        }
    }
}