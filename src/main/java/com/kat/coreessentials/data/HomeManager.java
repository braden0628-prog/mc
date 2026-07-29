package com.kat.coreessentials.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HomeManager {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public HomeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private int maxHomes() {
        return plugin.getConfig().getInt("homes.max-homes", 3);
    }

    public List<String> listHomes(UUID player) {
        ConfigurationSection section = yaml.getConfigurationSection(player.toString());
        if (section == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    public boolean hasHome(UUID player, String name) {
        return yaml.contains(player + "." + name.toLowerCase());
    }

    /** Returns false if the player is already at their max home count. */
    public boolean setHome(UUID player, String name, Location location) {
        String key = name.toLowerCase();
        boolean alreadyExists = hasHome(player, key);
        if (!alreadyExists && listHomes(player).size() >= maxHomes()) {
            return false;
        }

        String path = player + "." + key;
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".x", location.getX());
        yaml.set(path + ".y", location.getY());
        yaml.set(path + ".z", location.getZ());
        yaml.set(path + ".yaw", (double) location.getYaw());
        yaml.set(path + ".pitch", (double) location.getPitch());
        save();
        return true;
    }

    public boolean deleteHome(UUID player, String name) {
        String path = player + "." + name.toLowerCase();
        if (!yaml.contains(path)) {
            return false;
        }
        yaml.set(path, null);
        save();
        return true;
    }

    public Location getHome(UUID player, String name) {
        String path = player + "." + name.toLowerCase();
        if (!yaml.contains(path)) {
            return null;
        }
        String worldName = yaml.getString(path + ".world");
        World world = Bukkit.getWorld(worldName == null ? "" : worldName);
        if (world == null) {
            return null;
        }
        return new Location(
            world,
            yaml.getDouble(path + ".x"),
            yaml.getDouble(path + ".y"),
            yaml.getDouble(path + ".z"),
            (float) yaml.getDouble(path + ".yaw"),
            (float) yaml.getDouble(path + ".pitch")
        );
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[CoreEssentials] Failed to save homes.yml: " + e.getMessage());
        }
    }
}
