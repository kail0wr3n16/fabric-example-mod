package com.example.mixin.client;

import com.example.module.InventoryMoveModule;
import com.mojang.blaze3d.platform.InputConstants;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(KeyboardHandler.class)
public class InventoryMoveMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "tick", at = @At("TAIL"))
    private void clientmodules$inventoryMove(CallbackInfo ci) {
        InventoryMoveModule module = InventoryMoveModule.getInstance();
        if (module == null || !module.isEnabled() || !(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            return;
        }

        updateKey(minecraft.options.keyUp);
        updateKey(minecraft.options.keyDown);
        updateKey(minecraft.options.keyLeft);
        updateKey(minecraft.options.keyRight);
        updateKey(minecraft.options.keyJump);
        updateKey(minecraft.options.keyShift);
        updateKey(minecraft.options.keySprint);
    }

    private void updateKey(KeyMapping mapping) {
        InputConstants.Key key = ((KeyMappingAccessor) mapping).clientmodules$getKey();
        if (key == null) {
            return;
        }

        mapping.setDown(InputConstants.isKeyDown(minecraft.getWindow(), key.getValue()));
    }
}