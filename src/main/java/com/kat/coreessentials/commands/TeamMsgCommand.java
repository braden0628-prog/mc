package com.kat.coreessentials.commands;

import com.kat.coreessentials.data.TeamManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class TeamMsgCommand {

    private TeamMsgCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(TeamManager teams) {
        return Commands.literal("teammsg")
            .requires(source -> source.getSender().hasPermission("coreessentials.team"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage(Component.text("Only players can use this.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    String teamKey = teams.getTeamOf(player.getUniqueId());
                    if (teamKey == null) {
                        player.sendMessage(Component.text("You're not in a team.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    String message = StringArgumentType.getString(ctx, "message");
                    NamedTextColor color = teams.getColor(teamKey);
                    Component formatted = Component.text("[", NamedTextColor.WHITE)
                        .append(Component.text(teams.getDisplayName(teamKey), color))
                        .append(Component.text("] ", NamedTextColor.WHITE))
                        .append(Component.text(player.getName() + ": ", NamedTextColor.WHITE))
                        .append(Component.text(message, NamedTextColor.WHITE));

                    for (UUID memberId : teams.getMembers(teamKey)) {
                        Player member = Bukkit.getPlayer(memberId);
                        if (member != null) {
                            member.sendMessage(formatted);
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
