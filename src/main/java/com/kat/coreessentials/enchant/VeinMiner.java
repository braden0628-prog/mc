package com.kat.coreessentials.enchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Core logic for the Vein Miner enchantment: breaking one ore block with an
 * enchanted pickaxe chain-breaks connected ore blocks of the same type.
 */
public final class VeinMiner {

    /** Namespaced key this plugin registers the enchantment under. */
    public static final Key KEY = Key.key("coreessentials", "vein_miner");

    /** Typed key used to look up the registered Enchantment at runtime. */
    public static final TypedKey<Enchantment> TYPED_KEY = EnchantmentKeys.create(KEY);

    // Ore blocks - chainable only when the tool in hand is a pickaxe.
    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
        Material.ANCIENT_DEBRIS
    );

    // Log blocks - chainable only when the tool in hand is an axe.
    private static final Set<Material> LOG_BLOCKS = EnumSet.of(
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG, Material.CHERRY_LOG,
        Material.CRIMSON_STEM, Material.WARPED_STEM
    );

    private VeinMiner() {
    }

    public static boolean isVeinBlock(Material material) {
        return ORE_BLOCKS.contains(material) || LOG_BLOCKS.contains(material);
    }

    public static boolean isPickaxe(Material toolType) {
        return toolType.name().endsWith("_PICKAXE");
    }

    public static boolean isAxe(Material toolType) {
        return toolType.name().endsWith("_AXE");
    }

    /**
     * Whether this tool type is even allowed to chain-break this block type:
     * pickaxes only chain ores, axes only chain logs. Prevents an axe
     * chaining ore (or vice versa) even though both hold the enchant.
     */
    public static boolean toolMatchesBlock(Material blockType, Material toolType) {
        if (ORE_BLOCKS.contains(blockType)) {
            return isPickaxe(toolType);
        }
        if (LOG_BLOCKS.contains(blockType)) {
            return isAxe(toolType);
        }
        return false;
    }

    /**
     * Resolves the live Enchantment instance from the registry. Returns null
     * if, for whatever reason, it wasn't registered (should not happen once
     * the bootstrapper has run).
     */
    public static Enchantment get() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(TYPED_KEY);
    }

    public static int levelOn(ItemStack tool) {
        Enchantment enchantment = get();
        if (enchantment == null || tool == null) {
            return 0;
        }
        return tool.getEnchantmentLevel(enchantment);
    }

    /**
     * Finds every block connected to {@code origin} that shares its Material,
     * via a 6-directional flood fill, capped at {@code maxBlocks}.
     * The origin block itself is NOT included in the result - it is broken
     * normally by the game before this runs.
     */
    public static Set<Block> findConnected(Block origin, int maxBlocks) {
        Material target = origin.getType();
        Set<Block> visited = new HashSet<>();
        Set<Block> result = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        int[][] offsets = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
        };

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            for (int[] offset : offsets) {
                Block neighbor = current.getRelative(offset[0], offset[1], offset[2]);
                if (visited.contains(neighbor)) {
                    continue;
                }
                visited.add(neighbor);
                if (neighbor.getType() == target) {
                    result.add(neighbor);
                    queue.add(neighbor);
                    if (result.size() >= maxBlocks) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Applies one point of durability damage to the tool. Returns false if
     * the tool broke (caller should stop the chain and remove the item).
     */
    public static boolean damageTool(ItemStack tool) {
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return true;
        }
        int max = tool.getType().getMaxDurability();
        if (max <= 0) {
            return true;
        }
        damageable.setDamage(damageable.getDamage() + 1);
        tool.setItemMeta((ItemMeta) damageable);
        return damageable.getDamage() < max;
    }
}
