package com.autobuilder.util;

import baritone.api.schematic.IStaticSchematic;
import baritone.api.utils.BlockOptionalMeta;
import com.autobuilder.schematic.SchematicInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class BlockCounter {

    public static Map<Block, Integer> countRequiredBlocks(SchematicInfo info) {
        Map<Block, Integer> counts = new HashMap<>();
        IStaticSchematic schematic = info.getSchematic();

        for (int x = 0; x < info.getWidth(); x++) {
            for (int y = 0; y < info.getHeight(); y++) {
                for (int z = 0; z < info.getLength(); z++) {
                    var state = schematic.getDirect(x, y, z);
                    if (state == null) continue;

                    Block block = state.getBlock();
                    if (block == Blocks.AIR) continue;

                    counts.merge(block, 1, Integer::sum);
                }
            }
        }

        return counts;
    }

    public static String getBlockDisplayName(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return "unknown";
        return id.getPath();
    }

    public static BlockOptionalMeta blockToBom(Block block) {
        return new BlockOptionalMeta(BuiltInRegistries.BLOCK.getKey(block).toString());
    }
}
