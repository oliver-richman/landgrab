package io.github.oliverrichman.landgrab.mixin;

import io.github.oliverrichman.landgrab.Bounties;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;
	@Shadow
	public abstract net.minecraft.advancements.AdvancementProgress getOrStartProgress(AdvancementHolder holder);
	@Inject(method = "award", at = @At("RETURN"))
	private void landgrab$payBounty(AdvancementHolder holder, String criterion,
			CallbackInfoReturnable<Boolean> info) {
		if (!info.getReturnValueZ() || player == null) {
			return;
		}
		if (getOrStartProgress(holder).isDone()) {
			Bounties.pay(player, holder);
		}
	}
}
