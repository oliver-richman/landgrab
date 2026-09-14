package io.github.oliverrichman.landgrab.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
	@Accessor("leftPos")
	int landgrab$leftPos();
	@Accessor("topPos")
	int landgrab$topPos();
}
