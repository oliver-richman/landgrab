package io.github.oliverrichman.landgrab;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class FogChunk {
	private static final BlockState FOG = Blocks.CONCRETE.lightGray().defaultBlockState();
	private static final BlockState HAZE = LandgrabBlocks.HAZE.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final int SIDE_WEST = 1;
	private static final int SIDE_EAST = 2;
	private static final int SIDE_NORTH = 4;
	private static final int SIDE_SOUTH = 8;

	private FogChunk() {
	}

	public static BlockState fog() {
		return FOG;
	}

	public static int borderingSides(ServerPlayer viewer, ChunkPos pos) {
		int sides = 0;
		if (ChunkVisibility.sees(viewer, new ChunkPos(pos.x() - 1, pos.z()))) {
			sides |= SIDE_WEST;
		}
		if (ChunkVisibility.sees(viewer, new ChunkPos(pos.x() + 1, pos.z()))) {
			sides |= SIDE_EAST;
		}
		if (ChunkVisibility.sees(viewer, new ChunkPos(pos.x(), pos.z() - 1))) {
			sides |= SIDE_NORTH;
		}
		if (ChunkVisibility.sees(viewer, new ChunkPos(pos.x(), pos.z() + 1))) {
			sides |= SIDE_SOUTH;
		}
		return sides;
	}

	public static BlockState stateFor(BlockState real, int sides, int localX, int localZ) {
		if (sides == 0) {
			return FOG;
		}

		if (onFace(sides, localX, localZ)) {
			return HAZE;
		}
		return real.isAir() ? HAZE : real;
	}

	private static boolean onFace(int sides, int localX, int localZ) {
		return ((sides & SIDE_WEST) != 0 && localX == 0)
				|| ((sides & SIDE_EAST) != 0 && localX == 15)
				|| ((sides & SIDE_NORTH) != 0 && localZ == 0)
				|| ((sides & SIDE_SOUTH) != 0 && localZ == 15);
	}

	public static LevelChunk of(ServerLevel level, LevelChunk source, int sides) {
		LevelChunk fogged = new LevelChunk(level, source.getPos());
		LevelChunkSection[] from = source.getSections();
		LevelChunkSection[] to = fogged.getSections();
		for (int index = 0; index < from.length && index < to.length; index++) {
			boolean empty = from[index].hasOnlyAir();
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState real = empty ? AIR : from[index].getBlockState(x, y, z);
						to[index].setBlockState(x, y, z, stateFor(real, sides, x, z), false);
					}
				}
			}
		}
		return fogged;
	}
}
