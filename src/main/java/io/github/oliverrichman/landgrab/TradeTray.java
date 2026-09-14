package io.github.oliverrichman.landgrab;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TradeTray {
	private static final Map<UUID, List<ItemStack>> TRAYS = new HashMap<>();

	private TradeTray() {
	}

	private static List<ItemStack> tray(ServerPlayer player) {
		return TRAYS.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
	}

	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, new Payloads.TraySync(List.copyOf(tray(player))));
	}

	public static void deposit(ServerPlayer player, boolean whole) {
		ItemStack carried = player.containerMenu.getCarried();
		if (carried.isEmpty()) {
			return;
		}
		if (!Economy.isSellable(carried)) {
			Landgrab.warn(player, Component.translatable("landgrab.message.no_offer_for",
					carried.getHoverName()));
			return;
		}
		ItemStack moved = whole ? carried.copy() : carried.copyWithCount(1);
		ItemStack remainder = carried.copy();
		remainder.shrink(moved.getCount());
		tray(player).add(moved);
		setCarried(player, remainder);
		sync(player);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP,
				SoundSource.PLAYERS, 0.3f, 1.4f);
	}

	public static void withdraw(ServerPlayer player, int group) {
		List<ItemStack> items = tray(player);
		List<TradeGroups.Group> groups = TradeGroups.of(items);
		if (group < 0 || group >= groups.size()) {
			return;
		}

		List<Integer> sources = new ArrayList<>(groups.get(group).sources());
		sources.sort((left, right) -> Integer.compare(right, left));
		for (int index : sources) {
			player.getInventory().placeItemBackInInventory(items.remove(index));
		}
		sync(player);
	}

	public static void sell(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		List<ItemStack> items = tray(player);
		if (items.isEmpty()) {
			return;
		}
		long earned = 0L;
		int count = 0;
		for (ItemStack stack : items) {
			earned += Economy.stackValue(stack);
			count += stack.getCount();
		}
		items.clear();

		LandgrabState.get(server).credit(earned);
		Landgrab.syncAll(server);
		sync(player);
		Landgrab.notify(player, Component.translatable("landgrab.message.sold", count, Coins.of(earned)));
		player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
				SoundSource.PLAYERS, 0.5f, 1.2f);
	}

	public static void returnAll(ServerPlayer player) {
		List<ItemStack> items = TRAYS.get(player.getUUID());
		if (items == null || items.isEmpty()) {
			return;
		}
		for (ItemStack stack : items) {
			player.getInventory().placeItemBackInInventory(stack);
		}
		items.clear();
		sync(player);
	}

	public static void forget(UUID player) {
		TRAYS.remove(player);
	}

	private static void setCarried(ServerPlayer player, ItemStack stack) {
		player.containerMenu.setCarried(stack);
		player.connection.send(new ClientboundSetCursorItemPacket(stack));
	}
}
