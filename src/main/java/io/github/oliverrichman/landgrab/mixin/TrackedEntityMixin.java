package io.github.oliverrichman.landgrab.mixin;

import io.github.oliverrichman.landgrab.ChunkVisibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {
	@Shadow
	@Final
	private Entity entity;
	@Shadow
	public abstract void removePlayer(ServerPlayer player);
	@Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
	private void landgrab$hideBeyondTheMargin(ServerPlayer player, CallbackInfo info) {
		if (entity == player) {
			return;
		}
		ChunkPos pos = ChunkPos.containing(entity.blockPosition());
		if (ChunkVisibility.sees(player, pos) || ChunkVisibility.nearOwned(player, pos)) {
			return;
		}
		removePlayer(player);
		info.cancel();
	}
}
