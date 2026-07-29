package com.kat.coreessentials.commands;

import com.kat.coreessentials.combat.CombatManager;
import com.kat.coreessentials.combat.TeleportDelayManager;
import com.kat.coreessentials.combat.TeleportExecutor;
import com.kat.coreessentials.data.HomeManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HomeCommand {

    private HomeCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(JavaPlugin plugin, HomeManager homes,
                                                                CombatManager combat, TeleportDelayManager delays) {
        SuggestionProvider<CommandSourceStack> homeNameSuggestions = (ctx, builder) -> {
            CommandSender sender = ctx.getSource().getSender();
            if (sender instanceof Player player) {
                String remaining = builder.getRemaining().toLowerCase();
                for (String name : homes.listHomes(player.getUniqueId())) {
                    if (name.toLowerCase().startsWith(remaining)) {
                        builder.suggest(name);
                    }
                }
            }
            return builder.buildFuture();
        };

        return Commands.literal("home")
            .requires(source -> source.getSender().hasPermission("coreessentials.home"))
            .executes(ctx -> {
                listHomes(ctx.getSource().getSender(), homes);
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("list")
                .executes(ctx -> {
                    listHomes(ctx.getSource().getSender(), homes);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("set")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(Component.text("Only players can set homes.", NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        String name = StringArgumentType.getString(ctx, "name");
                        boolean success = homes.setHome(player.getUniqueId(), name, player.getLocation());
                        if (success) {
                            player.sendMessage(Component.text("Home '" + name + "' set.", NamedTextColor.GREEN));
                        } else {
                            player.sendMessage(Component.text("You already have the max number of homes. Delete one first with /home del <name>.", NamedTextColor.RED));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("del")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(homeNameSuggestions)
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!(sender instanceof Player player)) {
                            return Command.SINGLE_SUCCESS;
                        }
                        String name = StringArgumentType.getString(ctx, "name");
                        boolean removed = homes.deleteHome(player.getUniqueId(), name);
                        if (removed) {
                            player.sendMessage(Component.text("Home '" + name + "' deleted.", NamedTextColor.GREEN));
                        } else {
                            player.sendMessage(Component.text("No home named '" + name + "'.", NamedTextColor.RED));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("tp")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests(homeNameSuggestions)
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage(Component.text("Only players can teleport to homes.", NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        String name = StringArgumentType.getString(ctx, "name");
                        Location location = homes.getHome(player.getUniqueId(), name);
                        if (location == null) {
                            player.sendMessage(Component.text("No home named '" + name + "'.", NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        TeleportExecutor.request(plugin, combat, delays, player, location, "Teleported to home '" + name + "'.");
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .build();
    }

    private static void listHomes(CommandSender sender, HomeManager homes) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players have homes.", NamedTextColor.RED));
            return;
        }
        var names = homes.listHomes(player.getUniqueId());
        if (names.isEmpty()) {
            player.sendMessage(Component.text("You have no homes set. Use /home set <name>.", NamedTextColor.YELLOW));
            return;
        }
        player.sendMessage(Component.text("Your homes: " + String.join(", ", names) + " - teleport with /home tp <name>", NamedTextColor.AQUA));
    }
}
