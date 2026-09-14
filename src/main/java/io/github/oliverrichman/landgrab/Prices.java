package io.github.oliverrichman.landgrab;

public final class Prices {
	private static final double BASE = 192 * Coins.PER_COIN;
	private static final double GROWTH = 1.12;

	private Prices() {
	}

	public static long nextChunk(int ownedCount) {
		int steps = Math.max(0, ownedCount - 1);
		long exact = Math.round(BASE * Math.pow(GROWTH, steps));
		return exact / Coins.PER_COIN * Coins.PER_COIN;
	}
}
