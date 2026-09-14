package io.github.oliverrichman.landgrab.client;

import io.github.oliverrichman.landgrab.Payloads;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientState {
	private static final int MESSAGE_LIMIT = 50;

	public static final class Message {
		private final Component text;
		private final long receivedAt;
		private boolean read;

		private Message(Component text) {
			this.text = text;
			this.receivedAt = System.currentTimeMillis();
		}

		public Component text() {
			return text;
		}

		public long receivedAt() {
			return receivedAt;
		}

		public boolean isRead() {
			return read;
		}
	}

	private static final Set<Long> OWNED = new HashSet<>();
	private static final List<ItemStack> TRAY = new ArrayList<>();
	private static final List<Message> MESSAGES = new ArrayList<>();
	private static boolean active;
	private static long balance;
	private static long nextPrice;
	private static ChunkPos origin = ChunkPos.ZERO;

	private ClientState() {
	}

	public static void accept(Payloads.StateSync sync) {
		active = sync.active();
		balance = sync.balance();
		nextPrice = sync.nextPrice();
		origin = ChunkPos.unpack(sync.origin());
		OWNED.clear();
		for (long packed : sync.owned()) {
			OWNED.add(packed);
		}
	}

	public static void acceptTray(Payloads.TraySync sync) {
		TRAY.clear();
		TRAY.addAll(sync.items());
	}

	public static void acceptMessage(Component text) {
		MESSAGES.addFirst(new Message(text));
		while (MESSAGES.size() > MESSAGE_LIMIT) {
			MESSAGES.removeLast();
		}
	}

	public static void markMessagesRead() {
		for (Message message : MESSAGES) {
			message.read = true;
		}
	}

	public static int unreadCount() {
		int unread = 0;
		for (Message message : MESSAGES) {
			if (!message.read) {
				unread++;
			}
		}
		return unread;
	}

	public static boolean hasUnread() {
		return unreadCount() > 0;
	}

	public static void clear() {
		active = false;
		OWNED.clear();
		TRAY.clear();
		MESSAGES.clear();
		balance = 0L;
		nextPrice = 0L;
	}

	public static boolean isActive() {
		return active;
	}

	public static boolean owns(int x, int z) {
		return OWNED.contains(ChunkPos.pack(x, z));
	}

	public static int ownedCount() {
		return OWNED.size();
	}

	public static long balance() {
		return balance;
	}

	public static long nextPrice() {
		return nextPrice;
	}

	public static ChunkPos origin() {
		return origin;
	}

	public static List<ItemStack> tray() {
		return TRAY;
	}

	public static List<Message> messages() {
		return MESSAGES;
	}

	public static boolean isForSale(int x, int z) {
		if (owns(x, z)) {
			return false;
		}
		return owns(x + 1, z) || owns(x - 1, z) || owns(x, z + 1) || owns(x, z - 1);
	}
}
