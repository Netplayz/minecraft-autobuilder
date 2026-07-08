# AutoBuilder - Minecraft Fabric Mod

Automatically gathers materials and builds structures from `.schematic` (Schematica) files using Baritone.

## Requirements

### Minecraft 26.1 (Java Edition 2026)
| Tool | Version |
|------|---------|
| Java | 25+ |
| Gradle | 9.4+ |
| Fabric Loader | 0.18.4+ |
| Minecraft | 26.1 |
| Fabric API | Match MC version |
| Baritone | 26.1 community build |

### Minecraft 1.21.8 (alternative)
| Tool | Version |
|------|---------|
| Java | 21+ |
| Gradle | 8.x |
| Fabric Loader | 0.16.x |
| Minecraft | 1.21.8 |
| Fabric API | 0.115.x+ |
| Baritone | v1.15.0+ |

## Setup

### 1. Get Baritone API jar

**For MC 1.21.8 (default):** Download `baritone-api-fabric-1.15.0.jar` from the [official releases](https://github.com/cabaletta/baritone/releases/tag/v1.15.0) and place it in `libs/`.

**For MC 26.1:** Download a community Baritone API build from [issue #5011](https://github.com/cabaletta/baritone/issues/5011) or build from [PR #4990](https://github.com/cabaletta/baritone/pull/4990). Place the jar in `libs/`.

### 2. Configure version

Edit `gradle.properties`:
- **MC 1.21.8 (default - builds with Java 21):**
  ```
  minecraft_version=1.21.8
  fabric_api_version=0.115.0+1.21.8
  loader_version=0.16.14
  java_version=21
  ```
- **MC 26.1 (requires Java 25):**
  ```
  minecraft_version=26.1
  fabric_api_version=26.1.0
  loader_version=0.18.4
  java_version=25
  ```
  Also copy `gradle/wrapper/gradle-wrapper-26.1.properties` over `gradle/wrapper/gradle-wrapper.properties`.

### 3. Build

```bash
./gradlew build
```

The built mod jar will be at `build/libs/autobuilder-1.0.0.jar`.

### 4. Install

Place the mod jar, Fabric API jar, and the Baritone API jar into your `.minecraft/mods/` folder.

### 5. Place schematics

Put `.schematic` and `.schem` files in `.minecraft/autobuilder/schematics/`.

## Commands

| Command | Description |
|---------|-------------|
| `/autobuilder list` | List all available schematics |
| `/autobuilder info <name>` | Show schematic dimensions & block info |
| `/autobuilder start <name>` | Auto-gather materials then build |
| `/autobuilder gather <name>` | Only gather materials, don't build |
| `/autobuilder cancel` | Cancel current build job |
| `/autobuilder status` | Show progress of current build |
| `/autobuilder reload` | Reload schematic files from disk |

## How it works

1. **Load**: Scans `.minecraft/autobuilder/schematics/` for `.schematic`/`.schem` files
2. **Analyze**: Counts every required block type in the schematic
3. **Inventory check**: Compares requirements against your inventory
4. **Gather**: For missing blocks, uses Baritone's `MineProcess` to auto-mine them
5. **Build**: Once all materials are gathered, uses Baritone's `BuilderProcess` to place blocks

## Architecture

```
autobuilder/
├── src/main/java/com/autobuilder/
│   ├── AutoBuilderMod.java      # Fabric entry point, tick/command registration
│   ├── command/
│   │   └── AutoBuildCommand.java # /autobuilder command tree
│   ├── builder/
│   │   ├── BuildManager.java    # Orchestrates gather-then-build pipeline
│   │   ├── BuildJob.java        # Tracks a single build operation
│   │   └── BuildState.java      # State machine (IDLE→ANALYZE→GATHER→BUILD→DONE)
│   ├── gather/
│   │   ├── MaterialManager.java # Inventory checking & auto-mining
│   │   └── MaterialRequirement.java # Block requirement model
│   ├── schematic/
│   │   ├── SchematicManager.java # Loads & caches .schematic files
│   │   └── SchematicInfo.java   # Schematic metadata
│   ├── config/
│   │   └── AutoBuilderConfig.java # Mod configuration
│   └── util/
│       ├── BlockCounter.java    # Scans schematics for block counts
│       └── InventoryHelper.java # Player inventory query utils
├── build.gradle
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/
    └── gradle-wrapper.properties
```
