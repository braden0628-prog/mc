package com.kat.coreessentials.commands;

import com.kat.coreessentials.data.TeamManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class TeamCommand {

    private TeamCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(TeamManager teams) {
        return Commands.literal("team")
            .requires(source -> source.getSender().hasPermission("coreessentials.team"))
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        Player player = asPlayer(ctx.getSource().getSender());
                        if (player == null) return Command.SINGLE_SUCCESS;
                        String name = StringArgumentType.getString(ctx, "name");
                        if (teams.createTeam(player.getUniqueId(), name)) {
                            player.sendMessage(Component.text("Team '" + name + "' created.", NamedTextColor.GREEN));
                        } else {
                            player.sendMessage(Component.text("That team name is taken, or you're already in a team.", NamedTextColor.RED));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("invite")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(ctx -> {
                        Player player = asPlayer(ctx.getSource().getSender());
                        if (player == null) return Command.SINGLE_SUCCESS;

                        String teamKey = teams.getTeamOf(player.getUniqueId());
                        if (teamKey == null) {
                            player.sendMessage(Component.text("You're not in a team.", NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }

                        PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                        Player target = resolver.resolve(ctx.getSource()).getFirst();

                        if (teams.getTeamOf(target.getUniqueId()) != null) {
                            player.sendMessage(Component.text(target.getName() + " is already in a team.", NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }

                        teams.invite(teamKey, player.getUniqueId(), target.getUniqueId());
                        String display = teams.getDisplayName(teamKey);

                        player.sendMessage(Component.text("Invited " + target.getName() + " to " + display + ".", NamedTextColor.GREEN));

                        Component accept = Component.text("[Accept]")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/team accept"));
                        Component decline = Component.text("[Decline]")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/team deny"));

                        target.sendMessage(
                            Component.text(player.getName() + " invited you to team " + display + " ", NamedTextColor.YELLOW)
                                .append(accept)
                                .append(Component.text(" "))
                                .append(decline)
                        );
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("accept")
                .executes(ctx -> {
                    Player player = asPlayer(ctx.getSource().getSender());
                    if (player == null) return Command.SINGLE_SUCCESS;
                    String teamKey = teams.acceptInvite(player.getUniqueId());
                    if (teamKey == null) {
                        player.sendMessage(Component.text("You have no pending team invite.", NamedTextColor.RED));
                    } else {
                        player.sendMessage(Component.text("Joined team " + teams.getDisplayName(teamKey) + "!", NamedTextColor.GREEN));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("deny")
                .executes(ctx -> {
                    Player player = asPlayer(ctx.getSource().getSender());
                    if (player == null) return Command.SINGLE_SUCCESS;
                    teams.clearInvite(player.getUniqueId());
                    player.sendMessage(Component.text("Team invite declined.", NamedTextColor.YELLOW));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("leave")
                .executes(ctx -> {
                    Player player = asPlayer(ctx.getSource().getSender());
                    if (player == null) return Command.SINGLE_SUCCESS;
                    if (teams.leaveTeam(player.getUniqueId())) {
                        player.sendMessage(Component.text("You left your team.", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("You're not in a team.", NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("disband")
                .executes(ctx -> {
                    Player player = asPlayer(ctx.getSource().getSender());
                    if (player == null) return Command.SINGLE_SUCCESS;
                    if (teams.disbandTeam(player.getUniqueId())) {
                        player.sendMessage(Component.text("Team disbanded.", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("You must be the team owner to disband it.", NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("kick")
                .then(Commands.argument("target", ArgumentTypes.player())
                    .executes(ctx -> {
                        Player player = asPlayer(ctx.getSource().getSender());
                        if (player == null) return Command.SINGLE_SUCCESS;
                        PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                        Player target = resolver.resolve(ctx.getSource()).getFirst();
                        if (teams.kick(player.getUniqueId(), target.getUniqueId())) {
                            player.sendMessage(Component.text("Kicked " + target.getName() + " from the team.", NamedTextColor.GREEN));
                            target.sendMessage(Component.text("You were kicked from your team.", NamedTextColor.RED));
                        } else {
                            player.sendMessage(Component.text("You must be the team owner, and they must be a member.", NamedTextColor.RED));
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    Player player = asPlayer(ctx.getSource().getSender());
                    if (player == null) return Command.SINGLE_SUCCESS;
                    String teamKey = teams.getTeamOf(player.getUniqueId());
                    if (teamKey == null) {
                        player.sendMessage(Component.text("You're not in a team.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    UUID owner = teams.getOwner(teamKey);
                    List<String> names = teams.getMembers(teamKey).stream()
                        .map(id -> {
                            String name = Bukkit.getOfflinePlayer(id).getName();
                            name = name == null ? id.toString() : name;
                            return id.equals(owner) ? name + " (owner)" : name;
                        })
                        .collect(Collectors.toList());
                    player.sendMessage(Component.text(teams.getDisplayName(teamKey) + ": " + String.join(", ", names), NamedTextColor.AQUA));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("chat")
                .executes(ctx -> {
                    Player player = asPlayer(ctx.getSource().getSender());
                    if (player == null) return Command.SINGLE_SUCCESS;
                    if (teams.getTeamOf(player.getUniqueId()) == null) {
                        player.sendMessage(Component.text("You're not in a team.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    boolean nowOn = teams.toggleTeamChat(player.getUniqueId());
                    if (nowOn) {
                        player.sendMessage(Component.text("Team chat enabled. Everything you type now only goes to your team. Use /team chat again to turn it off.", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("Team chat disabled.", NamedTextColor.YELLOW));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        Player player = asPlayer(ctx.getSource().getSender());
                        if (player == null) return Command.SINGLE_SUCCESS;
                        String teamKey = teams.getTeamOf(player.getUniqueId());
                        if (teamKey == null) {
                            player.sendMessage(Component.text("You're not in a team.", NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        String message = StringArgumentType.getString(ctx, "message");
                        NamedTextColor color = teams.getColor(teamKey);
                        Component formatted = Component.text("[" + teams.getDisplayName(teamKey) + "] ", color)
                            .append(Component.text(player.getName() + ": ", NamedTextColor.WHITE))
                            .append(Component.text(message, NamedTextColor.GRAY));

                        for (UUID memberId : teams.getMembers(teamKey)) {
                            Player member = Bukkit.getPlayer(memberId);
                            if (member != null) {
                                member.sendMessage(formatted);
                            }
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .build();
    }

    private static Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
        return null;
    }
}
