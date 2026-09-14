package io.github.oliverrichman.landgrab;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class LandgrabRules {
	public static final GameRule<Boolean> MODE = GameRuleBuilder.forBoolean(false)
			.category(GameRuleCategory.MISC)
			.buildAndRegister(Landgrab.id("landgrab"));

	private LandgrabRules() {
	}

	public static void register() {
	}
}
