package com.autobuilder;

import com.autobuilder.builder.BuildManager;
import com.autobuilder.command.AutoBuildCommand;
import com.autobuilder.config.AutoBuilderConfig;
import com.autobuilder.gather.MaterialManager;
import com.autobuilder.schematic.SchematicManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class AutoBuilderMod implements ClientModInitializer {
    private static AutoBuilderMod instance;

    private AutoBuilderConfig config;
    private SchematicManager schematicManager;
    private MaterialManager materialManager;
    private BuildManager buildManager;

    @Override
    public void onInitializeClient() {
        instance = this;

        this.config = new AutoBuilderConfig();
        this.schematicManager = new SchematicManager();
        this.materialManager = new MaterialManager(config);
        this.buildManager = new BuildManager(schematicManager, materialManager, config);

        this.schematicManager.reload();

        registerCommands();
        registerTick();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AutoBuildCommand.register(dispatcher);
        });
    }

    private void registerTick() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                buildManager.tick();
            }
        });
    }

    public static AutoBuilderMod getInstance() {
        return instance;
    }

    public AutoBuilderConfig getConfig() {
        return config;
    }

    public SchematicManager getSchematicManager() {
        return schematicManager;
    }

    public MaterialManager getMaterialManager() {
        return materialManager;
    }

    public BuildManager getBuildManager() {
        return buildManager;
    }
}
