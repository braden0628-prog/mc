package com.kat.coreessentials.commands;

import com.kat.coreessentials.xray.XrayDetector;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class XrayCheckCommand {

    private XrayCheckCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(XrayDetector detector) {
        return Commands.literal("xraycheck")
            .requires(source -> source.getSender().hasPermission("coreessentials.xray.alerts"))
            .then(Commands.argument("target", ArgumentTypes.player())
                .executes(ctx -> {
                    PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                    Player target = resolver.resolve(ctx.getSource()).getFirst();
                    ctx.getSource().getSender().sendMessage(
                        Component.text(target.getName() + " - " + detector.describe(target.getUniqueId()), NamedTextColor.AQUA)
                    );
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
