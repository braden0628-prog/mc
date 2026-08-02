package com.kat.coreessentials.villager;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Shift + right-click a villager to open the trade cycling GUI instead of
 * the vanilla trade screen. Plain right-click is left completely alone, so
 * normal trading is unchanged.
 */
public class VillagerInteractListener implements Listener {

    private final TradeCycleGui gui;

    public VillagerInteractListener(TradeCycleGui gui) {
        this.gui = gui;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // fires twice otherwise (main hand + off hand)
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (!player.hasPermission("coreessentials.villagertrade.cycle")) {
            return;
        }
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }

        event.setCancelled(true);
        gui.open(player, villager);
    }
}
