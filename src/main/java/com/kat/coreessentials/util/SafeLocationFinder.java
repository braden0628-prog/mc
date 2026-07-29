package com.kat.coreessentials.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.concurrent.ThreadLocalRandom;

public final class SafeLocationFinder {

    private SafeLocationFinder() {
    }

    /**
     * Attempts to find a safe, standable location within an annulus of
     * [minRadius, maxRadius] around the world's spawn point.
     * Returns null if no safe spot was found within maxAttempts.
     */
    public static Location findSafeLocation(World world, int minRadius, int maxRadius, int maxAttempts) {
        Location center = world.getSpawnLocation();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = center.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * distance);

            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y, z);
            Block feet = ground.getRelative(0, 1, 0);
            Block head = ground.getRelative(0, 2, 0);

            if (!isSafeGround(ground.getType())) {
                continue;
            }
            if (!feet.getType().isAir() || !head.getType().isAir()) {
                continue;
            }

            return new Location(world, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    private static boolean isSafeGround(Material material) {
        if (!material.isSolid()) {
            return false;
        }
        return switch (material) {
            case LAVA, WATER, MAGMA_BLOCK, FIRE -> false;
            default -> true;
        };
    }
}
