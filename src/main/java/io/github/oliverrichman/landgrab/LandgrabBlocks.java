package io.github.oliverrichman.landgrab;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class LandgrabBlocks {
	public static final ResourceKey<Block> HAZE_KEY =
			ResourceKey.create(Registries.BLOCK, Landgrab.id("haze"));
	public static final Block HAZE = new HalfTransparentBlock(BlockBehaviour.Properties.of()
			.setId(HAZE_KEY)
			.mapColor(MapColor.COLOR_BLUE)
			.sound(SoundType.GLASS)
			.strength(-1.0f, 3600000.0f)
			.noOcclusion());

	private LandgrabBlocks() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK, HAZE_KEY, HAZE);
	}
}
