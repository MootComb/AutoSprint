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
		// When the mod is disabled, do nothing at all.
		// Do not touch sprint state — let the player sprint manually.
		if (!enabled) {
			return;
		}

		LocalPlayer player = client.player;
		if (player == null || player.input == null) {
			return;
		}

		// Always set exactly what it should be, not just force true.
		player.setSprinting(shouldSprint(player));
	}

	/**
	 * Legit sprint conditions for 1.17.1.
	 * Mirrors what LocalPlayer itself does so we don't fight vanilla's sprint reset logic.
	 * Sprint is allowed in creative and spectator (flying included), so the only
	 * exclusions are things that genuinely break movement.
	 */
	private static boolean shouldSprint(LocalPlayer player) {
		// Player must be moving forward (zza), not sideways/backwards.
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

		// Elytra gliding — no sprint (this is not the same as creative flight).
		if (player.isFallFlying()) {
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

		// Vanilla 1.17.1 mobility restriction checks:
		// blindness effect blocks sprinting.
		if (player.hasEffect(MobEffects.BLINDNESS)) {
			return false;
		}

		// Hunger check — skipped in creative and spectator, since those modes
		// don't consume food and the player should still be able to sprint.
		if (!player.isCreative() && !player.isSpectator()
				&& player.getFoodData().getFoodLevel() <= 6) {
			return false;
		}

		// Hit a wall — vanilla resets sprint.
		if (player.horizontalCollision) {
			return false;
		}

		return true;
	}
}
