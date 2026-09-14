package io.github.oliverrichman.landgrab;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TradeGroups {
	public record Group(ItemStack sample, int count, long value, List<Integer> sources) {
	}

	private TradeGroups() {
	}

	public static List<Group> of(List<ItemStack> tray) {
		Map<Item, List<Integer>> byItem = new LinkedHashMap<>();
		for (int index = 0; index < tray.size(); index++) {
			ItemStack stack = tray.get(index);
			if (!stack.isEmpty()) {
				byItem.computeIfAbsent(stack.getItem(), item -> new ArrayList<>()).add(index);
			}
		}
		List<Group> groups = new ArrayList<>(byItem.size());
		byItem.forEach((item, indices) -> {
			int count = 0;
			long value = 0L;
			for (int index : indices) {
				ItemStack stack = tray.get(index);
				count += stack.getCount();
				value += Economy.stackValue(stack);
			}
			groups.add(new Group(tray.get(indices.getFirst()), count, value, List.copyOf(indices)));
		});
		return groups;
	}

	public static long totalValue(List<ItemStack> tray) {
		long total = 0L;
		for (ItemStack stack : tray) {
			total += Economy.stackValue(stack);
		}
		return total;
	}
}
