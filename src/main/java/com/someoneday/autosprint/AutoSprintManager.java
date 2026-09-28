package com.someoneday.autosprint;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AutoSprintManager {
	private static final Path CONFIG_PATH =
			FabricLoader.getInstance().getConfigDir().resolve("autosprint.properties");

	private static boolean enabled = true;

	private AutoSprintManager() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static boolean toggle() {
		enabled = !enabled;
		save();
		return enabled;
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			save();
			return;
		}

		Properties props = new Properties();
		try (var in = Files.newInputStream(CONFIG_PATH)) {
			props.load(in);
			enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
		} catch (IOException e) {
			System.err.println("[AutoSprint] " + e.getMessage());
		}
	}

	private static void save() {
		Properties props = new Properties();
		props.setProperty("enabled", Boolean.toString(enabled));

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (var out = Files.newOutputStream(CONFIG_PATH)) {
				props.store(out, "AutoSprint config");
			}
		} catch (IOException e) {
			System.err.println("[AutoSprint] " + e.getMessage());
		}
	}

	public static void tick(Minecraft client) {
		if (!enabled) {
			return;
		}

		LocalPlayer player = client.player;
		if (player == null || player.input == null) {
			return;
		}

		if (!shouldMoveForward(client, player)) {
			return;
		}

		if (!canSprint(player)) {
			return;
		}

		player.setSprinting(true);
	}

	private static boolean shouldMoveForward(Minecraft client, LocalPlayer player) {
		if (client.options.keyUp.isDown()) {
			return true;
		}
		return player.input.forwardImpulse > 0.0F;
	}

	private static boolean canSprint(LocalPlayer player) {
		if (player.isCrouching() || player.isFallFlying() || player.isPassenger()) {
			return false;
		}
		if (player.isUsingItem() && !player.isPassenger()) {
			return false;
		}
		if (player.hasEffect(MobEffects.BLINDNESS)) {
			return false;
		}
		if (player.getFoodData().getFoodLevel() <= 6 && !player.getAbilities().flying) {
			return false;
		}
		return true;
	}
}
