package io.github.oliverrichman.landgrab.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.oliverrichman.landgrab.Coins;
import io.github.oliverrichman.landgrab.Economy;
import io.github.oliverrichman.landgrab.Landgrab;
import io.github.oliverrichman.landgrab.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

public final class LandgrabClient implements ClientModInitializer {
	private static KeyMapping openPhone;
	@Override
	public void onInitializeClient() {
		openPhone = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.landgrab.phone",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_B,
				KeyMapping.Category.INVENTORY));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.StateSync.ID,
				(payload, context) -> ClientState.accept(payload));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.TraySync.ID,
				(payload, context) -> ClientState.acceptTray(payload));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Message.ID,
				(payload, context) -> ClientState.acceptMessage(payload.text()));
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Shock.ID,
				(payload, context) -> ShockOverlay.strike());
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Prices.ID,
				(payload, context) -> Economy.set(payload.values()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientState.clear();
			PhoneClient.reset();
			ShockOverlay.clear();
		});
		HudElementRegistry.addLast(Landgrab.id("shock"), ShockOverlay.INSTANCE);
		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> {
			if (!ClientState.isActive() || outline == null) {
				return true;
			}
			LocalPlayer viewer = Minecraft.getInstance().player;
			if (viewer != null && (viewer.isCreative() || viewer.isSpectator())) {
				return true;
			}
			ChunkPos pos = ChunkPos.containing(outline.pos());
			return ClientState.owns(pos.x(), pos.z());
		});
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!ClientState.isActive()) {
				return;
			}
			long total = Economy.stackValue(stack);
			if (total <= 0L) {
				return;
			}
			lines.add(stack.getCount() > 1
					? Component.translatable("landgrab.tooltip.value_stack",
							Coins.of(total), Coins.of(total / stack.getCount()))
							.withStyle(ChatFormatting.GOLD)
					: Coins.of(total).copy().withStyle(ChatFormatting.GOLD));
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openPhone.consumeClick()) {
				reachForPhone(client);
			}
			BoundarySparks.tick(client);
			ShockOverlay.tick();
		});
	}

	private static void reachForPhone(Minecraft client) {
		if (!ClientState.isActive() || client.player == null) {
			return;
		}
		if (client.gui.screen() == null) {
			client.setScreenAndShow(new InventoryScreen(client.player));
		}
		if (client.gui.screen() instanceof PhoneHost host) {
			host.landgrab$togglePhone();
		}
	}
}
