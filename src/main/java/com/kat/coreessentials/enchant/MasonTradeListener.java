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
 * Gives masons a chance to offer a "1 emerald -> 16 stone" trade. Fires on
 * VillagerAcquireTradeEvent, which covers both natural trade generation and
 * re-rolls done through the trade cycling GUI.
 */
public class MasonTradeListener implements Listener {

    private static final double OFFER_CHANCE = 0.25;

    @EventHandler
    public void onTradeAcquire(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || villager.getProfession() != Villager.Profession.MASON) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > OFFER_CHANCE) {
            return;
        }

        MerchantRecipe recipe = new MerchantRecipe(new ItemStack(Material.STONE, 16), 0, 16, true, 2, 0.05f);
        recipe.addIngredient(new ItemStack(Material.EMERALD, 1));

        event.setRecipe(recipe);
    }
}
