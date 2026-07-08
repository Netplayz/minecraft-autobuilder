package com.autobuilder.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public class InventoryHelper {

    public static int countItem(Inventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static Map<Block, Integer> getInventoryCounts(Inventory inventory, Iterable<Block> blocks) {
        Map<Block, Integer> counts = new HashMap<>();
        for (Block block : blocks) {
            counts.put(block, countItem(inventory, block.asItem()));
        }
        return counts;
    }
}
