package io.github.oliverrichman.landgrab;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Payloads {
	private Payloads() {
	}

	public record StateSync(boolean active, long balance, long origin, long nextPrice, long[] owned)
			implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<StateSync> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("state"));
		public static final StreamCodec<FriendlyByteBuf, StateSync> CODEC =
				CustomPacketPayload.codec(StateSync::write, StateSync::new);

		private StateSync(FriendlyByteBuf buf) {
			this(buf.readBoolean(), buf.readVarLong(), buf.readLong(), buf.readVarLong(), buf.readLongArray());
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeBoolean(active);
			buf.writeVarLong(balance);
			buf.writeLong(origin);
			buf.writeVarLong(nextPrice);
			buf.writeLongArray(owned);
		}

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record TraySync(List<ItemStack> items) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TraySync> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("tray"));
		public static final StreamCodec<RegistryFriendlyByteBuf, TraySync> CODEC =
				ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(TraySync::new, TraySync::items);
		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record Prices(Map<Item, Integer> values) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<Prices> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("prices"));
		public static final StreamCodec<FriendlyByteBuf, Prices> CODEC =
				CustomPacketPayload.codec(Prices::write, Prices::new);

		private Prices(FriendlyByteBuf buf) {
			this(read(buf));
		}

		private static Map<Item, Integer> read(FriendlyByteBuf buf) {
			int count = buf.readVarInt();
			Map<Item, Integer> values = new HashMap<>(count);
			for (int entry = 0; entry < count; entry++) {
				int id = buf.readVarInt();
				int price = buf.readVarInt();
				BuiltInRegistries.ITEM.get(id).ifPresent(item -> values.put(item.value(), price));
			}
			return values;
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeVarInt(values.size());
			values.forEach((item, price) -> {
				buf.writeVarInt(BuiltInRegistries.ITEM.getId(item));
				buf.writeVarInt(price);
			});
		}

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record Shock() implements CustomPacketPayload {
		public static final Shock INSTANCE = new Shock();
		public static final CustomPacketPayload.Type<Shock> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("shock"));
		public static final StreamCodec<FriendlyByteBuf, Shock> CODEC = StreamCodec.unit(INSTANCE);
		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record Message(Component text) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<Message> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("message"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Message> CODEC =
				ComponentSerialization.STREAM_CODEC.map(Message::new, Message::text);
		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record BuyChunk(long chunk) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<BuyChunk> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("buy_chunk"));
		public static final StreamCodec<FriendlyByteBuf, BuyChunk> CODEC =
				CustomPacketPayload.codec(BuyChunk::write, BuyChunk::new);

		private BuyChunk(FriendlyByteBuf buf) {
			this(buf.readLong());
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeLong(chunk);
		}

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record TrayDeposit(boolean whole) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TrayDeposit> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("tray_deposit"));
		public static final StreamCodec<FriendlyByteBuf, TrayDeposit> CODEC =
				CustomPacketPayload.codec(TrayDeposit::write, TrayDeposit::new);

		private TrayDeposit(FriendlyByteBuf buf) {
			this(buf.readBoolean());
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeBoolean(whole);
		}

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record TrayWithdraw(int group) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TrayWithdraw> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("tray_withdraw"));
		public static final StreamCodec<FriendlyByteBuf, TrayWithdraw> CODEC =
				CustomPacketPayload.codec(TrayWithdraw::write, TrayWithdraw::new);

		private TrayWithdraw(FriendlyByteBuf buf) {
			this(buf.readVarInt());
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeVarInt(group);
		}

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record TrayAction(boolean sell) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<TrayAction> ID =
				new CustomPacketPayload.Type<>(Landgrab.id("tray_action"));
		public static final StreamCodec<FriendlyByteBuf, TrayAction> CODEC =
				CustomPacketPayload.codec(TrayAction::write, TrayAction::new);

		private TrayAction(FriendlyByteBuf buf) {
			this(buf.readBoolean());
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeBoolean(sell);
		}

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(StateSync.ID, StateSync.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TraySync.ID, TraySync.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Message.ID, Message.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Shock.ID, Shock.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Prices.ID, Prices.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BuyChunk.ID, BuyChunk.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TrayDeposit.ID, TrayDeposit.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TrayWithdraw.ID, TrayWithdraw.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TrayAction.ID, TrayAction.CODEC);
	}
}
