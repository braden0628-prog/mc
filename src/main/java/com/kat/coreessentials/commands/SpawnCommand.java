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

public final class SpawnCommand {

    private SpawnCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(SpawnManager spawnManager) {
        return Commands.literal("spawn")
            // No permission gate - any player can always use this, mirroring
            // how /setspawn is a hard op-check rather than a permission node.
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can teleport to spawn.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                player.teleportAsync(spawnManager.getSpawn());
                player.sendMessage(Component.text("Teleported to spawn.", NamedTextColor.GREEN));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }
}
