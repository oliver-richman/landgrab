package io.github.oliverrichman.landgrab.client;

import io.github.oliverrichman.landgrab.Coins;
import io.github.oliverrichman.landgrab.Economy;
import io.github.oliverrichman.landgrab.Landgrab;
import io.github.oliverrichman.landgrab.Payloads;
import io.github.oliverrichman.landgrab.TradeGroups;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix3x2fStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class PhonePanel {
	public static final int WIDTH = 146;
	public static final int HEIGHT = 166;
	private static final Identifier ICON_MAP = Landgrab.id("app/map");
	private static final Identifier ICON_TRADE = Landgrab.id("app/trade");
	private static final Identifier ICON_MESSAGES = Landgrab.id("app/messages");
	private static final Identifier MARKER = Landgrab.id("marker");
	private static final DateTimeFormatter STAMP =
			DateTimeFormatter.ofPattern("d MMM, HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final int SCREEN_INSET_X = 6;
	private static final int SCREEN_INSET_Y = 8;
	private static final int SCREEN_WIDTH = WIDTH - SCREEN_INSET_X * 2;
	private static final int SCREEN_HEIGHT = HEIGHT - SCREEN_INSET_Y * 2;
	private static final int STATUS_HEIGHT = 12;
	private static final int NAV_HEIGHT = 24;
	private static final int CONTENT_TOP = STATUS_HEIGHT + 1;
	private static final int CONTENT_HEIGHT = SCREEN_HEIGHT - CONTENT_TOP - NAV_HEIGHT;
	private static final int CELL = 14;
	private static final int MAP_COLUMNS = 9;
	private static final int MAP_ROWS = 7;
	private static final int MAP_TOP = CONTENT_TOP + 12;
	private static final int ROW_HEIGHT = 20;
	private static final int VISIBLE_ROWS = 4;
	private static final int BUTTON_HEIGHT = 16;
	private static final int MESSAGE_PADDING = 6;
	private static final int MESSAGE_GAP = 4;
	private static final int BEZEL = 0xFF0C0F13;
	private static final int BEZEL_EDGE = 0xFF2A323B;
	private static final int SCREEN = 0xFF141A21;
	private static final int STATUS = 0xFF1D252E;
	private static final int NAV = 0xFF1D252E;
	private static final int NAV_ACTIVE = 0xFF2E6E8E;
	private static final int TEXT = 0xFFE8EDF2;
	private static final int DIM_TEXT = 0xFF8A99A8;
	private static final int FAINT_TEXT = 0xFF66727E;
	private static final int MONEY = 0xFFF0C64B;
	private static final int OWNED = 0xFF2C6E4C;
	private static final int OWNED_EDGE = 0xFF49A272;
	private static final int AFFORDABLE = 0xFF8A6C1E;
	private static final int AFFORDABLE_EDGE = 0xFFE0B33C;
	private static final int TOO_DEAR = 0xFF33291A;
	private static final int TOO_DEAR_EDGE = 0xFF6B5A33;
	private static final int LOCKED = 0xFF10151A;
	private static final int LOCKED_EDGE = 0xFF1E252C;
	private static final int HIGHLIGHT = 0xFFFFFFFF;
	private static final int ROW = 0xFF1B232B;
	private static final int ROW_HOVER = 0xFF26313B;
	private static final int UNREAD = 0xFF1F2C38;
	private static final int SELL = 0xFF2F6B45;
	private static final int CANCEL = 0xFF6B3030;
	private static final int BADGE = 0xFFE0483C;
	private static final int BADGE_EDGE = 0xFF14181D;

	private enum App {
		MAP, TRADE, MESSAGES
	}

	private App app = App.MAP;
	private int x;
	private int y;
	private double panX;
	private double panZ;
	private int scroll;

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
	}

	public void reset() {
		panX = 0;
		panZ = 0;
		scroll = 0;
	}

	private int screenX() {
		return x + SCREEN_INSET_X;
	}

	private int screenY() {
		return y + SCREEN_INSET_Y;
	}

	public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		graphics.fill(x, y, x + WIDTH, y + HEIGHT, BEZEL);
		graphics.outline(x, y, WIDTH, HEIGHT, BEZEL_EDGE);
		graphics.fill(screenX(), screenY(), screenX() + SCREEN_WIDTH, screenY() + SCREEN_HEIGHT, SCREEN);
		status(graphics, font);
		switch (app) {
			case MAP -> map(graphics, font, client, mouseX, mouseY);
			case TRADE -> trade(graphics, font, client, mouseX, mouseY);
			case MESSAGES -> messages(graphics, font, mouseX, mouseY);
		}
		nav(graphics, mouseX, mouseY);
	}

	private void status(GuiGraphicsExtractor graphics, Font font) {
		int top = screenY();
		graphics.fill(screenX(), top, screenX() + SCREEN_WIDTH, top + STATUS_HEIGHT, STATUS);
		Component title = Component.translatable(switch (app) {
			case MAP -> "landgrab.phone.app.map";
			case TRADE -> "landgrab.phone.app.trade";
			case MESSAGES -> "landgrab.phone.app.messages";
		});
		graphics.text(font, title, screenX() + 4, top + 2, TEXT);

		Component money = Coins.of(ClientState.balance());
		graphics.text(font, money, screenX() + SCREEN_WIDTH - 4 - font.width(money), top + 2, MONEY);
	}

	private ChunkPos here() {
		Minecraft client = Minecraft.getInstance();
		return client.player != null
				? ChunkPos.containing(client.player.blockPosition())
				: ClientState.origin();
	}

	private ChunkPos centre() {
		ChunkPos here = here();
		return new ChunkPos(here.x() + (int) Math.round(panX), here.z() + (int) Math.round(panZ));
	}

	private int gridLeft() {
		return screenX() + (SCREEN_WIDTH - MAP_COLUMNS * CELL) / 2;
	}

	private int gridTop() {
		return screenY() + MAP_TOP;
	}

	private void map(GuiGraphicsExtractor graphics, Font font, Minecraft client, int mouseX, int mouseY) {
		ChunkPos centre = centre();
		ChunkPos here = here();
		long price = ClientState.nextPrice();
		boolean canAfford = ClientState.balance() >= price;
		graphics.text(font, Landgrab.chunks(ClientState.ownedCount()),
				screenX() + 4, screenY() + CONTENT_TOP + 1, DIM_TEXT);
		Component cost = Component.translatable("landgrab.phone.next", Coins.of(price));
		graphics.text(font, cost, screenX() + SCREEN_WIDTH - 4 - font.width(cost),
				screenY() + CONTENT_TOP + 1, canAfford ? MONEY : TOO_DEAR_EDGE);
		for (int row = 0; row < MAP_ROWS; row++) {
			for (int column = 0; column < MAP_COLUMNS; column++) {
				int chunkX = centre.x() + column - MAP_COLUMNS / 2;
				int chunkZ = centre.z() + row - MAP_ROWS / 2;
				int cellX = gridLeft() + column * CELL;
				int cellY = gridTop() + row * CELL;

				int fill;
				int edge;
				if (ClientState.owns(chunkX, chunkZ)) {
					fill = OWNED;
					edge = OWNED_EDGE;
				} else if (ClientState.isForSale(chunkX, chunkZ)) {
					fill = canAfford ? AFFORDABLE : TOO_DEAR;
					edge = canAfford ? AFFORDABLE_EDGE : TOO_DEAR_EDGE;
				} else {
					fill = LOCKED;
					edge = LOCKED_EDGE;
				}
				graphics.fill(cellX + 1, cellY + 1, cellX + CELL - 1, cellY + CELL - 1, fill);
				graphics.outline(cellX, cellY, CELL, CELL, edge);
				boolean hovered = mouseX >= cellX && mouseX < cellX + CELL
						&& mouseY >= cellY && mouseY < cellY + CELL;
				if (hovered) {
					graphics.outline(cellX, cellY, CELL, CELL, HIGHLIGHT);
					graphics.setTooltipForNextFrame(tooltipFor(chunkX, chunkZ, price), mouseX, mouseY);
				}

				if (chunkX == here.x() && chunkZ == here.z() && client.player != null) {
					marker(graphics, cellX + CELL / 2f, cellY + CELL / 2f, client.player.getYRot());
				}
			}
		}
	}

	private void marker(GuiGraphicsExtractor graphics, float centreX, float centreY, float yaw) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(centreX, centreY);
		pose.rotate((float) Math.toRadians(yaw + 180.0f));
		pose.translate(-4.0f, -4.0f);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MARKER, 0, 0, 8, 8);
		pose.popMatrix();
	}

	private Component tooltipFor(int chunkX, int chunkZ, long price) {
		if (ClientState.owns(chunkX, chunkZ)) {
			return Component.translatable("landgrab.phone.tip.owned", chunkX, chunkZ);
		}
		if (ClientState.isForSale(chunkX, chunkZ)) {
			return Component.translatable("landgrab.phone.tip.for_sale", chunkX, chunkZ, Coins.of(price));
		}
		return Component.translatable("landgrab.phone.tip.locked", chunkX, chunkZ);
	}

	private int listTop() {
		return screenY() + CONTENT_TOP + 12;
	}

	private int listHeight() {
		return VISIBLE_ROWS * ROW_HEIGHT;
	}

	private int buttonsTop() {
		return screenY() + CONTENT_TOP + CONTENT_HEIGHT - BUTTON_HEIGHT - 2;
	}

	private void trade(GuiGraphicsExtractor graphics, Font font, Minecraft client, int mouseX, int mouseY) {
		List<TradeGroups.Group> groups = TradeGroups.of(ClientState.tray());
		long total = TradeGroups.totalValue(ClientState.tray());

		Component worth = Component.translatable("landgrab.phone.tray_total", Coins.of(total));
		graphics.text(font, worth, screenX() + SCREEN_WIDTH - 4 - font.width(worth),
				screenY() + CONTENT_TOP + 1, total > 0L ? MONEY : DIM_TEXT);
		int left = screenX() + 4;
		int width = SCREEN_WIDTH - 8;
		int top = listTop();
		graphics.fill(left, top, left + width, top + listHeight(), 0xFF0F141A);
		if (groups.isEmpty()) {
			graphics.text(font, Component.translatable("landgrab.phone.tray_empty"),
					left + 6, top + listHeight() / 2 - 4, DIM_TEXT);
		}
		scroll = Math.max(0, Math.min(scroll, Math.max(0, groups.size() - VISIBLE_ROWS)));
		for (int slot = 0; slot < VISIBLE_ROWS; slot++) {
			int index = slot + scroll;
			if (index >= groups.size()) {
				break;
			}
			TradeGroups.Group group = groups.get(index);
			int rowY = top + slot * ROW_HEIGHT;
			boolean hovered = mouseX >= left && mouseX < left + width
					&& mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
			graphics.fill(left, rowY, left + width, rowY + ROW_HEIGHT - 1, hovered ? ROW_HOVER : ROW);
			graphics.item(group.sample(), left + 2, rowY + 2);
			Component value = Coins.of(group.value());
			int valueX = left + width - 4 - font.width(value);
			graphics.text(font, value, valueX, rowY + 6, MONEY);
			Component name = Component.empty()
					.append(group.sample().getHoverName())
					.append(Component.literal(" x" + group.count()));
			int nameRight = Math.max(left + 22, valueX - 3);
			graphics.enableScissor(left + 21, rowY, nameRight, rowY + ROW_HEIGHT);
			graphics.text(font, name, left + 22, rowY + 6, TEXT);
			graphics.disableScissor();
		}
		int half = (width - 4) / 2;
		button(graphics, font, left, buttonsTop(), half, Component.translatable("landgrab.phone.sell"),
				SELL, total > 0L, mouseX, mouseY);
		button(graphics, font, left + half + 4, buttonsTop(), half,
				Component.translatable("landgrab.phone.cancel"), CANCEL, !groups.isEmpty(), mouseX, mouseY);
		counterHint(graphics, client, groups, left, width, top, mouseX, mouseY);
	}

	private void counterHint(GuiGraphicsExtractor graphics, Minecraft client,
			List<TradeGroups.Group> groups, int left, int width, int top, int mouseX, int mouseY) {
		boolean overCounter = mouseX >= left && mouseX < left + width
				&& mouseY >= top && mouseY < top + listHeight();
		if (!overCounter || client.player == null) {
			return;
		}
		ItemStack carried = client.player.containerMenu.getCarried();
		if (!carried.isEmpty()) {
			long value = Economy.stackValue(carried);
			graphics.setTooltipForNextFrame(value > 0L
							? Component.translatable("landgrab.phone.tip.sells_for",
									carried.getHoverName(), Coins.of(value))
							: Component.translatable("landgrab.phone.tip.not_sellable",
									carried.getHoverName()),
					mouseX, mouseY);
			return;
		}
		int index = (int) ((mouseY - top) / ROW_HEIGHT) + scroll;
		if (index >= 0 && index < groups.size()) {
			graphics.setTooltipForNextFrame(
					Component.translatable("landgrab.phone.tip.take_back"), mouseX, mouseY);
		}
	}

	private void button(GuiGraphicsExtractor graphics, Font font, int left, int top, int width,
			Component label, int colour, boolean enabled, int mouseX, int mouseY) {
		boolean hovered = enabled && mouseX >= left && mouseX < left + width
				&& mouseY >= top && mouseY < top + BUTTON_HEIGHT;
		int fill = enabled ? (hovered ? brighten(colour) : colour) : 0xFF20262D;
		graphics.fill(left, top, left + width, top + BUTTON_HEIGHT, fill);
		graphics.text(font, label, left + (width - font.width(label)) / 2, top + 4,
				enabled ? TEXT : DIM_TEXT);
	}

	private static int brighten(int colour) {
		int red = Math.min(255, ((colour >> 16) & 0xFF) + 30);
		int green = Math.min(255, ((colour >> 8) & 0xFF) + 30);
		int blue = Math.min(255, (colour & 0xFF) + 30);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	private void messages(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
		List<ClientState.Message> lines = ClientState.messages();
		int left = screenX() + 4;
		int width = SCREEN_WIDTH - 8;
		int top = screenY() + CONTENT_TOP + 2;
		int bottom = screenY() + CONTENT_TOP + CONTENT_HEIGHT;
		if (lines.isEmpty()) {
			graphics.text(font, Component.translatable("landgrab.phone.no_messages"),
					left, top + 4, DIM_TEXT);
			return;
		}
		scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - 1)));

		long now = System.currentTimeMillis();
		int drawY = top;
		for (int index = scroll; index < lines.size(); index++) {
			ClientState.Message message = lines.get(index);
			Component age = Component.translatable("landgrab.phone.ago", relative(now - message.receivedAt()));

			int textWidth = width - 7 - font.width(age) - MESSAGE_GAP;
			int height = Math.max(font.wordWrapHeight(message.text(), textWidth), font.lineHeight)
					+ MESSAGE_PADDING;
			if (drawY + height > bottom) {
				break;
			}
			boolean hovered = mouseX >= left && mouseX < left + width
					&& mouseY >= drawY && mouseY < drawY + height;
			if (!message.isRead()) {
				graphics.fill(left, drawY, left + width, drawY + height - 2, UNREAD);
				graphics.fill(left, drawY, left + 2, drawY + height - 2, BADGE);
			} else if (hovered) {
				graphics.fill(left, drawY, left + width, drawY + height - 2, ROW);
			}
			graphics.text(font, age, left + width - 2 - font.width(age), drawY + 2, FAINT_TEXT);
			graphics.textWithWordWrap(font, message.text(), left + 5, drawY + 2, textWidth,
					message.isRead() ? DIM_TEXT : TEXT);
			if (hovered) {
				graphics.setTooltipForNextFrame(
						Component.literal(STAMP.format(Instant.ofEpochMilli(message.receivedAt()))),
						mouseX, mouseY);
			}
			drawY += height;
		}
	}

	private static String relative(long millis) {
		long seconds = Math.max(0L, millis / 1000L);
		if (seconds < 60L) {
			return seconds + "s";
		}
		if (seconds < 3600L) {
			return seconds / 60L + "m";
		}
		if (seconds < 86400L) {
			return seconds / 3600L + "h";
		}
		return seconds / 86400L + "d";
	}

	private int navTop() {
		return screenY() + SCREEN_HEIGHT - NAV_HEIGHT;
	}

	private void nav(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.fill(screenX(), navTop(), screenX() + SCREEN_WIDTH, screenY() + SCREEN_HEIGHT, NAV);
		App[] apps = App.values();
		int slotWidth = SCREEN_WIDTH / apps.length;
		for (int index = 0; index < apps.length; index++) {
			int left = screenX() + index * slotWidth;
			boolean active = apps[index] == app;
			boolean hovered = mouseX >= left && mouseX < left + slotWidth
					&& mouseY >= navTop() && mouseY < screenY() + SCREEN_HEIGHT;

			if (active) {
				graphics.fill(left + 2, navTop() + 1, left + slotWidth - 2, navTop() + 3, NAV_ACTIVE);
			}
			if (hovered) {
				graphics.fill(left + 2, navTop() + 4, left + slotWidth - 2,
						screenY() + SCREEN_HEIGHT - 2, 0x30FFFFFF);
				graphics.setTooltipForNextFrame(Component.translatable(switch (apps[index]) {
					case MAP -> "landgrab.phone.app.map";
					case TRADE -> "landgrab.phone.app.trade";
					case MESSAGES -> "landgrab.phone.app.messages";
				}), mouseX, mouseY);
			}
			Identifier icon = switch (apps[index]) {
				case MAP -> ICON_MAP;
				case TRADE -> ICON_TRADE;
				case MESSAGES -> ICON_MESSAGES;
			};
			int iconX = left + (slotWidth - 16) / 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, iconX, navTop() + 5, 16, 16);
			if (apps[index] == App.MESSAGES && ClientState.hasUnread()) {
				unreadDot(graphics, iconX + 12, navTop() + 4);
			}
		}
	}

	public static void unreadDot(GuiGraphicsExtractor graphics, int left, int top) {
		graphics.fill(left - 1, top - 1, left + 5, top + 5, BADGE_EDGE);
		graphics.fill(left, top, left + 4, top + 4, BADGE);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY)) {
			return false;
		}
		if (mouseY >= navTop() && mouseY < screenY() + SCREEN_HEIGHT) {
			App[] apps = App.values();
			int slotWidth = SCREEN_WIDTH / apps.length;
			int index = (int) ((mouseX - screenX()) / slotWidth);
			if (index >= 0 && index < apps.length) {
				show(apps[index]);
			}
			return true;
		}

		return switch (app) {
			case MAP -> clickMap(mouseX, mouseY);
			case TRADE -> clickTrade(mouseX, mouseY, button);
			case MESSAGES -> true;
		};
	}

	public void show(App next) {
		app = next;
		scroll = 0;
		if (next == App.MESSAGES) {
			ClientState.markMessagesRead();
		}
	}

	public void showMessages() {
		show(App.MESSAGES);
	}

	private boolean clickMap(double mouseX, double mouseY) {
		int column = (int) Math.floor((mouseX - gridLeft()) / CELL);
		int row = (int) Math.floor((mouseY - gridTop()) / CELL);
		if (column < 0 || column >= MAP_COLUMNS || row < 0 || row >= MAP_ROWS) {
			return true;
		}
		ChunkPos centre = centre();
		int chunkX = centre.x() + column - MAP_COLUMNS / 2;
		int chunkZ = centre.z() + row - MAP_ROWS / 2;
		if (ClientState.isForSale(chunkX, chunkZ)) {
			ClientPlayNetworking.send(new Payloads.BuyChunk(ChunkPos.pack(chunkX, chunkZ)));
		}
		return true;
	}

	private boolean clickTrade(double mouseX, double mouseY, int button) {
		Minecraft client = Minecraft.getInstance();
		ItemStack carried = client.player == null
				? ItemStack.EMPTY
				: client.player.containerMenu.getCarried();
		int left = screenX() + 4;
		int width = SCREEN_WIDTH - 8;
		int top = listTop();
		if (mouseY >= top && mouseY < top + listHeight() && mouseX >= left && mouseX < left + width) {
			if (!carried.isEmpty()) {
				ClientPlayNetworking.send(new Payloads.TrayDeposit(button != 1));
				return true;
			}
			int slot = (int) ((mouseY - top) / ROW_HEIGHT) + scroll;
			if (slot >= 0 && slot < TradeGroups.of(ClientState.tray()).size()) {
				ClientPlayNetworking.send(new Payloads.TrayWithdraw(slot));
			}
			return true;
		}
		if (mouseY >= buttonsTop() && mouseY < buttonsTop() + BUTTON_HEIGHT) {
			int half = (width - 4) / 2;
			if (mouseX >= left && mouseX < left + half) {
				ClientPlayNetworking.send(new Payloads.TrayAction(true));
			} else if (mouseX >= left + half + 4 && mouseX < left + half + 4 + half) {
				ClientPlayNetworking.send(new Payloads.TrayAction(false));
			}
			return true;
		}
		if (!carried.isEmpty()) {
			ClientPlayNetworking.send(new Payloads.TrayDeposit(button != 1));
		}
		return true;
	}

	public boolean mouseDragged(double dragX, double dragY) {
		if (app != App.MAP) {
			return false;
		}
		panX -= dragX / CELL;
		panZ -= dragY / CELL;
		return true;
	}

	public boolean mouseScrolled(double amount) {
		if (app == App.MAP) {
			return false;
		}
		scroll = Math.max(0, scroll - (int) Math.signum(amount));
		return true;
	}
}
