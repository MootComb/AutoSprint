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

	/**
	 * Called from the mixin at the HEAD of LocalPlayer.tickMovement(),
	 * i.e. BEFORE vanilla decides whether to sprint this tick.
	 * This removes the 1-tick delay that END_CLIENT_TICK has and
	 * makes sprint restore instantly after leaving a slab/stairs.
	 */
	public static void onClientMove(LocalPlayer player) {
		if (!enabled) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (player != client.player) {
			return;
		}

		// Vanilla handles these states itself. Don't interfere.
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
		if (!player.isSprinting() && shouldSprint(client, player)) {
			player.setSprinting(true);
		}
	}

	private static boolean shouldSprint(Minecraft client, LocalPlayer player) {
		// Raw key press, not `input.forwardImpulse` (interpolated, flickers).
		if (!client.options.keyUp.isDown()) {
			return false;
		}

		if (player.isCrouching()) {
			return false;
		}

		if (player.isUsingItem()) {
			return false;
		}

		if (player.hasEffect(MobEffects.BLINDNESS)) {
			return false;
		}

		if (!player.isCreative() && !player.isSpectator()
				&& player.getFoodData().getFoodLevel() <= 6) {
			return false;
		}

		// No horizontalCollision check — vanilla resets sprint itself,
		// and checking it here causes flicker on stairs/slabs/edges.

		return true;
	}
}
