package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
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