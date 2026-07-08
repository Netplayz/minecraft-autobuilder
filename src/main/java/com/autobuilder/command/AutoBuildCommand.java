package com.autobuilder.command;

import com.autobuilder.AutoBuilderMod;
import com.autobuilder.builder.BuildManager;
import com.autobuilder.schematic.SchematicInfo;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AutoBuildCommand {
    private static final String PREFIX = "§7[§9AutoBuilder§7] §r";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("autobuilder")
                .then(Commands.literal("list")
                        .executes(AutoBuildCommand::listSchematics))
                .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(AutoBuildCommand::suggestSchematics)
                                .executes(AutoBuildCommand::schematicInfo)))
                .then(Commands.literal("start")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(AutoBuildCommand::suggestSchematics)
                                .executes(AutoBuildCommand::startBuild)))
                .then(Commands.literal("gather")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(AutoBuildCommand::suggestSchematics)
                                .executes(AutoBuildCommand::gatherMaterials)))
                .then(Commands.literal("cancel")
                        .executes(AutoBuildCommand::cancelBuild))
                .then(Commands.literal("status")
                        .executes(AutoBuildCommand::buildStatus))
                .then(Commands.literal("reload")
                        .executes(AutoBuildCommand::reloadSchematics))
                .executes(AutoBuildCommand::showHelp)
        );
    }

    private static CompletableFuture<Suggestions> suggestSchematics(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        BuildManager mgr = AutoBuilderMod.getInstance().getBuildManager();
        for (String name : mgr.getSchematicManager().getSchematicNames()) {
            if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private static int listSchematics(CommandContext<CommandSourceStack> ctx) {
        var mgr = AutoBuilderMod.getInstance().getBuildManager().getSchematicManager();
        var names = mgr.getSchematicNames();

        if (names.isEmpty()) {
            ctx.getSource().sendSuccess(() ->
                    Component.literal(PREFIX + "§eNo schematics found. Place .schematic files in autobuilder/schematics/"), false);
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().sendSuccess(() ->
                Component.literal(PREFIX + "§6Available schematics (§f" + names.size() + "§6):"), false);
        for (String name : names) {
            SchematicInfo info = mgr.getSchematic(name).orElse(null);
            if (info != null) {
                ctx.getSource().sendSuccess(() ->
                        Component.literal(" §7- §f" + info.getName()
                                + " §7(" + info.getWidth() + "x" + info.getHeight() + "x" + info.getLength() + ")"), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int schematicInfo(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        var opt = AutoBuilderMod.getInstance().getBuildManager().getSchematicManager().getSchematic(name);

        if (opt.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cSchematic '" + name + "' not found."));
            return 0;
        }

        SchematicInfo info = opt.get();
        ctx.getSource().sendSuccess(() ->
                Component.literal(PREFIX + "§6=== " + info.getName() + " ==="), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7Size: §f" + info.getWidth() + " x " + info.getHeight() + " x " + info.getLength()), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7Volume: §f" + info.getTotalVolume() + " blocks"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7File: §f" + info.getFileName()), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int startBuild(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof Player)) {
            ctx.getSource().sendFailure(Component.literal("§cPlayer-only command."));
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        boolean started = AutoBuilderMod.getInstance().getBuildManager().startBuild(name);

        if (!started) {
            ctx.getSource().sendFailure(Component.literal("§cFailed to start build. Check the name and try again."));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int gatherMaterials(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof Player)) {
            ctx.getSource().sendFailure(Component.literal("§cPlayer-only command."));
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "name");
        var opt = AutoBuilderMod.getInstance().getBuildManager().getSchematicManager().getSchematic(name);

        if (opt.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cSchematic '" + name + "' not found."));
            return 0;
        }

        SchematicInfo info = opt.get();
        var blockCounts = com.autobuilder.util.BlockCounter.countRequiredBlocks(info);
        var reqs = AutoBuilderMod.getInstance().getMaterialManager().analyzeRequirements(blockCounts);

        long missing = reqs.stream().filter(r -> !r.isFulfilled()).count();
        if (missing == 0) {
            ctx.getSource().sendSuccess(() ->
                    Component.literal(PREFIX + "§aYou already have all materials for this schematic!"), false);
            return Command.SINGLE_SUCCESS;
        }

        AutoBuilderMod.getInstance().getMaterialManager().startGathering(reqs);
        ctx.getSource().sendSuccess(() ->
                Component.literal(PREFIX + "§eStarted gathering §6" + missing + " §emissing material types"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int cancelBuild(CommandContext<CommandSourceStack> ctx) {
        AutoBuilderMod.getInstance().getBuildManager().cancelBuild();
        return Command.SINGLE_SUCCESS;
    }

    private static int buildStatus(CommandContext<CommandSourceStack> ctx) {
        AutoBuilderMod.getInstance().getBuildManager().printStatus();
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadSchematics(CommandContext<CommandSourceStack> ctx) {
        AutoBuilderMod.getInstance().getBuildManager().getSchematicManager().reload();
        int count = AutoBuilderMod.getInstance().getBuildManager().getSchematicManager().getCount();
        ctx.getSource().sendSuccess(() ->
                Component.literal(PREFIX + "§aReloaded schematics. Found §6" + count + " §afiles."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(PREFIX + "§6=== AutoBuilder Commands ==="), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7/autobuilder §flist §7- List available schematics"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7/autobuilder §finfo <name> §7- Show schematic details"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7/autobuilder §fstart <name> §7- Auto-gather & build"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7/autobuilder §fgather <name> §7- Only gather materials"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7/autobuilder §fcancel §7- Stop current job"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7/autobuilder §fstatus §7- Show job progress"), false);
        ctx.getSource().sendSuccess(() ->
                Component.literal(" §7/autobuilder §freload §7- Reload schematic files"), false);
        return Command.SINGLE_SUCCESS;
    }
}
