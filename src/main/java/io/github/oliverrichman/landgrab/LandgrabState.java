package io.github.oliverrichman.landgrab;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LandgrabState extends SavedData {
	public static final SavedDataType<LandgrabState> TYPE = new SavedDataType<>(
			Landgrab.id("state"),
			LandgrabState::new,
			codec(),
			DataFixTypes.LEVEL);

	public static final class Territory {
		private static final Codec<Territory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.LONG.listOf().fieldOf("owned").forGetter(territory -> List.copyOf(territory.owned)),
				Codec.LONG.fieldOf("origin").forGetter(territory -> territory.origin)
		).apply(instance, Territory::new));
		private final Set<Long> owned;
		private final long origin;

		private Territory(List<Long> owned, long origin) {
			this.owned = new HashSet<>(owned);
			this.origin = origin;
		}

		private Territory(ChunkPos start) {
			this.owned = new HashSet<>();
			this.owned.add(start.pack());
			this.origin = start.pack();
		}

		public boolean owns(ChunkPos pos) {
			return owned.contains(pos.pack());
		}

		public int size() {
			return owned.size();
		}

		public ChunkPos origin() {
			return ChunkPos.unpack(origin);
		}

		public long[] packed() {
			long[] out = new long[owned.size()];
			int index = 0;
			for (long value : owned) {
				out[index++] = value;
			}
			return out;
		}

		public boolean isAdjacentToOwned(ChunkPos pos) {
			return owns(new ChunkPos(pos.x() + 1, pos.z()))
					|| owns(new ChunkPos(pos.x() - 1, pos.z()))
					|| owns(new ChunkPos(pos.x(), pos.z() + 1))
					|| owns(new ChunkPos(pos.x(), pos.z() - 1));
		}
	}

	private final Map<ResourceKey<Level>, Territory> territories;
	private final Set<String> paidAdvancements;
	private long balance;

	private LandgrabState() {
		this.territories = new HashMap<>();
		this.paidAdvancements = new HashSet<>();
		this.balance = 0L;
	}

	private LandgrabState(Map<ResourceKey<Level>, Territory> territories, long balance,
			List<String> paidAdvancements) {
		this.territories = new HashMap<>(territories);
		this.balance = balance;
		this.paidAdvancements = new HashSet<>(paidAdvancements);
	}

	private static Codec<LandgrabState> codec() {
		return RecordCodecBuilder.create(instance -> instance.group(
				Codec.unboundedMap(ResourceKey.codec(Registries.DIMENSION), Territory.CODEC)
						.optionalFieldOf("territories", Map.of())
						.forGetter(state -> state.territories),
				Codec.LONG.optionalFieldOf("balance", 0L).forGetter(state -> state.balance),
				Codec.STRING.listOf().optionalFieldOf("paid_advancements", List.of())
						.forGetter(state -> List.copyOf(state.paidAdvancements))
		).apply(instance, LandgrabState::new));
	}

	public static LandgrabState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public Territory territory(ResourceKey<Level> dimension) {
		return territories.get(dimension);
	}

	public boolean hasTerritory(ResourceKey<Level> dimension) {
		return territories.containsKey(dimension);
	}

	public void seed(ResourceKey<Level> dimension, ChunkPos start) {
		if (territories.containsKey(dimension)) {
			return;
		}
		territories.put(dimension, new Territory(start));
		setDirty();
	}

	public boolean owns(ResourceKey<Level> dimension, ChunkPos pos) {
		Territory territory = territories.get(dimension);
		return territory != null && territory.owns(pos);
	}

	public void claim(ResourceKey<Level> dimension, ChunkPos pos) {
		Territory territory = territories.get(dimension);
		if (territory == null) {
			territories.put(dimension, new Territory(pos));
		} else {
			territory.owned.add(pos.pack());
		}
		setDirty();
	}

	public boolean claimAdvancement(Identifier advancement) {
		if (!paidAdvancements.add(advancement.toString())) {
			return false;
		}
		setDirty();
		return true;
	}

	public long balance() {
		return balance;
	}

	public void credit(long amount) {
		balance += amount;
		setDirty();
	}

	public boolean debit(long amount) {
		if (amount > balance) {
			return false;
		}
		balance -= amount;
		setDirty();
		return true;
	}
}
