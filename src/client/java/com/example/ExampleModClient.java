package com.example;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ExampleModClient implements ClientModInitializer {
	private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath("clientloaded", "overlay_text");
	private static boolean overlayEnabled = false;

	@Override
	public void onInitializeClient() {
		KeyMapping toggleOverlayKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping(
				"key.clientloaded.toggle_overlay",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath("clientloaded", "controls"))
			)
		);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleOverlayKey.consumeClick()) {
				overlayEnabled = !overlayEnabled;
				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal("Overlay " + (overlayEnabled ? "enabled" : "disabled")));
				}
			}
		});

		HudElementRegistry.addLast(OVERLAY_ID, (guiGraphics, tickCounter) -> {
			if (!overlayEnabled) {
				return;
			}

			Minecraft client = Minecraft.getInstance();
			Component overlayText = Component.literal("Overlay enabled");
			int x = 8;
			int y = 8;
			int padding = 3;
			int textWidth = client.font.width(overlayText);

			// Colors are ARGB in this version; include alpha so text is not transparent.
			guiGraphics.fill(x - padding, y - padding, x + textWidth + padding, y + 9 + padding, 0x90000000);
			guiGraphics.text(client.font, overlayText, x, y, 0xFFFFFFFF, true);
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(
				ClientCommands.literal("clientloaded")
					.executes(context -> {
						context.getSource().sendFeedback(Component.literal("Usage: /clientloaded ping"));
						return 1;
					})
					.then(
						ClientCommands.literal("ping")
							.executes(context -> {
								context.getSource().sendFeedback(Component.literal("Pong from clientloaded"));
								return 1;
							})
					)
			)
		);

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (client.player != null) {
				client.player.sendSystemMessage(Component.literal("Client loaded successfully"));
			}
		});
	}
}