package io.github.oliverrichman.landgrab;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Containment {
	private static final float DAMAGE = 1.0f;
	private static final int DAMAGE_INTERVAL_TICKS = 10;
	private static final double KNOCKBACK = 0.42;
	private static final double KNOCKBACK_LIFT = 0.24;
	private static final double TOUCH_REACH = 0.42;
	private static final Map<UUID, Vec3> LAST_INSIDE = new HashMap<>();
	private static final Map<UUID, Integer> LAST_HURT_TICK = new HashMap<>();
	private static final Map<UUID, Boolean> LAST_UNRESTRICTED = new HashMap<>();

	private Containment() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Containment::tick);

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) ->
				mayTouch(player, pos));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				mayTouch(player, pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!mayTouch(player, hit.getBlockPos())) {
				return InteractionResult.FAIL;
			}
			BlockPos placed = placementOf(player, hand, hit);
			if (placed != null && !mayTouch(player, placed)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
	}

	private static BlockPos placementOf(Player player, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof BlockItem) {
			return new BlockPlaceContext(player, hand, stack, hit).getClickedPos();
		}
		if (stack.getItem() instanceof BucketItem) {
			return hit.getBlockPos().relative(hit.getDirection());
		}
		return null;
	}

	private static boolean exempt(Player player) {
		return player.isCreative() || player.isSpectator() || player.isDeadOrDying();
	}

	private static boolean mayTouch(Player player, BlockPos pos) {
		if (!Landgrab.isActive() || player.level().isClientSide() || exempt(player)) {
			return true;
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return true;
		}
		LandgrabState state = LandgrabState.get(server);
		LandgrabState.Territory territory = state.territory(player.level().dimension());
		if (territory == null || territory.owns(ChunkPos.containing(pos))) {
			return true;
		}

		((ServerPlayer) player).sendSystemMessage(
				Component.translatable("landgrab.message.not_yours"), true);
		return false;
	}

	private static void tick(MinecraftServer server) {
		if (!Landgrab.isActive()) {
			return;
		}
		LandgrabState state = LandgrabState.get(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();
			boolean free = ChunkVisibility.unrestricted(player);
			Boolean was = LAST_UNRESTRICTED.put(id, free);
			if (was != null && was != free) {
				ChunkVisibility.refresh(player);
			}
			LandgrabState.Territory territory = state.territory(player.level().dimension());
			if (territory == null) {
				ChunkPos start = ChunkPos.containing(player.blockPosition());
				state.seed(player.level().dimension(), start);
				ChunkVisibility.reveal(server, player.level().dimension(), start);
				Landgrab.sync(player);
				continue;
			}

			if (exempt(player)) {
				LAST_INSIDE.remove(id);
				continue;
			}
			if (territory.owns(ChunkPos.containing(player.blockPosition()))) {
				LAST_INSIDE.put(id, player.position());
				touchFence(player, territory, server.getTickCount());
				continue;
			}
			Vec3 back = LAST_INSIDE.get(id);
			if (back == null) {

				back = groundIn(player.level(),
						nearestOwned(territory, ChunkPos.containing(player.blockPosition())));
			}
			repel(player, back, server.getTickCount());
		}
	}

	private static void touchFence(ServerPlayer player, LandgrabState.Territory territory,
			int tickCount) {
		ChunkPos here = ChunkPos.containing(player.blockPosition());
		double west = here.getMinBlockX();
		double north = here.getMinBlockZ();
		double x = player.getX();
		double z = player.getZ();
		double inwardX = 0.0;
		double inwardZ = 0.0;
		if (!territory.owns(new ChunkPos(here.x() - 1, here.z())) && x - west < TOUCH_REACH) {
			inwardX = 1.0;
		} else if (!territory.owns(new ChunkPos(here.x() + 1, here.z()))
				&& west + 16.0 - x < TOUCH_REACH) {
			inwardX = -1.0;
		}
		if (!territory.owns(new ChunkPos(here.x(), here.z() - 1)) && z - north < TOUCH_REACH) {
			inwardZ = 1.0;
		} else if (!territory.owns(new ChunkPos(here.x(), here.z() + 1))
				&& north + 16.0 - z < TOUCH_REACH) {
			inwardZ = -1.0;
		}
		if (inwardX == 0.0 && inwardZ == 0.0) {
			return;
		}
		shock(player, new Vec3(inwardX, 0.0, inwardZ).normalize(), tickCount);
	}

	private static void shock(ServerPlayer player, Vec3 inward, int tickCount) {
		player.setDeltaMovement(inward.x * KNOCKBACK, KNOCKBACK_LIFT, inward.z * KNOCKBACK);
		player.hurtMarked = true;
		player.resetFallDistance();
		Integer last = LAST_HURT_TICK.get(player.getUUID());
		if (last != null && tickCount - last < DAMAGE_INTERVAL_TICKS) {
			return;
		}
		LAST_HURT_TICK.put(player.getUUID(), tickCount);
		ServerLevel level = player.level();
		DamageSource source = new DamageSource(level.registryAccess()
				.lookupOrThrow(Registries.DAMAGE_TYPE)
				.getOrThrow(Landgrab.BOUNDARY_DAMAGE));
		player.invulnerableTime = 0;
		player.hurtServer(level, source, DAMAGE);
		ServerPlayNetworking.send(player, Payloads.Shock.INSTANCE);
		level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(),
				SoundSource.PLAYERS, 0.4f, 1.6f);
	}

	private static void repel(ServerPlayer player, Vec3 to, int tickCount) {
		Vec3 inward = to.subtract(player.position());
		inward = inward.horizontalDistanceSqr() < 1.0e-4
				? Vec3.ZERO
				: inward.normalize();
		player.teleportTo(player.level(), to.x, to.y, to.z, Set.of(), player.getYRot(), player.getXRot(), false);
		shock(player, inward, tickCount);
	}

	private static ChunkPos nearestOwned(LandgrabState.Territory territory, ChunkPos from) {
		ChunkPos best = territory.origin();
		long bestDistance = Long.MAX_VALUE;
		for (long packed : territory.packed()) {
			ChunkPos candidate = ChunkPos.unpack(packed);
			long dx = candidate.x() - (long) from.x();
			long dz = candidate.z() - (long) from.z();
			long distance = dx * dx + dz * dz;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = candidate;
			}
		}
		return best;
	}

	private static Vec3 groundIn(Level level, ChunkPos chunk) {
		BlockPos middle = new BlockPos(chunk.getMiddleBlockX(), 0, chunk.getMiddleBlockZ());
		BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, middle);
		int y = Math.max(surface.getY(), level.getSeaLevel());
		return new Vec3(surface.getX() + 0.5, y, surface.getZ() + 0.5);
	}

	public static void forget(UUID player) {
		LAST_INSIDE.remove(player);
		LAST_HURT_TICK.remove(player);
		LAST_UNRESTRICTED.remove(player);
	}
}
