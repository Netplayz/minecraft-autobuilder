package com.autobuilder.builder;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.process.IBuilderProcess;
import com.autobuilder.config.AutoBuilderConfig;
import com.autobuilder.gather.MaterialManager;
import com.autobuilder.gather.MaterialRequirement;
import com.autobuilder.schematic.SchematicInfo;
import com.autobuilder.schematic.SchematicManager;
import com.autobuilder.util.BlockCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BuildManager {
    private final SchematicManager schematicManager;
    private final MaterialManager materialManager;
    private final AutoBuilderConfig config;
    private final IBaritone baritone;
    private BuildJob currentJob;

    public BuildManager(SchematicManager schematicManager, MaterialManager materialManager, AutoBuilderConfig config) {
        this.schematicManager = schematicManager;
        this.materialManager = materialManager;
        this.config = config;
        this.baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        this.currentJob = null;
    }

    public boolean startBuild(String schematicName) {
        if (currentJob != null && currentJob.isActive()) {
            sendMsg("§cAlready building! Use /autobuilder cancel first.");
            return false;
        }

        Optional<SchematicInfo> optSchematic = schematicManager.getSchematic(schematicName);
        if (optSchematic.isEmpty()) {
            sendMsg("§cSchematic not found. Use /autobuilder list to see available schematics.");
            return false;
        }

        SchematicInfo schematic = optSchematic.get();

        Map<Block, Integer> blockCounts = BlockCounter.countRequiredBlocks(schematic);
        if (blockCounts.isEmpty()) {
            sendMsg("§cSchematic is empty or contains only air blocks.");
            return false;
        }

        List<MaterialRequirement> requirements = materialManager.analyzeRequirements(blockCounts);
        currentJob = new BuildJob(schematic, requirements);
        currentJob.setState(BuildState.CHECKING_INVENTORY);

        long totalMissing = requirements.stream().mapToInt(MaterialRequirement::getMissing).sum();
        long totalRequired = requirements.stream().mapToInt(MaterialRequirement::getRequired).sum();

        sendMsg("§eStarting build: §6" + schematic.getName());
        sendMsg("§7Dimensions: " + schematic.getWidth() + "x" + schematic.getHeight() + "x" + schematic.getLength()
                + " (" + schematic.getTotalVolume() + " blocks)");
        sendMsg("§7Blocks needed: §6" + totalRequired + " §7(§6" + totalMissing + " §7missing)");

        if (totalMissing > 0 && config.isAutoGatherMaterials()) {
            currentJob.setState(BuildState.GATHERING_MATERIALS);
            materialManager.startGathering(requirements);
        } else if (totalMissing > 0) {
            sendMsg("§eAuto-gather is disabled. Gather materials manually, then run /autobuilder start again.");
            currentJob = null;
            return false;
        } else {
            startBuilding();
        }

        return true;
    }

    public void startBuilding() {
        if (currentJob == null) return;

        SchematicInfo schematic = currentJob.getSchematic();
        currentJob.setState(BuildState.BUILDING);

        sendMsg("§aStarting construction of §6" + schematic.getName());

        IBuilderProcess builder = baritone.getBuilderProcess();
        builder.build(schematic.getName(), schematic.getSchematic(), new BlockPos(
                (int) Minecraft.getInstance().player.position().x,
                (int) Minecraft.getInstance().player.position().y,
                (int) Minecraft.getInstance().player.position().z
        ));
    }

    public void startLitematicaBuild() {
        if (currentJob != null && currentJob.isActive()) {
            sendMsg("§cAlready building! Cancel first.");
            return;
        }

        SchematicInfo info = new SchematicInfo("Litematica Placement", "Litematica", null);
        currentJob = new BuildJob(info, List.of());
        currentJob.setState(BuildState.BUILDING);

        baritone.getBuilderProcess().buildOpenLitematic(0);
        sendMsg("§aStarting build from Litematica placement");
    }

    public void cancelBuild() {
        if (currentJob == null) {
            sendMsg("§cNo active build job.");
            return;
        }

        materialManager.cancelGathering();
        baritone.getPathingBehavior().cancelEverything();
        currentJob.setState(BuildState.IDLE);
        currentJob = null;
        sendMsg("§cBuild cancelled.");
    }

    public void tick() {
        if (currentJob == null || !currentJob.isActive()) return;

        if (materialManager.isGathering()) {
            materialManager.tick();

            if (!materialManager.isGathering()) {
                startBuilding();
            }
            return;
        }

        if (currentJob.getState() == BuildState.BUILDING) {
            IBuilderProcess builder = baritone.getBuilderProcess();
            if (!builder.isActive()) {
                currentJob.setState(BuildState.COMPLETED);
                sendMsg("§a✔ Build complete: §6" + currentJob.getSchematic().getName());
                currentJob = null;
            }
        }
    }

    public void printStatus() {
        if (currentJob == null) {
            sendMsg("§7No active build job.");
            return;
        }

        BuildState state = currentJob.getState();
        sendMsg("§6=== Build Status ===");
        sendMsg("§7Schematic: §f" + currentJob.getSchematic().getName());
        sendMsg("§7State: §f" + state);

        switch (state) {
            case GATHERING_MATERIALS -> {
                long missing = currentJob.getRequirements().stream()
                        .filter(r -> !r.isFulfilled())
                        .count();
                long total = currentJob.getRequirements().size();
                sendMsg("§7Gathering: §f" + missing + "/" + total + " material types remaining");
            }
            case BUILDING -> {
                int placed = currentJob.getBlocksPlaced();
                int total = currentJob.getTotalBlocks();
                int pct = total > 0 ? (int) (placed * 100.0 / total) : 0;
                sendMsg("§7Progress: §f" + placed + "/" + total + " blocks (" + pct + "%)");
            }
            case FAILED -> sendMsg("§cError: §f" + currentJob.getErrorMessage());
        }
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public BuildJob getCurrentJob() {
        return currentJob;
    }

    public boolean isBusy() {
        return currentJob != null && currentJob.isActive();
    }

    private void sendMsg(String msg) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal("§7[§9AutoBuilder§7] §r" + msg));
        }
    }
}
