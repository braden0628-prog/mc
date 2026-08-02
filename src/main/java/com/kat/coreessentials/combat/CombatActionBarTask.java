package com.kat.coreessentials.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CombatActionBarTask implements Runnable {

    private final CombatManager combat;

    public CombatActionBarTask(CombatManager combat) {
        this.combat = combat;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (combat.isInCombat(player.getUniqueId())) {
                long remaining = combat.secondsRemaining(player.getUniqueId());
                player.sendActionBar(Component.text("\u2694 Combat: " + remaining + "s", NamedTextColor.RED));
            }
        }
    }
}
