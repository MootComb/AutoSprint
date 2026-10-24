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

	/**
	 * Forward impulse threshold. Vanilla considers sprint valid only when moving forward.
	 * Lowered from 0.8F to 0.5F because input interpolation makes the raw value
	 * fluctuate between ~0.7 and 1.0 while walking, which caused sprint to flicker.
	 */
	private static final float FORWARD_THRESHOLD = 0.5F;

	private static boolean enabled = true;

	/**
	 * Cached last applied sprint state. Used to avoid calling setSprinting()
	 * every tick with the same value, which causes FOV jitter and stuttering.
	 * null means "no decision yet" (e.g. just after toggle or leaving water).
	 */
	private static Boolean lastSprintState = null;

	private AutoSprintManager() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static boolean toggle() {
		enabled = !enabled;
		// Reset cache so the next tick re-applies the correct state cleanly.
		lastSprintState = null;
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
			lastSprintState = null;
			return;
		}

		LocalPlayer player = client.player;
		if (player == null || player.input == null) {
			return;
		}

		// In water, hand control back to vanilla entirely.
		// Vanilla handles swim-sprint itself; interfering here breaks swimming.
		if (player.isInWater()) {
			lastSprintState = null;
			return;
		}

		boolean should = shouldSprint(player);

		// Only call setSprinting when the state actually changes.
		// Calling it every tick with the same value causes FOV jitter / stutter.
		if (lastSprintState == null || lastSprintState != should) {
			player.setSprinting(should);
			lastSprintState = should;
		}
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

		// NOTE: horizontalCollision check removed on purpose.
		// It flickers true/false on uneven terrain (stairs, slabs, edges),
		// which made sprint toggle every tick and caused screen stutter.
		// Vanilla already resets sprint on wall hits by itself.

		return true;
	}
}
