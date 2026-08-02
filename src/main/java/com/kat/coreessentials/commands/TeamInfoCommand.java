package com.kat.coreessentials.commands;

import com.kat.coreessentials.data.TeamManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class TeamInfoCommand {

    private TeamInfoCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(TeamManager teams) {
        SuggestionProvider<CommandSourceStack> teamNameSuggestions = (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();
            for (String key : teams.getAllTeamKeys()) {
                String display = teams.getDisplayName(key);
                if (display.toLowerCase().startsWith(remaining)) {
                    builder.suggest(display);
                }
            }
            return builder.buildFuture();
        };

        return Commands.literal("teaminfo")
            .requires(source -> source.getSender().hasPermission("coreessentials.team"))
            .then(Commands.argument("name", StringArgumentType.greedyString())
                .suggests(teamNameSuggestions)
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    String query = StringArgumentType.getString(ctx, "name");
                    String key = query.toLowerCase();

                    if (!teams.teamExists(key)) {
                        sender.sendMessage(Component.text("No team named '" + query + "'.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    NamedTextColor color = teams.getColor(key);
                    UUID owner = teams.getOwner(key);
                    List<UUID> members = teams.getMembers(key);
                    List<String> names = members.stream()
                        .map(id -> {
                            String name = Bukkit.getOfflinePlayer(id).getName();
                            name = name == null ? id.toString() : name;
                            return id.equals(owner) ? name + " (owner)" : name;
                        })
                        .collect(Collectors.toList());

                    sender.sendMessage(
                        Component.text("Team: ", NamedTextColor.WHITE)
                            .append(Component.text(teams.getDisplayName(key), color))
                            .append(Component.text(" (" + members.size() + "/" + teams.maxMembers() + ")", NamedTextColor.GRAY))
                    );
                    sender.sendMessage(Component.text("Members: " + String.join(", ", names), NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
