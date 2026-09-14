package io.github.oliverrichman.landgrab;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Optional;

public final class Bounties {
	private static final long TASK = 100L * Coins.PER_COIN;
	private static final long GOAL = 400L * Coins.PER_COIN;
	private static final long CHALLENGE = 2000L * Coins.PER_COIN;

	private Bounties() {
	}

	public static long valueOf(Advancement advancement) {
		Optional<DisplayInfo> display = advancement.display();
		if (display.isEmpty()) {
			return 0L;
		}
		AdvancementType type = display.get().getType();
		if (type == AdvancementType.CHALLENGE) {
			return CHALLENGE;
		}
		if (type == AdvancementType.GOAL) {
			return GOAL;
		}
		return TASK;
	}

	public static void pay(ServerPlayer player, AdvancementHolder holder) {
		MinecraftServer server = player.level().getServer();
		if (!Landgrab.isActive() || server == null) {
			return;
		}
		long value = valueOf(holder.value());
		if (value <= 0L) {
			return;
		}
		LandgrabState state = LandgrabState.get(server);
		if (!state.claimAdvancement(holder.id())) {
			return;
		}
		state.credit(value);
		Landgrab.syncAll(server);
		Landgrab.notify(player, Component.translatable("landgrab.message.advancement",
				Advancement.name(holder), Coins.of(value)));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
				SoundSource.PLAYERS, 0.4f, 1.8f);
	}
}
