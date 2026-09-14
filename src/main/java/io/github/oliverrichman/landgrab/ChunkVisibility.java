package io.github.oliverrichman.landgrab;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkVisibility {
	private ChunkVisibility() {
	}

	private static final int FOG_RADIUS = 2;

	public static boolean unrestricted(ServerPlayer player) {
		return player.isCreative() || player.isSpectator();
	}

	public static void refresh(ServerPlayer player) {
		ServerLevel level = player.level();
		MinecraftServer server = level.getServer();
		if (server == null) {
			return;
		}
		int reach = server.getPlayerList().getViewDistance();
		ChunkPos centre = player.chunkPosition();
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(centre.x() + dx, centre.z() + dz);
				if (chunk != null) {
					player.connection.chunkSender.markChunkPendingToSend(chunk);
				}
			}
		}
	}

	public static boolean sees(ServerPlayer player, ChunkPos pos) {
		if (!Landgrab.isActive() || unrestricted(player)) {
			return true;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return true;
		}
		ResourceKey<Level> dimension = player.level().dimension();
		LandgrabState.Territory territory = LandgrabState.get(server).territory(dimension);

		return territory != null && territory.owns(pos);
	}

	public static boolean nearOwned(ServerPlayer player, ChunkPos pos) {
		if (!Landgrab.isActive()) {
			return true;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return true;
		}
		LandgrabState.Territory territory =
				LandgrabState.get(server).territory(player.level().dimension());
		if (territory == null) {
			return false;
		}
		for (long packed : territory.packed()) {
			ChunkPos owned = ChunkPos.unpack(packed);
			if (Math.abs(owned.x() - pos.x()) <= FOG_RADIUS
					&& Math.abs(owned.z() - pos.z()) <= FOG_RADIUS) {
				return true;
			}
		}
		return false;
	}

	public static void reveal(MinecraftServer server, ResourceKey<Level> dimension, ChunkPos pos) {
		ServerLevel level = server.getLevel(dimension);
		if (level == null) {
			return;
		}
		int reach = FOG_RADIUS + 1;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level().dimension() != dimension) {
				continue;
			}
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x() + dx, pos.z() + dz);
					if (chunk != null) {
						player.connection.chunkSender.markChunkPendingToSend(chunk);
					}
				}
			}
		}
	}
}
