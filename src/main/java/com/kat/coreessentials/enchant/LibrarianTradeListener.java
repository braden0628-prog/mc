package com.kat.coreessentials.enchant;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.concurrent.ThreadLocalRandom;

public class LibrarianTradeListener implements Listener {

    // Chance (0-1) that a librarian restock includes a Vein Miner book offer.
    private static final double OFFER_CHANCE = 0.15;

    @EventHandler
    public void onTradeAcquire(VillagerAcquireTradeEvent event) {
        if (event.getEntity().getProfession() != Villager.Profession.LIBRARIAN) {
            return;
        }
        Enchantment veinMiner = VeinMiner.get();
        if (veinMiner == null) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > OFFER_CHANCE) {
            return;
        }

        int level = 1 + ThreadLocalRandom.current().nextInt(veinMiner.getMaxLevel());
        int emeraldCost = 8 + (level * 6);

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(veinMiner, level, true);
        book.setItemMeta(meta);

        MerchantRecipe recipe = new MerchantRecipe(book, 0, 8, true, 5, 0.05f);
        recipe.addIngredient(new ItemStack(Material.EMERALD, emeraldCost));
        recipe.addIngredient(new ItemStack(Material.BOOK, 1));

        event.setTrade(recipe);
    }
}
