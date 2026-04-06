package com.example.mixin.client;

import com.example.module.FovMemoryModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;

/**
 * Hooks into Camera.calculateFov — the same injection point used by CameraMixin/ZoomModule.
 * When FovMemory is active and the player is sprinting or using an item,
 * we return the raw options FOV so the sprint/bow visual boost is neutralised.
 */
@Mixin(Camera.class)
public class FovMemoryMixin {
    @Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void clientmodules$fovMemory(float partialTick, CallbackInfoReturnable<Float> cir) {
        FovMemoryModule module = FovMemoryModule.getInstance();
        if (module == null || !module.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;

        boolean sprinting = mc.player.isSprinting() && module.isLockSprint();
        boolean usingItem  = mc.player.isUsingItem()  && module.isLockBow();
        if (!sprinting && !usingItem) return;

        // Return the player's configured FOV directly, bypassing any modifier
        cir.setReturnValue((float) (double) mc.options.fov().get());
    }
}
