package com.kat.coreessentials.farmload;

/** One chunk registered for offline crop growth. */
public record FarmZone(String world, int chunkX, int chunkZ) {

    public String key() {
        return world + "," + chunkX + "," + chunkZ;
    }

    public static FarmZone fromKey(String key) {
        String[] parts = key.split(",");
        return new FarmZone(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}
