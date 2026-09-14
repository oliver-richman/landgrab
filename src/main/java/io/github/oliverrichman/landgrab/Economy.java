package io.github.oliverrichman.landgrab;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.Map;

public final class Economy {
	private static final Map<Item, Integer> PRICES = new HashMap<>();
	private static final long PER_LEVEL = 30L * Coins.PER_COIN;
	private static final int COMMONEST = 10;

	private Economy() {
	}

	public static void set(Map<Item, Integer> prices) {
		PRICES.clear();
		PRICES.putAll(prices);
	}

	public static Map<Item, Integer> all() {
		return PRICES;
	}

	public static int unitValue(Item item) {
		return PRICES.getOrDefault(item, 0);
	}

	public static long stackValue(ItemStack stack) {
		if (stack.isEmpty()) {
			return 0L;
		}
		long unit = unitValue(stack.getItem());
		if (stack.isDamaged() && stack.getMaxDamage() > 0) {
			long left = stack.getMaxDamage() - stack.getDamageValue();
			unit = Coins.floorToQuarter(unit * left / stack.getMaxDamage());
		}
		return (unit + enchantmentValue(stack)) * stack.getCount();
	}

	public static long enchantmentValue(ItemStack stack) {
		return worthOf(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
				+ worthOf(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
	}

	private static long worthOf(ItemEnchantments enchantments) {
		long total = 0L;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
			if (!entry.getKey().isBound()) {
				continue;
			}
			int weight = Math.max(1, entry.getKey().value().getWeight());
			total += PER_LEVEL * entry.getIntValue() * COMMONEST / weight;
		}
		return Coins.floorToQuarter(total);
	}

	public static boolean isSellable(ItemStack stack) {
		return stackValue(stack) > 0L;
	}
}
