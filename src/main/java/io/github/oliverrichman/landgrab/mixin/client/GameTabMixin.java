package io.github.oliverrichman.landgrab.mixin.client;

import io.github.oliverrichman.landgrab.LandgrabRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class GameTabMixin extends GridLayoutTab {
	private GameTabMixin(Component title) {
		super(title);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void landgrab$addModeToggle(CreateWorldScreen screen, CallbackInfo info) {
		Checkbox box = Checkbox.builder(
						Component.translatable("landgrab.create.enable"),
						Minecraft.getInstance().font)
				.selected(screen.getUiState().getGameRules().get(LandgrabRules.MODE))
				.tooltip(Tooltip.create(Component.translatable("landgrab.create.enable.tooltip")))
				.onValueChange((checkbox, value) ->
						screen.getUiState().getGameRules().set(LandgrabRules.MODE, value, null))
				.build();

		int[] rows = {0};
		layout.visitChildren(child -> rows[0]++);
		layout.addChild(box, rows[0], 0);
	}
}
