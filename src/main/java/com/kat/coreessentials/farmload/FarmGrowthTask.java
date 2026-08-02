package com.kat.coreessentials.farmload;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Periodically advances crop growth in registered "farmload" zones - but
 * only for zones nobody is actually near, since vanilla already handles
 * growth correctly for any chunk a player is close enough to be ticking.
 * This checks each zone individually (not just "is anyone online at all"),
 * so a farm stays covered even while some of the team is online but off
 * somewhere else entirely. Loads each zone's chunk just long enough to roll
 * growth on any unripe crop found, then lets it fall back out of memory
 * naturally afterward (no permanent forceload tickets are kept).
 * This is our own approximation of growth (a tunable chance per crop per
 * pass), not a byte-perfect replica of vanilla's internal random-tick
 * formula. Covers the common hand-plantable age-based crops (wheat,
 * carrots, potatoes, beetroot, nether wart, cocoa, sweet berry bush) plus
 * sugar cane, which grows differently (stacking a new block on top up to
 * height 3, rather than an age value) and gets its own handling.
 * Deliberately skips melon/pumpkin stems, since maturing those also has to
 * spawn an adjacent fruit block, a side effect this doesn't replicate.
 */
public class FarmGrowthTask implements Runnable {

    private static final Set<Material> AGE_BASED_GROWABLE = EnumSet.of(
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
        Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH
    );

    private static final int SUGAR_CANE_MAX_HEIGHT = 3;

    private final JavaPlugin plugin;
    private final FarmLoadManager manager;
    private final double growChance;
    private final double nearbyRadiusBlocks;

    public FarmGrowthTask(JavaPlugin plugin, FarmLoadManager manager, double growChance, double nearbyRadiusBlocks) {
        this.plugin = plugin;
        this.manager = manager;
        this.growChance = growChance;
        this.nearbyRadiusBlocks = nearbyRadiusBlocks;
    }

    @Override
    public void run() {
        if (manager.getZones().isEmpty()) {
            return;
        }

        for (FarmZone zone : manager.getZones()) {
            World world = Bukkit.getWorld(zone.world());
            if (world == null || isPlayerNearby(world, zone)) {
                continue;
            }
            world.getChunkAtAsync(zone.chunkX(), zone.chunkZ(), true).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(plugin, () -> growChunk(chunk))
            );
        }
    }

    /** True if any online player in this world is close enough that vanilla is already ticking this zone naturally. */
    private boolean isPlayerNearby(World world, FarmZone zone) {
        double centerX = zone.chunkX() * 16 + 8;
        double centerZ = zone.chunkZ() * 16 + 8;

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            double dx = loc.getX() - centerX;
            double dz = loc.getZ() - centerZ;
            if (Math.sqrt(dx * dx + dz * dz) <= nearbyRadiusBlocks) {
                return true;
            }
        }
        return false;
    }

    private void growChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material type = block.getType();

                    if (type == Material.SUGAR_CANE) {
                        tryGrowSugarCane(block);
                        continue;
                    }

                    if (!AGE_BASED_GROWABLE.contains(type)) {
                        continue;
                    }
                    BlockData data = block.getBlockData();
                    if (!(data instanceof Ageable ageable)) {
                        continue;
                    }
                    if (ageable.getAge() >= ageable.getMaximumAge()) {
                        continue;
                    }
                    if (ThreadLocalRandom.current().nextDouble() < growChance) {
                        ageable.setAge(ageable.getAge() + 1);
                        block.setBlockData(ageable);
                    }
                }
            }
        }
    }

    /** Adds one block to the top of a sugar cane stalk, up to max height, if there's room and the roll succeeds. */
    private void tryGrowSugarCane(Block base) {
        Block above = base.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            return; // not the top of the stalk, or something's blocking it
        }

        int height = 0;
        Block check = base;
        while (check.getType() == Material.SUGAR_CANE) {
            height++;
            check = check.getRelative(0, -1, 0);
        }
        if (height >= SUGAR_CANE_MAX_HEIGHT) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < growChance) {
            above.setType(Material.SUGAR_CANE);
        }
    }
}
