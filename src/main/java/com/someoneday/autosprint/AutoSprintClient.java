package com.someoneday.autosprint;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

public class AutoSprintClient implements ClientModInitializer {
	public static final String MOD_ID = "autosprint";

	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		AutoSprintManager.load();

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.autosprint.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_N,
				"category.autosprint"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				boolean enabled = AutoSprintManager.toggle();
				if (client.player != null) {
					client.player.displayClientMessage(
							new TranslatableComponent(enabled
									? "message.autosprint.enabled"
									: "message.autosprint.disabled"),
							true
					);
				}
			}

			AutoSprintManager.tick(client);
		});
	}
}
