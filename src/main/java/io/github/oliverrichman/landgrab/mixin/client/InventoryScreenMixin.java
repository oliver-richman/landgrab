package io.github.oliverrichman.landgrab.mixin.client;

import io.github.oliverrichman.landgrab.Landgrab;
import io.github.oliverrichman.landgrab.client.ClientState;
import io.github.oliverrichman.landgrab.client.PhoneClient;
import io.github.oliverrichman.landgrab.client.PhoneHost;
import io.github.oliverrichman.landgrab.client.PhonePanel;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends Screen implements PhoneHost {
	private static final int BUTTON_GAP = 22;
	@Shadow
	protected abstract ScreenPosition getRecipeBookButtonPosition();

	@Unique
	private SpriteIconButton landgrab$phoneButton;

	private InventoryScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void landgrab$addPhone(CallbackInfo info) {
		if (!ClientState.isActive()) {
			return;
		}
		ContainerScreenAccess panel = (ContainerScreenAccess) this;
		PhoneClient.panel().setPosition(
				panel.landgrab$leftPos() - PhonePanel.WIDTH - 2, panel.landgrab$topPos());
		ScreenPosition beside = getRecipeBookButtonPosition();
		landgrab$phoneButton = SpriteIconButton.builder(
						Component.translatable("landgrab.phone.open"),
						pressed -> landgrab$togglePhone(), true)
				.sprite(Landgrab.id("phone"), 16, 16)
				.size(20, 18)
				.build();
		landgrab$phoneButton.setPosition(beside.x() + BUTTON_GAP, beside.y());
		Screens.getWidgets(this).add(landgrab$phoneButton);
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void landgrab$drawPhone(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick, CallbackInfo info) {
		if (PhoneClient.isOpen()) {
			PhoneClient.panel().render(graphics, mouseX, mouseY);
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void landgrab$drawUnreadDot(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick, CallbackInfo info) {
		if (landgrab$phoneButton != null && ClientState.hasUnread()) {
			PhonePanel.unreadDot(graphics,
					landgrab$phoneButton.getX() + 14, landgrab$phoneButton.getY() - 1);
		}
	}

	@Inject(method = "onRecipeBookButtonClick", at = @At("TAIL"))
	private void landgrab$followRecipeBook(CallbackInfo info) {
		if (PhoneClient.isOpen()) {
			PhoneClient.close();
			rebuildWidgets();
			return;
		}
		if (landgrab$phoneButton != null) {
			ScreenPosition beside = getRecipeBookButtonPosition();
			landgrab$phoneButton.setPosition(beside.x() + BUTTON_GAP, beside.y());
		}
	}

	@Override
	public void landgrab$togglePhone() {
		if (!ClientState.isActive()) {
			return;
		}
		if (PhoneClient.isOpen()) {
			PhoneClient.close();
		} else {
			RecipeBookComponent<?> book = ((RecipeBookAccess) this).landgrab$recipeBook();
			if (book.isVisible()) {
				book.toggleVisibility();
			}
			PhoneClient.open();
		}
		rebuildWidgets();
	}
}
