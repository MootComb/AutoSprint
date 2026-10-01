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

	private static final float FORWARD_THRESHOLD = 0.8F;

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

		if (player.isInWater()) {
			return;
		}

		player.setSprinting(shouldSprint(player));
	}

	private static boolean shouldSprint(LocalPlayer player) {
		if (player.input.forwardImpulse <= FORWARD_THRESHOLD) {
			return false;
		}

		if (player.isCrouching()) {
			return false;
		}

		if (player.isPassenger()) {
			return false;
		}

		if (player.isFallFlying()) {
			return false;
		}

		if (player.isUsingItem()) {
			return false;
		}

		if (player.isInLava()) {
			return false;
		}

		if (player.hasEffect(MobEffects.BLINDNESS)) {
			return false;
		}

		if (!player.isCreative() && !player.isSpectator()
				&& player.getFoodData().getFoodLevel() <= 6) {
			return false;
		}

		if (player.horizontalCollision) {
			return false;
		}

		return true;
	}
}
