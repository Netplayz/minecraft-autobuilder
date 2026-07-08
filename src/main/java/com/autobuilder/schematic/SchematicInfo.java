package com.autobuilder.schematic;

import baritone.api.schematic.ISchematic;

public class SchematicInfo {
    private final String name;
    private final String fileName;
    private final ISchematic schematic;
    private final int width;
    private final int height;
    private final int length;

    public SchematicInfo(String name, String fileName, ISchematic schematic) {
        this.name = name;
        this.fileName = fileName;
        this.schematic = schematic;
        this.width = schematic.widthX();
        this.height = schematic.heightY();
        this.length = schematic.lengthZ();
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public ISchematic getSchematic() {
        return schematic;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public int getTotalVolume() {
        return width * height * length;
    }
}
