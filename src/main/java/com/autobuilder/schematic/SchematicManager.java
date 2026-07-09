package com.autobuilder.schematic;

import baritone.api.BaritoneAPI;
import baritone.api.schematic.IStaticSchematic;
import baritone.api.schematic.format.ISchematicFormat;
import com.autobuilder.config.AutoBuilderConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class SchematicManager {
    private final Map<String, SchematicInfo> schematics = new HashMap<>();

    public void reload() {
        schematics.clear();
        Path dir = AutoBuilderConfig.getSchematicsDir();

        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {}
            return;
        }

        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(p -> {
                String name = p.getFileName().toString().toLowerCase();
                return name.endsWith(".schematic") || name.endsWith(".schem") || name.endsWith(".litematic");
            }).forEach(this::loadSchematic);
        } catch (IOException ignored) {}
    }

    private void loadSchematic(Path path) {
        try {
            Optional<ISchematicFormat> optFormat = BaritoneAPI.getProvider()
                    .getSchematicSystem().getByFile(path.toFile());
            if (optFormat.isEmpty()) return;

            try (InputStream in = Files.newInputStream(path)) {
                IStaticSchematic schematic = optFormat.get().parse(in);
                if (schematic == null) return;

                String fileName = path.getFileName().toString();
                String name = fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf('.'))
                        : fileName;

                SchematicInfo info = new SchematicInfo(name, fileName, schematic);
                schematics.put(name.toLowerCase(), info);
            }
        } catch (Exception ignored) {}
    }

    public Optional<SchematicInfo> getSchematic(String name) {
        return Optional.ofNullable(schematics.get(name.toLowerCase()));
    }

    public List<String> getSchematicNames() {
        return schematics.values().stream()
                .map(SchematicInfo::getName)
                .sorted()
                .toList();
    }

    public List<SchematicInfo> getAllSchematics() {
        return schematics.values().stream()
                .sorted(Comparator.comparing(SchematicInfo::getName))
                .toList();
    }

    public int getCount() {
        return schematics.size();
    }
}
