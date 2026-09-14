package io.github.oliverrichman.landgrab.mixin.client;

import io.github.oliverrichman.landgrab.client.PhoneClient;
import io.github.oliverrichman.landgrab.client.PhonePanel;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	private boolean landgrab$phoneUp() {
		return (Object) this instanceof InventoryScreen && PhoneClient.isOpen();
	}

	@Redirect(
			method = "init",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;updateScreenPosition(II)I"))
	private int landgrab$makeRoomForPhone(RecipeBookComponent<?> component, int width, int imageWidth) {
		int vanilla = component.updateScreenPosition(width, imageWidth);
		if (landgrab$phoneUp()) {
			return (width - imageWidth - PhonePanel.WIDTH) / 2 + PhonePanel.WIDTH;
		}
		return vanilla;
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void landgrab$phoneClick(MouseButtonEvent event, boolean doubled,
			CallbackInfoReturnable<Boolean> info) {
		if (landgrab$phoneUp() && PhoneClient.panel().contains(event.x(), event.y())) {
			PhoneClient.panel().mouseClicked(event.x(), event.y(), event.button());
			info.setReturnValue(true);
		}
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void landgrab$phoneDrag(MouseButtonEvent event, double dragX, double dragY,
			CallbackInfoReturnable<Boolean> info) {
		if (landgrab$phoneUp() && PhoneClient.panel().contains(event.x(), event.y())
				&& PhoneClient.panel().mouseDragged(dragX, dragY)) {
			info.setReturnValue(true);
		}
	}

	@Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
	private void landgrab$phoneIsNotOutside(double mouseX, double mouseY, int left, int top,
			CallbackInfoReturnable<Boolean> info) {
		if (landgrab$phoneUp() && PhoneClient.panel().contains(mouseX, mouseY)) {
			info.setReturnValue(false);
		}
	}
}
