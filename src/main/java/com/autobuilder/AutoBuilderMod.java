package com.autobuilder;

import com.autobuilder.builder.BuildManager;
import com.autobuilder.command.AutoBuildCommand;
import com.autobuilder.config.AutoBuilderConfig;
import com.autobuilder.gather.MaterialManager;
import com.autobuilder.gui.SchematicScreen;
import com.autobuilder.schematic.SchematicManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutoBuilderMod implements ClientModInitializer {
    private static AutoBuilderMod instance;

    private AutoBuilderConfig config;
    private SchematicManager schematicManager;
    private MaterialManager materialManager;
    private BuildManager buildManager;
    private KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        instance = this;

        this.config = new AutoBuilderConfig();
        this.schematicManager = new SchematicManager();
        this.materialManager = new MaterialManager(config);
        this.buildManager = new BuildManager(schematicManager, materialManager, config);

        this.schematicManager.reload();

        registerKeybinding();
        registerCommands();
        registerTick();
    }

    private void registerKeybinding() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autobuilder.open_gui",
                GLFW.GLFW_KEY_B,
                new KeyMapping.Category(Identifier.fromNamespaceAndPath("autobuilder", "category"))
        ));
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AutoBuildCommand.register(dispatcher);
        });
    }

    private void registerTick() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.player != null) {
                    client.setScreenAndShow(new SchematicScreen());
                }
            }

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
