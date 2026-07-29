package com.kat.coreessentials.commands;

import com.kat.coreessentials.data.TeleportRequestManager;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.mojang.brigadier.tree.LiteralCommandNode;

public final class TprCommand {

    private TprCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(TeleportRequestManager requests) {
        return Commands.literal("tpr")
            .requires(source -> source.getSender().hasPermission("coreessentials.tpr"))
            .then(Commands.argument("target", ArgumentTypes.player())
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player requester)) {
                        sender.sendMessage(Component.text("Only players can send teleport requests.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                    Player target = resolver.resolve(ctx.getSource()).getFirst();

                    if (target.getUniqueId().equals(requester.getUniqueId())) {
                        requester.sendMessage(Component.text("You can't send a teleport request to yourself.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    requests.addRequest(target.getUniqueId(), requester.getUniqueId());

                    requester.sendMessage(Component.text("Teleport request sent to " + target.getName() + ".", NamedTextColor.GREEN));

                    Component accept = Component.text("[Accept]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/tpraccept"));
                    Component decline = Component.text("[Decline]")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/tprdeny"));

                    target.sendMessage(
                        Component.text(requester.getName() + " sent you a teleport request ", NamedTextColor.YELLOW)
                            .append(accept)
                            .append(Component.text(" "))
                            .append(decline)
                    );

                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }

    public static LiteralCommandNode<CommandSourceStack> buildAccept(TeleportRequestManager requests) {
        return Commands.literal("tpraccept")
            .requires(source -> source.getSender().hasPermission("coreessentials.tpr"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player target)) {
                    return Command.SINGLE_SUCCESS;
                }
                var requesterId = requests.getValidRequester(target.getUniqueId());
                if (requesterId == null) {
                    target.sendMessage(Component.text("You have no pending teleport request.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                Player requester = target.getServer().getPlayer(requesterId);
                requests.clear(target.getUniqueId());
                if (requester == null || !requester.isOnline()) {
                    target.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                requester.teleportAsync(target.getLocation());
                requester.sendMessage(Component.text(target.getName() + " accepted your teleport request.", NamedTextColor.GREEN));
                target.sendMessage(Component.text("Teleporting " + requester.getName() + " to you.", NamedTextColor.GREEN));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    public static LiteralCommandNode<CommandSourceStack> buildDeny(TeleportRequestManager requests) {
        return Commands.literal("tprdeny")
            .requires(source -> source.getSender().hasPermission("coreessentials.tpr"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player target)) {
                    return Command.SINGLE_SUCCESS;
                }
                var requesterId = requests.getValidRequester(target.getUniqueId());
                requests.clear(target.getUniqueId());
                if (requesterId == null) {
                    target.sendMessage(Component.text("You have no pending teleport request.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                Player requester = target.getServer().getPlayer(requesterId);
                target.sendMessage(Component.text("Teleport request declined.", NamedTextColor.YELLOW));
                if (requester != null && requester.isOnline()) {
                    requester.sendMessage(Component.text(target.getName() + " declined your teleport request.", NamedTextColor.RED));
                }
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }
}
