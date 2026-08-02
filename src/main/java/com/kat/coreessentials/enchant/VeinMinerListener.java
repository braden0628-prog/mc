package com.kat.coreessentials.enchant;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

public class VeinMinerListener implements Listener {

    private final JavaPlugin plugin;

    public VeinMinerListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!VeinMiner.isVeinBlock(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!VeinMiner.toolMatchesBlock(block.getType(), tool.getType())) {
            return;
        }

        boolean hasEnchant = VeinMiner.levelOn(tool) > 0;
        if (!hasEnchant) {
            return;
        }

        FileConfiguration config = plugin.getConfig();
        int maxBlocks = config.getInt("vein-miner.blocks", 192);

        Set<Block> connected = VeinMiner.findConnected(block, maxBlocks);
        if (connected.isEmpty()) {
            return;
        }

        for (Block extra : connected) {
            if (tool.getAmount() <= 0) {
                break;
            }
            boolean toolSurvived = VeinMiner.damageTool(tool);
            extra.breakNaturally(tool);
            if (!toolSurvived) {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                break;
            }
        }
    }
}
