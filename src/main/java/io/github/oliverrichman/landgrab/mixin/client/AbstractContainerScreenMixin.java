package io.github.oliverrichman.landgrab.mixin.client;

import io.github.oliverrichman.landgrab.client.PhoneClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	private boolean landgrab$phoneUp() {
		return (Object) this instanceof InventoryScreen && PhoneClient.isOpen();
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void landgrab$phoneScroll(double mouseX, double mouseY, double deltaX, double deltaY,
			CallbackInfoReturnable<Boolean> info) {
		if (landgrab$phoneUp() && PhoneClient.panel().contains(mouseX, mouseY)
				&& PhoneClient.panel().mouseScrolled(deltaY)) {
			info.setReturnValue(true);
		}
	}

	@Inject(method = "removed", at = @At("TAIL"))
	private void landgrab$putPhoneAway(CallbackInfo info) {
		if ((Object) this instanceof InventoryScreen) {
			PhoneClient.close();
		}
	}
}
