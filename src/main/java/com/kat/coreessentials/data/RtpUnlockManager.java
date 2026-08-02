package com.kat.coreessentials.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RtpUnlockManager {

    public enum Dimension {
        OVERWORLD, NETHER, END
    }

    private final File file;
    private final YamlConfiguration yaml;

    public RtpUnlockManager(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "rtp-unlocks.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isUnlocked(UUID player, Dimension dimension) {
        return yaml.getBoolean(player + "." + dimension.name(), false);
    }

    public void setUnlocked(UUID player, Dimension dimension) {
        yaml.set(player + "." + dimension.name(), true);
        save();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[CoreEssentials] Failed to save rtp-unlocks.yml: " + e.getMessage());
        }
    }
}
