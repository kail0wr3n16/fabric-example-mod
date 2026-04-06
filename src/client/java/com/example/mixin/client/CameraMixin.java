package com.example.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.module.ZoomModule;

import net.minecraft.client.Camera;

@Mixin(Camera.class)
public class CameraMixin {
	@Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
	private void clientmodules$applyZoomFov(float partialTick, CallbackInfoReturnable<Float> cir) {
		ZoomModule zoomModule = ZoomModule.getInstance();
		if (zoomModule == null) {
			return;
		}

		double baseFov = cir.getReturnValueF();
		cir.setReturnValue((float) zoomModule.applyRenderFov(baseFov));
	}
}
