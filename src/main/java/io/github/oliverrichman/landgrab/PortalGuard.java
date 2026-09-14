package io.github.oliverrichman.landgrab;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PortalGuard {
	private static final int MESSAGE_COOLDOWN_TICKS = 80;
	private static final Map<UUID, Integer> LAST_TOLD = new HashMap<>();

	private PortalGuard() {
	}

	public static boolean allow(ServerPlayer player, TeleportTransition transition) {
		if (!Landgrab.isActive()) {
			return true;
		}
		ServerLevel destination = transition.newLevel();
		if (destination == null || destination.dimension() == player.level().dimension()) {
			return true;
		}
		if (player.isCreative() || player.isSpectator()) {
			return true;
		}
		if (player.isDeadOrDying() || transition.missingRespawnBlock()) {
			return true;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return true;
		}
		ResourceKey<Level> dimension = destination.dimension();
		ChunkPos landing = ChunkPos.containing(BlockPos.containing(transition.position()));
		LandgrabState state = LandgrabState.get(server);
		if (!state.hasTerritory(dimension)) {

			state.seed(dimension, landing);
			ChunkVisibility.reveal(server, dimension, landing);
			Landgrab.notify(player, Component.translatable("landgrab.message.frontier_opened",
					name(dimension), landing.x(), landing.z()));
			return true;
		}
		if (state.owns(dimension, landing)) {
			return true;
		}
		int now = server.getTickCount();
		Integer told = LAST_TOLD.get(player.getUUID());
		if (told == null || now - told >= MESSAGE_COOLDOWN_TICKS) {
			LAST_TOLD.put(player.getUUID(), now);
			Landgrab.notify(player, Component.translatable("landgrab.message.portal_blocked",
					name(dimension), landing.x(), landing.z()));
		}
		return false;
	}

	public static void forget(UUID player) {
		LAST_TOLD.remove(player);
	}

	private static Component name(ResourceKey<Level> dimension) {
		return Component.translatableWithFallback(
				"landgrab.dimension." + dimension.identifier().getPath(),
				dimension.identifier().toString());
	}
}
