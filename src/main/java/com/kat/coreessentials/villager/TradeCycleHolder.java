package com.kat.coreessentials.villager;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marker holder so we can recognise our own GUI in click events, and know
 * which villager it belongs to. Also carries the per-GUI "are you sure?"
 * state, so a pending confirmation dies with the GUI instead of leaking
 * into a later re-roll.
 */
public class TradeCycleHolder implements InventoryHolder {

    private final Villager villager;
    private Inventory inventory;
    private boolean awaitingConfirm;

    public TradeCycleHolder(Villager villager) {
        this.villager = villager;
    }

    public Villager getVillager() {
        return villager;
    }

    public boolean isAwaitingConfirm() {
        return awaitingConfirm;
    }

    public void setAwaitingConfirm(boolean awaitingConfirm) {
        this.awaitingConfirm = awaitingConfirm;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
