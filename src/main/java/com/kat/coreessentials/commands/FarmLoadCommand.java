package com.kat.coreessentials.commands;

import com.kat.coreessentials.farmload.FarmLoadManager;
import com.kat.coreessentials.farmload.FarmZone;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FarmLoadCommand {

    private FarmLoadCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(FarmLoadManager manager) {
        return Commands.literal("farmload")
            .requires(source -> source.getSender().hasPermission("coreessentials.farmload"))
            .then(Commands.literal("add")
                .executes(ctx -> addZones(ctx, manager, 1))
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 5))
                    .executes(ctx -> addZones(ctx, manager, IntegerArgumentType.getInteger(ctx, "radius")))
                )
            )
            .then(Commands.literal("remove")
                .executes(ctx -> removeZones(ctx, manager, 1))
                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 5))
                    .executes(ctx -> removeZones(ctx, manager, IntegerArgumentType.getInteger(ctx, "radius")))
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Component.text(
                        manager.getZoneCount() + " chunk(s) registered for offline crop growth.", NamedTextColor.AQUA
                    ));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }

    private static int addZones(CommandContext<CommandSourceStack> ctx, FarmLoadManager manager, int radius) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        int centerX = player.getChunk().getX();
        int centerZ = player.getChunk().getZ();
        String world = player.getWorld().getName();

        int added = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (manager.addZone(new FarmZone(world, centerX + dx, centerZ + dz))) {
                    added++;
                }
            }
        }
        player.sendMessage(Component.text(
            "Registered " + added + " chunk(s) around you for offline crop growth.", NamedTextColor.GREEN
        ));
        return Command.SINGLE_SUCCESS;
    }

    private static int removeZones(CommandContext<CommandSourceStack> ctx, FarmLoadManager manager, int radius) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        int centerX = player.getChunk().getX();
        int centerZ = player.getChunk().getZ();
        String world = player.getWorld().getName();

        int removed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (manager.removeZone(new FarmZone(world, centerX + dx, centerZ + dz))) {
                    removed++;
                }
            }
        }
        player.sendMessage(Component.text(
            "Unregistered " + removed + " chunk(s) around you.", NamedTextColor.YELLOW
        ));
        return Command.SINGLE_SUCCESS;
    }
}
