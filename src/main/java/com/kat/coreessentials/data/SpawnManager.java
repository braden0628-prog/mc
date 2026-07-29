package com.kat.coreessentials.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class SpawnManager {

    private final File file;
    private final YamlConfiguration yaml;
    private Location spawn;

    public SpawnManager(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "spawn.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        if (!yaml.contains("world")) {
            spawn = null;
            return;
        }
        World world = Bukkit.getWorld(yaml.getString("world", ""));
        if (world == null) {
            spawn = null;
            return;
        }
        spawn = new Location(
            world,
            yaml.getDouble("x"),
            yaml.getDouble("y"),
            yaml.getDouble("z"),
            (float) yaml.getDouble("yaw"),
            (float) yaml.getDouble("pitch")
        );
    }

    public void setSpawn(Location location) {
        this.spawn = location.clone();
        yaml.set("world", location.getWorld().getName());
        yaml.set("x", location.getX());
        yaml.set("y", location.getY());
        yaml.set("z", location.getZ());
        yaml.set("yaw", (double) location.getYaw());
        yaml.set("pitch", (double) location.getPitch());
        save();
    }

    /** Returns the configured spawn, or the default world spawn if none was set. */
    public Location getSpawn() {
        if (spawn != null) {
            return spawn.clone();
        }
        World defaultWorld = Bukkit.getWorlds().get(0);
        return defaultWorld.getSpawnLocation();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[CoreEssentials] Failed to save spawn.yml: " + e.getMessage());
        }
    }
}
