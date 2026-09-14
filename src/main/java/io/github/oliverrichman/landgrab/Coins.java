package io.github.oliverrichman.landgrab;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

import java.util.Locale;

public final class Coins {
	private static final FontDescription FONT = new FontDescription.Resource(Landgrab.id("coin"));
	private static final String GLYPH = String.valueOf((char) 0xE000);
	public static final int PER_COIN = 1000;
	public static final int QUARTER_COIN = PER_COIN / 4;

	public static long floorToQuarter(long amount) {
		return amount / QUARTER_COIN * QUARTER_COIN;
	}

	private Coins() {
	}

	public static Component of(long amount) {
		return Component.empty()
				.append(Component.literal(GLYPH).withStyle(style -> style.withFont(FONT)))
				.append(Component.literal(" " + format(amount)));
	}

	public static String format(long amount) {
		long hundredths = Math.round(amount / (PER_COIN / 100.0));
		if (hundredths == 0L && amount != 0L) {
			hundredths = amount > 0L ? 1L : -1L;
		}
		long whole = hundredths / 100L;
		long fraction = Math.abs(hundredths % 100L);
		String digits = String.format(Locale.ROOT, "%,d", whole);
		if (hundredths < 0L && whole == 0L) {
			digits = "-" + digits;
		}
		if (fraction == 0L) {
			return digits;
		}
		if (fraction % 10L == 0L) {
			return digits + "." + (fraction / 10L);
		}
		return digits + "." + (fraction < 10L ? "0" : "") + fraction;
	}
}
