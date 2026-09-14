package io.github.oliverrichman.landgrab.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.RandomSource;

public final class ShockOverlay implements HudElement {
	private static final int DURATION = 9;
	private static final int ARCS = 5;
	private static final int FLASH = 0xFFFFFF;
	private static final int EDGE = 0x3FA8FF;
	private static final int ARC = 0xBFEFFF;
	private static final RandomSource RANDOM = RandomSource.create();
	private static int remaining;
	private static long seed;

	private ShockOverlay() {
	}

	public static final ShockOverlay INSTANCE = new ShockOverlay();

	public static void strike() {
		remaining = DURATION;
		seed = RANDOM.nextLong();
	}

	public static void tick() {
		if (remaining > 0) {
			remaining--;
		}
	}

	public static void clear() {
		remaining = 0;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
		if (remaining <= 0) {
			return;
		}
		float strength = remaining / (float) DURATION;
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		if (strength > 0.75f) {
			graphics.fill(0, 0, width, height, alpha(FLASH, (strength - 0.75f) * 4.0f * 0.45f));
		}
		int reach = Math.min(width, height) / 3;
		float edge = strength * 0.85f;
		graphics.fillGradient(0, 0, width, reach, alpha(EDGE, edge), alpha(EDGE, 0.0f));
		graphics.fillGradient(0, height - reach, width, height, alpha(EDGE, 0.0f), alpha(EDGE, edge));
		for (int step = 0; step < reach; step++) {
			float falloff = edge * (1.0f - step / (float) reach);
			graphics.fill(step, 0, step + 1, height, alpha(EDGE, falloff));
			graphics.fill(width - step - 1, 0, width - step, height, alpha(EDGE, falloff));
		}
		arcs(graphics, width, height, strength);
	}

	private void arcs(GuiGraphicsExtractor graphics, int width, int height, float strength) {
		RandomSource shape = RandomSource.create(seed);
		for (int arc = 0; arc < ARCS; arc++) {
			int y = shape.nextInt(height);
			int step = Math.max(8, width / 14);
			int at = y;
			for (int x = 0; x < width; x += step) {
				int next = at + shape.nextInt(25) - 12;
				int top = Math.min(at, next);
				int bottom = Math.max(at, next);
				graphics.fill(x, top, x + 2, bottom + 2, alpha(ARC, strength * 0.5f));
				at = next;
			}
		}
	}

	private static int alpha(int colour, float amount) {
		int a = (int) (Math.max(0.0f, Math.min(1.0f, amount)) * 255.0f);
		return (a << 24) | colour;
	}
}
