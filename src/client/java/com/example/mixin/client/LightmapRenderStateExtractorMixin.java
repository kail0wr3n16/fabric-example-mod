package com.example.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.module.FullbrightModule;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract", at = @At("TAIL"))
	private void clientmodules$applyFullbright(LightmapRenderState state, float tickDelta, CallbackInfo ci) {
		if (!FullbrightModule.isActive()) {
			return;
		}

		state.needsUpdate = true;
		state.blockFactor = 1.0F;
		state.skyFactor = 1.0F;
		state.blockLightTint = LightmapRenderStateExtractor.WHITE;
		state.skyLightColor = LightmapRenderStateExtractor.WHITE;
		state.ambientColor = LightmapRenderStateExtractor.WHITE;
		state.brightness = 1.0F;
		state.darknessEffectScale = 0.0F;
		state.nightVisionEffectIntensity = 0.0F;
		state.bossOverlayWorldDarkening = 0.0F;
	}
}