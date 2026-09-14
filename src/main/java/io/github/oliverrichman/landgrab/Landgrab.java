package io.github.oliverrichman.landgrab;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class Landgrab implements ModInitializer {
	public static final String MOD_ID = "landgrab";

	public static final ResourceKey<DamageType> BOUNDARY_DAMAGE =
			ResourceKey.create(Registries.DAMAGE_TYPE, id("boundary"));

	private static boolean active;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static boolean isActive() {
		return active;
	}

	@Override
	public void onInitialize() {
		LandgrabBlocks.register();
		LandgrabRules.register();
		Payloads.register();
		Containment.register();
		PhoneServer.register();
		LandgrabCommand.register();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			active = server.getGameRules().get(LandgrabRules.MODE);
			Economy.set(PriceSolver.solve(server));
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> active = false);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (active) {
				ChunkPos start = ChunkPos.containing(player.blockPosition());
				LandgrabState.get(server).seed(player.level().dimension(), start);
				ChunkVisibility.reveal(server, player.level().dimension(), start);
			}
			ServerPlayNetworking.send(player, new Payloads.Prices(Economy.all()));
			sync(player);
			TradeTray.sync(player);
		});

		ServerPlayerEvents.LEAVE.register(player -> {
			TradeTray.returnAll(player);
			TradeTray.forget(player.getUUID());
			Containment.forget(player.getUUID());
		});

		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
				(player, from, to) -> sync(player));

		ServerPlayerEvents.AFTER_RESPAWN.register((from, to, alive) -> {
			Containment.forget(to.getUUID());
			if (active) {
				LandgrabState state = LandgrabState.get(to.level().getServer());
				ChunkPos where = ChunkPos.containing(to.blockPosition());
				if (!state.owns(to.level().dimension(), where)) {
					state.claim(to.level().dimension(), where);
					ChunkVisibility.reveal(to.level().getServer(), to.level().dimension(), where);
				}
			}
			sync(to);
		});
	}

	public static void sync(ServerPlayer player) {
		if (!active) {
			ServerPlayNetworking.send(player, new Payloads.StateSync(false, 0L, 0L, 0L, new long[0]));
			return;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		LandgrabState state = LandgrabState.get(server);
		LandgrabState.Territory territory = state.territory(player.level().dimension());
		if (territory == null) {
			ServerPlayNetworking.send(player, new Payloads.StateSync(
					true, state.balance(), 0L, Prices.nextChunk(0), new long[0]));
			return;
		}
		ServerPlayNetworking.send(player, new Payloads.StateSync(
				true,
				state.balance(),
				territory.origin().pack(),
				Prices.nextChunk(territory.size()),
				territory.packed()));
	}

	public static void syncAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sync(player);
		}
	}

	public static void notify(ServerPlayer player, Component text) {
		ServerPlayNetworking.send(player, new Payloads.Message(text));
		player.sendSystemMessage(text, true);
	}

	public static Component chunks(int count) {
		return count == 1
				? Component.translatable("landgrab.chunks_one")
				: Component.translatable("landgrab.chunks", count);
	}

	public static void warn(ServerPlayer player, Component text) {
		player.sendSystemMessage(text, true);
	}
}
