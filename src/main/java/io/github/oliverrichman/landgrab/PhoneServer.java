package io.github.oliverrichman.landgrab;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class PhoneServer {
	private PhoneServer() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(Payloads.BuyChunk.ID,
				(payload, context) -> buy(context.player(), ChunkPos.unpack(payload.chunk())));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.TrayDeposit.ID,
				(payload, context) -> TradeTray.deposit(context.player(), payload.whole()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.TrayWithdraw.ID,
				(payload, context) -> TradeTray.withdraw(context.player(), payload.group()));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.TrayAction.ID, (payload, context) -> {
			if (payload.sell()) {
				TradeTray.sell(context.player());
			} else {
				TradeTray.returnAll(context.player());
			}
		});
	}

	private static void buy(ServerPlayer player, ChunkPos target) {
		MinecraftServer server = player.level().getServer();
		if (!Landgrab.isActive() || server == null) {
			return;
		}
		ResourceKey<Level> dimension = player.level().dimension();
		LandgrabState state = LandgrabState.get(server);
		LandgrabState.Territory territory = state.territory(dimension);
		if (territory == null || territory.owns(target)) {
			return;
		}
		if (!territory.isAdjacentToOwned(target)) {
			refuse(player, Component.translatable("landgrab.message.not_adjacent"));
			return;
		}
		long price = Prices.nextChunk(territory.size());
		if (!state.debit(price)) {
			refuse(player, Component.translatable("landgrab.message.cannot_afford", Coins.of(price), Coins.of(state.balance())));
			return;
		}
		state.claim(dimension, target);
		ChunkVisibility.reveal(server, dimension, target);
		Landgrab.syncAll(server);
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
				SoundSource.PLAYERS, 0.6f, 1.4f);
		Landgrab.notify(player, Component.translatable("landgrab.message.bought",
				target.x(), target.z(), Coins.of(price)));
	}

	private static void refuse(ServerPlayer player, Component reason) {
		Landgrab.warn(player, reason);
		player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO,
				SoundSource.PLAYERS, 0.4f, 1.0f);
	}
}
