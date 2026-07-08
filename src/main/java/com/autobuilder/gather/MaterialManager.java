package com.autobuilder.gather;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import com.autobuilder.config.AutoBuilderConfig;
import com.autobuilder.util.BlockCounter;
import com.autobuilder.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.*;
import java.util.stream.Collectors;

public class MaterialManager {
    private final AutoBuilderConfig config;
    private final IBaritone baritone;
    private List<MaterialRequirement> requirements;
    private boolean gathering;
    private int currentMaterialIndex;

    public MaterialManager(AutoBuilderConfig config) {
        this.config = config;
        this.baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        this.requirements = new ArrayList<>();
        this.gathering = false;
        this.currentMaterialIndex = 0;
    }

    public List<MaterialRequirement> analyzeRequirements(Map<Block, Integer> requiredBlocks) {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();

        var inventory = player.getInventory();
        Map<Block, Integer> inventoryCounts = InventoryHelper.getInventoryCounts(inventory, requiredBlocks.keySet());

        return requiredBlocks.entrySet().stream()
                .map(entry -> {
                    Block block = entry.getKey();
                    int required = entry.getValue();
                    int inInv = inventoryCounts.getOrDefault(block, 0);
                    String name = BlockCounter.getBlockDisplayName(block);
                    return new MaterialRequirement(block, required, inInv, name);
                })
                .sorted(Comparator.comparing(MaterialRequirement::getDisplayName))
                .collect(Collectors.toList());
    }

    public boolean startGathering(List<MaterialRequirement> reqs) {
        if (gathering || reqs.isEmpty()) return false;

        this.requirements = new ArrayList<>(reqs);
        this.currentMaterialIndex = 0;

        List<MaterialRequirement> missing = reqs.stream()
                .filter(r -> !r.isFulfilled())
                .collect(Collectors.toList());

        if (missing.isEmpty()) {
            sendMsg("§aAll materials already in inventory!");
            return false;
        }

        gathering = true;
        sendMsg("§eStarting material gathering for §6" + missing.size() + " §emissing block types");
        mineNext();
        return true;
    }

    private void mineNext() {
        while (currentMaterialIndex < requirements.size()) {
            MaterialRequirement req = requirements.get(currentMaterialIndex);
            int missing = req.getMissing();

            if (missing <= 0) {
                currentMaterialIndex++;
                continue;
            }

            Block block = req.getBlock();
            if (block == Blocks.AIR) {
                currentMaterialIndex++;
                continue;
            }

            sendMsg("§eGathering §6" + req.getDisplayName()
                    + " §e(need " + missing + " more, have " + req.getInInventory() + ")");

            baritone.getMineProcess().mine(0, new BlockOptionalMetaLookup(
                    new BlockOptionalMeta(BuiltInRegistries.BLOCK.getKey(block).toString())
            ));
            return;
        }

        gathering = false;
        sendMsg("§aAll materials gathered!");
    }

    public void tick() {
        if (!gathering || Minecraft.getInstance().player == null) return;

        MaterialRequirement current = requirements.get(currentMaterialIndex);
        int inInv = InventoryHelper.countItem(
                Minecraft.getInstance().player.getInventory(),
                current.getItem()
        );

        if (inInv >= current.getRequired()) {
            sendMsg("§aGot enough " + current.getDisplayName()
                    + " (" + inInv + "/" + current.getRequired() + ")");
            baritone.getMineProcess().cancel();
            currentMaterialIndex++;
            mineNext();
        }
    }

    public void cancelGathering() {
        gathering = false;
        baritone.getMineProcess().cancel();
        sendMsg("§cMaterial gathering cancelled");
    }

    public boolean isGathering() {
        return gathering;
    }

    public List<MaterialRequirement> getRequirements() {
        return requirements;
    }

    private void sendMsg(String msg) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal("§7[§9AutoBuilder§7] §r" + msg));
        }
    }
}
