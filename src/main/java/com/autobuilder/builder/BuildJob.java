package com.autobuilder.builder;

import com.autobuilder.gather.MaterialRequirement;
import com.autobuilder.schematic.SchematicInfo;

import java.util.List;

public class BuildJob {
    private final SchematicInfo schematic;
    private final List<MaterialRequirement> requirements;
    private BuildState state;
    private int blocksPlaced;
    private int totalBlocks;
    private String errorMessage;

    public BuildJob(SchematicInfo schematic, List<MaterialRequirement> requirements) {
        this.schematic = schematic;
        this.requirements = requirements;
        this.state = BuildState.ANALYZING_SCHEMATIC;
        this.blocksPlaced = 0;
        this.totalBlocks = requirements.stream().mapToInt(MaterialRequirement::getRequired).sum();
    }

    public SchematicInfo getSchematic() {
        return schematic;
    }

    public List<MaterialRequirement> getRequirements() {
        return requirements;
    }

    public BuildState getState() {
        return state;
    }

    public void setState(BuildState state) {
        this.state = state;
    }

    public int getBlocksPlaced() {
        return blocksPlaced;
    }

    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }

    public void incrementBlocksPlaced() {
        this.blocksPlaced++;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.state = BuildState.FAILED;
    }

    public double getProgress() {
        if (totalBlocks == 0) return 1.0;
        return (double) blocksPlaced / totalBlocks;
    }

    public boolean isActive() {
        return state != BuildState.IDLE
                && state != BuildState.COMPLETED
                && state != BuildState.FAILED;
    }
}
