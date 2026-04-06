package com.example.mixin.client;

import com.example.module.FastPlaceModule;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class FastPlaceMixin {
    @Shadow
    private int rightClickDelay;

    @Inject(method = "tick", at = @At("TAIL"))
    private void clientloaded$fastPlace(CallbackInfo ci) {
        FastPlaceModule module = FastPlaceModule.getInstance();
        if (module == null || !module.isEnabled()) {
            return;
        }

        rightClickDelay = 0;
    }
}