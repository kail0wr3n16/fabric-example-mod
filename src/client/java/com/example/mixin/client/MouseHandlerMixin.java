package com.example.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.module.ZoomModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
	private void clientmodules$zoomScroll(long windowPointer, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
		if (minecraft.screen != null) {
			return;
		}

		ZoomModule zoomModule = ZoomModule.getInstance();
		if (zoomModule == null) {
			return;
		}

		if (zoomModule.adjustZoomFromScroll(verticalAmount)) {
			ci.cancel();
		}
	}
}
