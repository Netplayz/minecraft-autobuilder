# AutoBuilder - Minecraft Fabric Mod

Automatically gathers materials and builds structures using Baritone. Builds directly from Litematica in-world placements — no file browsing needed.

## Requirements

### Minecraft 26.2
| Tool | Version |
|------|---------|
| Java | 26+ |
| Gradle | 9.6+ |
| Fabric Loader | 0.19.3+ |
| Minecraft | 26.2 |
| Fabric API | 0.154.2+ |
| Baritone | 26.2 community build |

## Setup

### 1. Get Baritone API jar

Download a community Baritone API build for MC 26.2 and place it in `libs/`.

### 2. Build

```bash
./gradlew build
```

The built mod jar will be at `build/libs/autobuilder-1.0.0.jar`.

### 3. Install

Place the mod jar, Fabric API jar, and the Baritone API jar into your `.minecraft/mods/` folder.

### 4. (Optional) Litematica mod

Install the [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) mod to place schematics in-world for one-click building.

## Usage

### GUI (Recommended)

Press **B** to open the AutoBuilder GUI.

| Action | Description |
|--------|-------------|
| **Build Litematica Placement** | Build whatever schematic you've placed in-world via Litematica |
| **Cancel Build** | Stop the current build |

The GUI includes a live **Event Log** panel at the bottom showing a scrollable history of every action: build started, gathering, construction, completion, cancellation, and failures.

### Commands

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

1. **Place**: Place a schematic in-world using the Litematica mod
2. **Build**: Press B, click "Build Litematica Placement"
3. **Watch**: The Event Log shows real-time progress
4. **Construct**: Baritone's `BuilderProcess` places every block

## Architecture

```
autobuilder/
├── src/main/java/com/autobuilder/
│   ├── AutoBuilderMod.java      # Fabric entry point, keybinding, tick
│   ├── gui/
│   │   └── SchematicScreen.java # In-game GUI (press B) with live event log
│   ├── command/
│   │   └── AutoBuildCommand.java # /autobuilder command tree
│   ├── builder/
│   │   ├── BuildManager.java    # Orchestrates gather-then-build pipeline
│   │   ├── BuildJob.java        # Tracks a single build operation
│   │   └── BuildState.java      # State machine (IDLE→GATHER→BUILD→DONE)
│   ├── gather/
│   │   ├── MaterialManager.java # Inventory checking & auto-mining
│   │   └── MaterialRequirement.java # Block requirement model
│   ├── schematic/
│   │   ├── SchematicManager.java # Loads & caches schematic files
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
