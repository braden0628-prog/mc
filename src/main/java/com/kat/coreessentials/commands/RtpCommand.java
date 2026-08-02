package com.kat.coreessentials.commands;

import com.kat.coreessentials.gui.RtpMenuListener;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RtpCommand {

    private RtpCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(RtpMenuListener rtpMenu) {
        return Commands.literal("rtp")
            .requires(source -> source.getSender().hasPermission("coreessentials.rtp"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use /rtp.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                rtpMenu.open(player);
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }
}
