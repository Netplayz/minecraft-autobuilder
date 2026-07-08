package com.autobuilder.gather;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class MaterialRequirement {
    private final Block block;
    private final Item item;
    private final int required;
    private final int inInventory;
    private final String displayName;

    public MaterialRequirement(Block block, int required, int inInventory, String displayName) {
        this.block = block;
        this.item = block.asItem();
        this.required = required;
        this.inInventory = inInventory;
        this.displayName = displayName;
    }

    public Block getBlock() {
        return block;
    }

    public Item getItem() {
        return item;
    }

    public int getRequired() {
        return required;
    }

    public int getInInventory() {
        return inInventory;
    }

    public int getMissing() {
        return Math.max(0, required - inInventory);
    }

    public boolean isFulfilled() {
        return inInventory >= required;
    }

    public String getDisplayName() {
        return displayName;
    }
}
