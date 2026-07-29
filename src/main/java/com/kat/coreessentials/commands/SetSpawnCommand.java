package com.kat.coreessentials.commands;

import com.kat.coreessentials.data.SpawnManager;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.mojang.brigadier.tree.LiteralCommandNode;

public final class SetSpawnCommand {

    private SetSpawnCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(SpawnManager spawnManager) {
        return Commands.literal("setspawn")
            // OP-only, as requested - not tied to a permission node so it can't
            // accidentally be granted away by a permissions plugin.
            .requires(source -> source.getSender().isOp())
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can set spawn.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                spawnManager.setSpawn(player.getLocation());
                player.sendMessage(Component.text("Spawn set to your current location.", NamedTextColor.GREEN));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }
}
