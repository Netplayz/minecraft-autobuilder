package com.autobuilder.config;

import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

public class AutoBuilderConfig {
    private static final Path SCHEMATICS_DIR = Minecraft.getInstance().gameDirectory
            .toPath().resolve("autobuilder").resolve("schematics");

    private boolean autoGatherMaterials = true;
    private int maxGatherRadius = 128;
    private boolean buildInLayers = false;
    private boolean ignoreAir = true;

    public AutoBuilderConfig() {
        try {
            Files.createDirectories(SCHEMATICS_DIR);
        } catch (Exception ignored) {}
    }

    public static Path getSchematicsDir() {
        return SCHEMATICS_DIR;
    }

    public boolean isAutoGatherMaterials() {
        return autoGatherMaterials;
    }

    public void setAutoGatherMaterials(boolean autoGatherMaterials) {
        this.autoGatherMaterials = autoGatherMaterials;
    }

    public int getMaxGatherRadius() {
        return maxGatherRadius;
    }

    public void setMaxGatherRadius(int maxGatherRadius) {
        this.maxGatherRadius = maxGatherRadius;
    }

    public boolean isBuildInLayers() {
        return buildInLayers;
    }

    public void setBuildInLayers(boolean buildInLayers) {
        this.buildInLayers = buildInLayers;
    }

    public boolean isIgnoreAir() {
        return ignoreAir;
    }

    public void setIgnoreAir(boolean ignoreAir) {
        this.ignoreAir = ignoreAir;
    }
}
