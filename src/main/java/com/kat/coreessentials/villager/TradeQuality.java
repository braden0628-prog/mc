package com.kat.coreessentials.villager;

import com.kat.coreessentials.enchant.VeinMiner;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for "is this trade worth keeping?" - used by both
 * the re-roll effects and the are-you-sure warning, so the two can never
 * disagree about what counts as good.
 */
public final class TradeQuality {

    /** Enchantment key -> minimum level at which it counts as a good roll. */
    private static final Map<String, Integer> GOOD_ENCHANTS = new LinkedHashMap<>();

    static {
        GOOD_ENCHANTS.put("minecraft:mending", 1);
        GOOD_ENCHANTS.put("minecraft:sharpness", 5);
        GOOD_ENCHANTS.put("minecraft:efficiency", 5);
        GOOD_ENCHANTS.put("minecraft:protection", 4);
        GOOD_ENCHANTS.put("minecraft:unbreaking", 3);
        GOOD_ENCHANTS.put("minecraft:fortune", 3);
        GOOD_ENCHANTS.put("minecraft:looting", 3);
        GOOD_ENCHANTS.put("minecraft:fire_aspect", 2);
        // Our own Vein Miner enchant counts at any level (it only has one).
        GOOD_ENCHANTS.put(VeinMiner.KEY.asString(), 1);
    }

    /** A good trade that was found, with its display name and emerald cost. */
    public record Hit(String displayName, int emeraldCost) {
    }

    private TradeQuality() {
    }

    /**
     * Returns a Hit if this recipe is a "good" trade, otherwise null.
     * Only enchanted books are considered - that's where the value is.
     */
    public static Hit evaluate(MerchantRecipe recipe) {
        ItemStack result = recipe.getResult();
        if (result.getType() != Material.ENCHANTED_BOOK) {
            return null;
        }
        if (!(result.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return null;
        }

        for (Map.Entry<Enchantment, Integer> stored : meta.getStoredEnchants().entrySet()) {
            String key = stored.getKey().getKey().toString();
            Integer minLevel = GOOD_ENCHANTS.get(key);
            if (minLevel != null && stored.getValue() >= minLevel) {
                String name = prettyName(key) + (stored.getValue() > 1 ? " " + roman(stored.getValue()) : "");
                return new Hit(name, emeraldCost(recipe));
            }
        }
        return null;
    }

    /** Total emeralds this recipe asks for across all its ingredients. */
    public static int emeraldCost(MerchantRecipe recipe) {
        int total = 0;
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient != null && ingredient.getType() == Material.EMERALD) {
                total += ingredient.getAmount();
            }
        }
        return total;
    }

    /** "minecraft:fire_aspect" -> "Fire Aspect" */
    private static String prettyName(String key) {
        String raw = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
        String[] parts = raw.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
