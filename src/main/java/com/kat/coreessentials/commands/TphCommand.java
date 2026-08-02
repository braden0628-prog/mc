package com.kat.coreessentials.commands;

import com.kat.coreessentials.data.TeleportRequestManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
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

public final class TphCommand {

    private TphCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(TeleportRequestManager requests) {
        return Commands.literal("tph")
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

                    requests.addRequest(target.getUniqueId(), requester.getUniqueId(), true);

                    requester.sendMessage(Component.text("Teleport-here request sent to " + target.getName() + ".", NamedTextColor.GREEN));

                    Component accept = Component.text("[Accept]")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/tpraccept"));
                    Component decline = Component.text("[Decline]")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/tprdeny"));

                    target.sendMessage(
                        Component.text(requester.getName() + " wants you to teleport to them ", NamedTextColor.YELLOW)
                            .append(accept)
                            .append(Component.text(" "))
                            .append(decline)
                    );

                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
