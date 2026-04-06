package com.example.mixin.client;

import com.example.module.CustomCrosshairModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the vanilla crosshair render when CustomCrosshairModule is active,
 * so only the custom one is visible.
 */
@Mixin(Gui.class)
public class CrosshairMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void clientmodules$suppressVanillaCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        CustomCrosshairModule module = CustomCrosshairModule.getInstance();
        if (module != null && module.isEnabled()) {
            ci.cancel();
        }
    }
}
