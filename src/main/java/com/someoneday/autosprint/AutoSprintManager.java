package com.someoneday.autosprint;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AutoSprintManager {
	private static final Path CONFIG_PATH =
			FabricLoader.getInstance().getConfigDir().resolve("autosprint.properties");

	/** Forward impulse threshold. Vanilla considers sprint valid only when moving forward. */
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
		LocalPlayer player = client.player;
		if (player == null || player.input == null) {
			return;
		}

		// Always set exactly what it should be, not just force true.
		player.setSprinting(enabled && shouldSprint(player));
	}

	/**
	 * Legit sprint conditions. Mirrors what LocalPlayer itself does
	 * so we don't fight vanilla's sprint reset logic.
	 */
	private static boolean shouldSprint(LocalPlayer player) {
		// Player must be moving forward (zza), not sideways/backwards.
		// forwardImpulse in 1.17 is the equivalent of zza.
		if (player.input.forwardImpulse <= FORWARD_THRESHOLD) {
			return false;
		}

		// Sneaking — no sprint.
		if (player.isCrouching()) {
			return false;
		}

		// Passenger — let the vehicle decide.
		if (player.isPassenger()) {
			return false;
		}

		// Elytra / flight — no sprint.
		if (player.isFallFlying() || player.getAbilities().flying) {
			return false;
		}

		// Using an item (eating, bow, shield) slows you down — sprint is reset.
		if (player.isUsingItem()) {
			return false;
		}

		// Water/lava — vanilla behaves differently, legit mode just disables sprint.
		if (player.isInWater() || player.isInLava()) {
			return false;
		}

		// Vanilla gates: hunger, effects (blindness, etc.).
		if (player.isMobilityRestricted()) {
			return false;
		}

		// Hit a wall — vanilla resets sprint.
		if (player.horizontalCollision) {
			return false;
		}

		return true;
	}
}
