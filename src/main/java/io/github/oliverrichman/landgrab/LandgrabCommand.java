package io.github.oliverrichman.landgrab;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class LandgrabCommand {
	private LandgrabCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("landgrab")
						.executes(LandgrabCommand::status)
						.then(Commands.literal("status").executes(LandgrabCommand::status))
						.then(Commands.literal("pay")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("amount", LongArgumentType.longArg(1))
										.executes(LandgrabCommand::pay)))
						.then(Commands.literal("grant")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("x", IntegerArgumentType.integer())
										.then(Commands.argument("z", IntegerArgumentType.integer())
												.executes(LandgrabCommand::grant))))));
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (!Landgrab.isActive()) {
			source.sendFailure(Component.translatable("landgrab.command.inactive"));
			return 0;
		}
		LandgrabState state = LandgrabState.get(source.getServer());
		ResourceKey<Level> dimension = source.getLevel().dimension();
		LandgrabState.Territory territory = state.territory(dimension);
		if (territory == null) {
			source.sendSuccess(() -> Component.translatable("landgrab.command.unopened",
					dimension.identifier().toString(), Coins.of(state.balance())), false);
			return 0;
		}
		ChunkPos origin = territory.origin();
		source.sendSuccess(() -> Component.translatable("landgrab.command.status",
				dimension.identifier().toString(),
				Landgrab.chunks(territory.size()),
				Coins.of(state.balance()),
				Coins.of(Prices.nextChunk(territory.size())),
				origin.x(),
				origin.z()), false);
		return territory.size();
	}

	private static int pay(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (!Landgrab.isActive()) {
			source.sendFailure(Component.translatable("landgrab.command.inactive"));
			return 0;
		}
		long amount = LongArgumentType.getLong(context, "amount") * Coins.PER_COIN;
		MinecraftServer server = source.getServer();
		LandgrabState.get(server).credit(amount);
		Landgrab.syncAll(server);
		source.sendSuccess(() -> Component.translatable("landgrab.command.paid", Coins.of(amount)), true);
		return 1;
	}

	private static int grant(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		if (!Landgrab.isActive()) {
			source.sendFailure(Component.translatable("landgrab.command.inactive"));
			return 0;
		}
		ChunkPos target = new ChunkPos(
				IntegerArgumentType.getInteger(context, "x"),
				IntegerArgumentType.getInteger(context, "z"));
		ResourceKey<Level> dimension = source.getLevel().dimension();
		MinecraftServer server = source.getServer();
		LandgrabState.get(server).claim(dimension, target);
		ChunkVisibility.reveal(server, dimension, target);
		Landgrab.syncAll(server);
		source.sendSuccess(() -> Component.translatable("landgrab.command.granted",
				target.x(), target.z(), dimension.identifier().toString()), true);
		return 1;
	}
}
