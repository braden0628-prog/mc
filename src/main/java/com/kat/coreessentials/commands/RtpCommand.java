package com.kat.coreessentials.commands;

import com.kat.coreessentials.combat.CombatManager;
import com.kat.coreessentials.combat.TeleportDelayManager;
import com.kat.coreessentials.combat.TeleportExecutor;
import com.kat.coreessentials.util.SafeLocationFinder;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RtpCommand {

    private RtpCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> build(JavaPlugin plugin, CombatManager combat, TeleportDelayManager delays) {
        Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

        return Commands.literal("rtp")
            .requires(source -> source.getSender().hasPermission("coreessentials.rtp"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use /rtp.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }

                long cooldownSeconds = plugin.getConfig().getLong("rtp.cooldown-seconds", 5);
                long now = System.currentTimeMillis();
                Long last = cooldowns.get(player.getUniqueId());
                if (last != null && !player.hasPermission("coreessentials.rtp.bypasscooldown")) {
                    long remaining = (last + cooldownSeconds * 1000L - now) / 1000L;
                    if (remaining > 0) {
                        player.sendMessage(Component.text("Wait " + remaining + "s before using /rtp again.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                }

                int minRadius = plugin.getConfig().getInt("rtp.min-radius", 100);
                int maxRadius = plugin.getConfig().getInt("rtp.radius", 1000);
                int maxAttempts = plugin.getConfig().getInt("rtp.max-attempts", 20);

                player.sendMessage(Component.text("Searching for a safe location...", NamedTextColor.YELLOW));

                Location safe = SafeLocationFinder.findSafeLocation(player.getWorld(), minRadius, maxRadius, maxAttempts);
                if (safe == null) {
                    player.sendMessage(Component.text("Couldn't find a safe location, try again.", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }

                boolean scheduled = TeleportExecutor.request(plugin, combat, delays, player, safe, "Teleported!");
                if (scheduled) {
                    cooldowns.put(player.getUniqueId(), now);
                }
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }
}

