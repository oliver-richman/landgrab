package io.github.oliverrichman.landgrab.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractRecipeBookScreen.class)
public interface RecipeBookAccess {
	@Accessor("recipeBookComponent")
	RecipeBookComponent<?> landgrab$recipeBook();
}
