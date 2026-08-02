package com.kat.coreessentials.leaves;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Vanilla leaves eventually decay on their own once no log is within range,
 * but on a slow random timer that can leave floating leaves for a while
 * after chopping a tree. This speeds that up: whenever a log is broken, it
 * scans nearby non-persistent (naturally-grown) leaves and schedules an
 * early decay - with a small staggered random delay per leaf, so a chopped
 * tree's canopy crumbles down over roughly a second instead of vanishing
 * all at once - for any that are no longer within range of any log.
 * Player-placed leaves (persistent) are never touched.
 */
public class QuickLeafDecayListener implements Listener {

    private final JavaPlugin plugin;
    private final int sustainRadius;
    private final int minDelayTicks;
    private final int maxDelayTicks;

    public QuickLeafDecayListener(JavaPlugin plugin, int sustainRadius, int minDelayTicks, int maxDelayTicks) {
        this.plugin = plugin;
        this.sustainRadius = sustainRadius;
        this.minDelayTicks = minDelayTicks;
        this.maxDelayTicks = maxDelayTicks;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLogBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (!Tag.LOGS.isTagged(broken.getType())) {
            return;
        }

        int scanRadius = sustainRadius + 1;
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dy = -scanRadius; dy <= scanRadius; dy++) {
                for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                    Block candidate = broken.getRelative(dx, dy, dz);
                    if (isDecayableLeaves(candidate) && !isSustained(candidate)) {
                        scheduleDecay(candidate);
                    }
                }
            }
        }
    }

    private boolean isDecayableLeaves(Block block) {
        if (!Tag.LEAVES.isTagged(block.getType())) {
            return false;
        }
        return block.getBlockData() instanceof Leaves leaves && !leaves.isPersistent();
    }

    private boolean isSustained(Block leafBlock) {
        for (int dx = -sustainRadius; dx <= sustainRadius; dx++) {
            for (int dy = -sustainRadius; dy <= sustainRadius; dy++) {
                for (int dz = -sustainRadius; dz <= sustainRadius; dz++) {
                    if (Tag.LOGS.isTagged(leafBlock.getRelative(dx, dy, dz).getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void scheduleDecay(Block leafBlock) {
        long delay = ThreadLocalRandom.current().nextInt(minDelayTicks, maxDelayTicks + 1);
        Location loc = leafBlock.getLocation();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block current = loc.getBlock();
            if (isDecayableLeaves(current) && !isSustained(current)) {
                current.breakNaturally();
            }
        }, delay);
    }
}
