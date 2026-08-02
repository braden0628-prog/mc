package com.kat.coreessentials.enchant;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gives farmers a chance to offer a "16 dirt -> 1 emerald" trade. Fires on
 * VillagerAcquireTradeEvent, which covers both natural trade generation and
 * re-rolls done through the trade cycling GUI.
 */
public class FarmerTradeListener implements Listener {

    private static final double OFFER_CHANCE = 0.25;

    @EventHandler
    public void onTradeAcquire(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.getProfession() != Villager.Profession.FARMER) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > OFFER_CHANCE) {
            return;
        }

        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.EMERALD, 1), 0, 16, true, 2, 0.05f);
        recipe.addIngredient(new ItemStack(Material.DIRT, 16));

        event.setRecipe(recipe);
    }
}
