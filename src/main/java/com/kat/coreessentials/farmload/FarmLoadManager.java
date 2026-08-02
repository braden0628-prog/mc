package com.kat.coreessentials.farmload;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class FarmLoadManager {

    private final File file;
    private final YamlConfiguration yaml;
    private final Set<FarmZone> zones = new HashSet<>();

    public FarmLoadManager(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "farmload-zones.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getStringList("zones")) {
            zones.add(FarmZone.fromKey(key));
        }
    }

    /** Returns true if this chunk wasn't already registered. */
    public boolean addZone(FarmZone zone) {
        boolean added = zones.add(zone);
        if (added) {
            save();
        }
        return added;
    }

    /** Returns true if this chunk was registered and got removed. */
    public boolean removeZone(FarmZone zone) {
        boolean removed = zones.remove(zone);
        if (removed) {
            save();
        }
        return removed;
    }

    public Set<FarmZone> getZones() {
        return zones;
    }

    public int getZoneCount() {
        return zones.size();
    }

    private void save() {
        List<String> keys = zones.stream().map(FarmZone::key).toList();
        yaml.set("zones", keys);
        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "[CoreEssentials] Failed to save farmload-zones.yml: " + e.getMessage());
        }
    }
}
