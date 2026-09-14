package io.github.oliverrichman.landgrab.mixin;

import io.github.oliverrichman.landgrab.ChunkVisibility;
import io.github.oliverrichman.landgrab.FogChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class PacketFilterMixin {
	@Unique
	private boolean landgrab$substituting;
	@Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void landgrab$hideBlocksBeyondTheBorder(Packet<?> packet, CallbackInfo info) {
		if (landgrab$substituting) {
			return;
		}
		if (!((Object) this instanceof ServerGamePacketListenerImpl connection)) {
			return;
		}
		ServerPlayer player = connection.getPlayer();
		if (player == null) {
			return;
		}
		if (packet instanceof ClientboundBlockUpdatePacket update) {

			ChunkPos pos = ChunkPos.containing(update.getPos());
			if (ChunkVisibility.sees(player, pos)) {
				return;
			}
			info.cancel();
			landgrab$substituting = true;
			try {
				connection.send(new ClientboundBlockUpdatePacket(update.getPos(),
						landgrab$disguise(player, update.getPos(), pos)));
			} finally {
				landgrab$substituting = false;
			}
			return;
		}
		if (packet instanceof ClientboundSectionBlocksUpdatePacket section2
				&& !ChunkVisibility.sees(player,
						((SectionBlocksUpdateAccess) section2).landgrab$sectionPos().chunk())) {
			info.cancel();
		}
	}

	@Unique
	private BlockState landgrab$disguise(ServerPlayer player, BlockPos block, ChunkPos pos) {
		LevelChunk chunk = player.level().getChunkSource().getChunkNow(pos.x(), pos.z());
		if (chunk == null) {
			return FogChunk.fog();
		}
		return FogChunk.stateFor(chunk.getBlockState(block), FogChunk.borderingSides(player, pos),
				block.getX() - pos.getMinBlockX(), block.getZ() - pos.getMinBlockZ());
	}
}
