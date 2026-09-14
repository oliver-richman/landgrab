package io.github.oliverrichman.landgrab.mixin;

import io.github.oliverrichman.landgrab.ChunkVisibility;
import io.github.oliverrichman.landgrab.FogChunk;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {
	@Inject(method = "sendChunk", at = @At("HEAD"), cancellable = true)
	private static void landgrab$obscureUnownedChunk(ServerGamePacketListenerImpl connection,
			ServerLevel level, LevelChunk chunk, CallbackInfo info) {
		ServerPlayer player = connection.getPlayer();
		if (ChunkVisibility.sees(player, chunk.getPos())) {
			return;
		}
		LevelChunk substitute = ChunkVisibility.nearOwned(player, chunk.getPos())
				? FogChunk.of(level, chunk, FogChunk.borderingSides(player, chunk.getPos()))
				: new LevelChunk(level, chunk.getPos());
		connection.send(new ClientboundLevelChunkWithLightPacket(
				substitute, level.getLightEngine(), null, null));
		info.cancel();
	}
}
