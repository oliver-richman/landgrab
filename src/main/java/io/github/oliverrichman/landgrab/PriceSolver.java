package io.github.oliverrichman.landgrab;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PriceSolver {
	private static final int PASSES = 12;
	private static final int SCALE = Coins.PER_COIN;
	private static final float LITTER = 0.5f;
	private static final float SOFT = 1.0f;

	private PriceSolver() {
	}

	public static Map<Item, Integer> solve(MinecraftServer server) {
		Map<Item, Integer> prices = new HashMap<>();
		List<Made> made = collect(server);
		Seeds.into(prices);
		relax(made, prices, Set.of());
		Set<Item> settled = Set.copyOf(prices.keySet());
		Set<Item> craftable = new HashSet<>();
		for (Made recipe : made) {
			craftable.add(recipe.result());
		}
		for (Item item : BuiltInRegistries.ITEM) {
			if (!craftable.contains(item)) {
				prices.putIfAbsent(item, guess(item));
			}
		}

		relax(made, prices, settled);

		for (Item item : BuiltInRegistries.ITEM) {
			prices.putIfAbsent(item, guess(item));
		}
		relax(made, prices, settled);

		Set<Item> fixed = new HashSet<>(settled);
		fixed.addAll(craftable);
		lift(made, prices, fixed);

		Map<Item, Integer> coins = new HashMap<>();
		prices.forEach((item, price) -> {
			int rounded = (int) Coins.floorToQuarter(price);
			if (rounded > 0) {
				coins.put(item, rounded);
			}
		});
		coins.remove(net.minecraft.world.item.Items.AIR);
		return coins;
	}

	private static void relax(List<Made> made, Map<Item, Integer> prices, Set<Item> frozen) {
		for (int pass = 0; pass < PASSES; pass++) {
			boolean changed = false;
			for (Made recipe : made) {
				if (frozen.contains(recipe.result())) {
					continue;
				}
				int cost = recipe.cost(prices);
				if (cost < 0) {
					continue;
				}
				int each = cost / recipe.count();
				Integer current = prices.get(recipe.result());
				if (current == null || each < current) {
					prices.put(recipe.result(), each);
					changed = true;
				}
			}
			if (!changed) {
				return;
			}
		}
	}

	private static void lift(List<Made> made, Map<Item, Integer> prices, Set<Item> frozen) {
		for (int pass = 0; pass < PASSES; pass++) {
			boolean changed = false;
			for (Made recipe : made) {
				if (recipe.inputs().size() != 1) {
					continue;
				}
				Integer result = prices.get(recipe.result());
				if (result == null) {
					continue;
				}
				int worth = result * recipe.count();
				for (Item choice : recipe.inputs().getFirst()) {
					if (frozen.contains(choice)) {
						continue;
					}
					Integer current = prices.get(choice);
					if (current == null || current < worth) {
						prices.put(choice, worth);
						changed = true;
					}
				}
			}
			if (!changed) {
				return;
			}
		}
	}

	private static int guess(Item item) {
		int stack = item.getDefaultMaxStackSize();
		int coins = stack >= 64 ? 4 : stack > 1 ? 16 : 48;
		Rarity rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
		coins *= switch (rarity) {
			case COMMON -> 1;
			case UNCOMMON -> 3;
			case RARE -> 8;
			case EPIC -> 20;
		};
		FoodProperties food = item.components().get(DataComponents.FOOD);
		if (food != null) {
			coins += food.nutrition();
		}
		return Math.max(1, effort(item, coins * SCALE));
	}

	private static int effort(Item item, int price) {
		if (!(item instanceof BlockItem placeable)) {
			return price;
		}
		Block block = placeable.getBlock();
		if (block.defaultBlockState().requiresCorrectToolForDrops()) {
			return price;
		}
		float hardness = block.defaultDestroyTime();
		if (hardness < LITTER) {
			return price / 4;
		}
		if (hardness < SOFT) {
			return price / 2;
		}
		return price;
	}

	private record Made(Item result, int count, List<List<Item>> inputs) {
		int cost(Map<Item, Integer> prices) {
			int total = 0;
			for (List<Item> choices : inputs) {
				int cheapest = -1;
				for (Item choice : choices) {
					Integer price = prices.get(choice);
					if (price != null && (cheapest < 0 || price < cheapest)) {
						cheapest = price;
					}
				}
				if (cheapest < 0) {
					return -1;
				}
				total += cheapest;
			}
			return total;
		}
	}

	@SuppressWarnings("deprecation")
	private static List<Made> collect(MinecraftServer server) {
		List<Made> made = new ArrayList<>();
		for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
			Recipe<?> recipe = holder.value();
			ItemStackTemplate result = resultOf(recipe);
			if (result == null || result.count() <= 0) {
				continue;
			}

			List<Ingredient> ingredients;
			if (recipe instanceof SingleItemRecipe single) {
				ingredients = List.of(single.input());
			} else if (recipe instanceof SmithingRecipe smithing) {
				ingredients = new ArrayList<>();
				smithing.templateIngredient().ifPresent(ingredients::add);
				ingredients.add(smithing.baseIngredient());
				smithing.additionIngredient().ifPresent(ingredients::add);
			} else {
				ingredients = recipe.placementInfo().ingredients();
			}
			List<List<Item>> inputs = new ArrayList<>();
			boolean usable = true;
			for (Ingredient ingredient : ingredients) {
				List<Item> choices = ingredient.items().map(holder2 -> holder2.value()).toList();
				if (choices.isEmpty()) {
					usable = false;
					break;
				}
				inputs.add(choices);
			}
			if (usable && !inputs.isEmpty()) {
				made.add(new Made(result.item().value(), result.count(), inputs));
			}
		}
		return made;
	}

	private static ItemStackTemplate resultOf(Recipe<?> recipe) {
		for (RecipeDisplay display : recipe.display()) {
			if (display.result() instanceof SlotDisplay.ItemStackSlotDisplay stack) {
				return stack.stack();
			}
		}
		return null;
	}

	private static final class Seeds {
		private static final Map<String, Integer> VALUES = new LinkedHashMap<>();

		static {

			put(1, "dirt", "coarse_dirt", "rooted_dirt", "sand", "red_sand", "gravel",
					"netherrack", "cobblestone", "cobbled_deepslate", "andesite", "diorite",
					"granite", "tuff", "bamboo", "kelp", "seagrass", "short_grass", "fern");
			put(2, "clay_ball", "sugar_cane", "cactus", "sweet_berries", "melon_slice",
					"brown_mushroom", "red_mushroom", "soul_sand", "basalt", "snowball");

			put(3, "flint", "feather", "egg", "cocoa_beans", "glow_berries", "carrot",
					"potato", "beetroot", "wheat_seeds", "resin_clump");
			put(4, "wheat", "string", "cod", "white_wool", "rabbit_hide");
			put(2, "oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log",
					"dark_oak_log", "mangrove_log", "cherry_log", "pale_oak_log");
			put(8, "bone", "salmon", "pumpkin", "apple");
			put(12, "coal", "ink_sac");
			put(16, "raw_copper", "redstone", "leather", "honeycomb", "nether_wart",
					"spider_eye");
			put(20, "lapis_lazuli", "quartz", "glowstone_dust", "slime_ball");
			put(48, "raw_iron", "gunpowder", "prismarine_shard", "pufferfish", "glow_ink_sac");
			put(56, "amethyst_shard", "tropical_fish");
			put(60, "prismarine_crystals");
			put(80, "raw_gold", "obsidian");
			put(120, "phantom_membrane", "rabbit_foot");
			put(160, "ender_pearl");
			put(180, "blaze_rod");
			put(240, "nautilus_shell", "disc_fragment_5");
			put(320, "ghast_tear");
			put(400, "netherite_upgrade_smithing_template");
			put(360, "emerald");
			put(400, "dragon_breath");
			put(480, "diamond", "echo_shard");
			put(600, "shulker_shell");
			put(1600, "heart_of_the_sea");
			put(2000, "ancient_debris", "totem_of_undying");
			put(2400, "enchanted_golden_apple");
			put(4000, "nether_star");
			put(8000, "elytra");
		}

		private Seeds() {
		}

		private static void put(int value, String... ids) {
			for (String id : ids) {
				VALUES.put(id, value);
			}
		}

		static void into(Map<Item, Integer> prices) {
			VALUES.forEach((id, value) -> BuiltInRegistries.ITEM
					.get(Identifier.withDefaultNamespace(id))
					.ifPresent(item -> prices.put(item.value(), value * SCALE)));
		}
	}
}
