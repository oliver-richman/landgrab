package io.github.oliverrichman.landgrab.client;

import io.github.oliverrichman.landgrab.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class PhoneClient {
	private static final PhonePanel PANEL = new PhonePanel();
	private static boolean open;

	private PhoneClient() {
	}

	public static PhonePanel panel() {
		return PANEL;
	}

	public static boolean isOpen() {
		return open && ClientState.isActive();
	}

	public static void open() {
		PANEL.reset();
		open = true;
	}

	public static void close() {
		if (!open) {
			return;
		}
		open = false;
		if (Minecraft.getInstance().getConnection() != null) {
			ClientPlayNetworking.send(new Payloads.TrayAction(false));
		}
	}

	public static void reset() {
		open = false;
	}
}
