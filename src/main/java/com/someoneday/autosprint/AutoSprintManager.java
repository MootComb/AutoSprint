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
		// When disabled, do nothing at all — let vanilla handle sprinting.
		if (!enabled) {
			return;
		}

		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}

		// Vanilla handles these states itself. Don't interfere.
		// Water: vanilla enables swim-sprint; forcing it here breaks swimming.
		if (player.isInWater()) {
			return;
		}
		if (player.isFallFlying()) {
			return;
		}
		if (player.isPassenger()) {
			return;
		}

		// Only ever push sprint ON. Never force it OFF.
		// Vanilla resets sprint itself on wall hit, hunger, item use, sneak, etc.
		// Writing `false` every tick is what causes FOV jitter and stutter.
		if (!player.isSprinting() && shouldSprint(client, player)) {
			player.setSprinting(true);
		}
	}

	/**
	 * Mirrors what vanilla considers a valid sprint, minus the checks that
	 * flicker (horizontalCollision, interpolated forwardImpulse).
	 */
	private static boolean shouldSprint(Minecraft client, LocalPlayer player) {
		// Use the raw key press, not `input.forwardImpulse` (which is
		// interpolated and flickers around 0 while starting/stopping).
		if (!client.options.keyUp.isDown()) {
			return false;
		}

		// Sneaking — no sprint.
		if (player.isCrouching()) {
			return false;
		}

		// Using an item (eating, bow, shield) slows you down — sprint is reset.
		if (player.isUsingItem()) {
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

		// NOTE: no horizontalCollision check on purpose.
		// It flickers true/false on uneven terrain (stairs, slabs, edges),
		// which made sprint toggle every tick and caused screen stutter.
		// Vanilla already resets sprint on wall hits by itself.

		return true;
	}
}
