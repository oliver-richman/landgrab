package io.github.oliverrichman.landgrab.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;

public final class BoundarySparks {
	private static final DustParticleOptions[] SPARKS = {
			new DustParticleOptions(0x4FE3FF, 0.45f),
			new DustParticleOptions(0x8FF2FF, 0.60f),
			new DustParticleOptions(0x28C4FF, 0.80f),
			new DustParticleOptions(0xBFF7FF, 0.35f),
	};
	private static final double RANGE = 16.0;
	private static final double HEIGHT_SPREAD = 9.0;
	private static final int PER_EDGE = 9;
	private static final float CRACKLE = 0.02f;
	private static final double CRACKLE_RANGE = 6.0;
	private static final RandomSource RANDOM = RandomSource.create();

	private BoundarySparks() {
	}

	public static void tick(Minecraft client) {
		if (!ClientState.isActive() || client.level == null || client.player == null) {
			return;
		}
		ChunkPos here = ChunkPos.containing(client.player.blockPosition());
		double eyeY = client.player.getY() + 1.0;
		int reach = (int) Math.ceil(RANGE / 16.0) + 1;
		for (int offsetX = -reach; offsetX <= reach; offsetX++) {
			for (int offsetZ = -reach; offsetZ <= reach; offsetZ++) {
				int chunkX = here.x() + offsetX;
				int chunkZ = here.z() + offsetZ;
				if (!ClientState.owns(chunkX, chunkZ)) {
					continue;
				}
				double westX = chunkX * 16.0;
				double northZ = chunkZ * 16.0;
				if (!ClientState.owns(chunkX - 1, chunkZ)) {
					edge(client, westX, northZ, 0.0, 1.0, eyeY);
				}
				if (!ClientState.owns(chunkX + 1, chunkZ)) {
					edge(client, westX + 16.0, northZ, 0.0, 1.0, eyeY);
				}
				if (!ClientState.owns(chunkX, chunkZ - 1)) {
					edge(client, westX, northZ, 1.0, 0.0, eyeY);
				}
				if (!ClientState.owns(chunkX, chunkZ + 1)) {
					edge(client, westX, northZ + 16.0, 1.0, 0.0, eyeY);
				}
			}
		}
	}

	private static void edge(Minecraft client, double startX, double startZ,
			double stepX, double stepZ, double eyeY) {
		for (int spark = 0; spark < PER_EDGE; spark++) {
			double along = RANDOM.nextDouble() * 16.0;
			double x = startX + stepX * along;
			double z = startZ + stepZ * along;
			double y = eyeY + (RANDOM.nextDouble() + RANDOM.nextDouble() - 1.0) * HEIGHT_SPREAD;
			double away = client.player.distanceToSqr(x, y, z);
			if (away > RANGE * RANGE) {
				continue;
			}
			client.level.addParticle(SPARKS[RANDOM.nextInt(SPARKS.length)], x, y, z, 0.0, 0.0, 0.0);
			if (away < CRACKLE_RANGE * CRACKLE_RANGE && RANDOM.nextFloat() < CRACKLE) {
				client.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.0, 0.0);
			}
		}
	}
}
