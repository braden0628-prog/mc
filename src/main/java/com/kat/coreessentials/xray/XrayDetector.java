package com.kat.coreessentials.xray;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects likely X-ray usage by scoring each player's mining behavior:
 * valuable ores add to a score, ordinary digging (stone, deepslate, etc.)
 * slowly decays it. Legitimate miners dig huge amounts of junk per ore
 * found; X-ray users find ore with very little digging in between, so
 * their score climbs fast. This is a detection/alerting layer, not
 * prevention - see the README for why Paper's native anti-xray setting
 * is still the actual fix.
 */
public class XrayDetector implements Listener {

    private static final Map<Material, Double> ORE_WEIGHTS = new EnumMap<>(Material.class);

    static {
        ORE_WEIGHTS.put(Material.DIAMOND_ORE, 10.0);
        ORE_WEIGHTS.put(Material.DEEPSLATE_DIAMOND_ORE, 10.0);
        ORE_WEIGHTS.put(Material.ANCIENT_DEBRIS, 12.0);
        ORE_WEIGHTS.put(Material.EMERALD_ORE, 10.0);
        ORE_WEIGHTS.put(Material.DEEPSLATE_EMERALD_ORE, 10.0);
        ORE_WEIGHTS.put(Material.GOLD_ORE, 4.0);
        ORE_WEIGHTS.put(Material.DEEPSLATE_GOLD_ORE, 4.0);
        ORE_WEIGHTS.put(Material.NETHER_GOLD_ORE, 4.0);
    }

    private static final double JUNK_DECAY = 0.5;

    private final JavaPlugin plugin;
    private final double alertThreshold;
    private final long alertCooldownMillis;
    private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();

    public XrayDetector(JavaPlugin plugin, double alertThreshold, long alertCooldownMinutes) {
        this.plugin = plugin;
        this.alertThreshold = alertThreshold;
        this.alertCooldownMillis = alertCooldownMinutes * 60_000L;
    }

    private static class PlayerStats {
        double score;
        int totalMined;
        int oreMined;
        long lastAlertMillis;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("coreessentials.xray.bypass")) {
            return;
        }

        Material type = event.getBlock().getType();
        PlayerStats s = stats.computeIfAbsent(player.getUniqueId(), k -> new PlayerStats());
        s.totalMined++;

        Double weight = ORE_WEIGHTS.get(type);
        if (weight != null) {
            s.score += weight;
            s.oreMined++;
            checkAlert(player, s, type);
        } else if (type.isSolid()) {
            s.score = Math.max(0, s.score - JUNK_DECAY);
        }
    }

    private void checkAlert(Player player, PlayerStats s, Material foundType) {
        if (s.score < alertThreshold) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - s.lastAlertMillis < alertCooldownMillis) {
            return;
        }
        s.lastAlertMillis = now;

        Location loc = player.getLocation();
        String coords = loc.getWorld().getName() + " x:" + loc.getBlockX() + " y:" + loc.getBlockY() + " z:" + loc.getBlockZ();
        String summary = player.getName() + " mining " + foundType.name() + " at an unusual rate "
            + "(score " + String.format("%.1f", s.score) + ", " + s.oreMined + " valuable ores / "
            + s.totalMined + " blocks mined). Possible X-ray. At " + coords;

        plugin.getLogger().warning("[XrayWatch] " + summary);

        Component tp = Component.text(" [TP]", NamedTextColor.AQUA)
            .clickEvent(ClickEvent.runCommand("/tp " + player.getName()));
        Component alert = Component.text("[XrayWatch] " + summary, NamedTextColor.RED).append(tp);

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("coreessentials.xray.alerts")) {
                staff.sendMessage(alert);
            }
        }
    }

    /** Human-readable current stats for a player, for /xraycheck. */
    public String describe(UUID playerId) {
        PlayerStats s = stats.get(playerId);
        if (s == null) {
            return "No mining data recorded yet.";
        }
        return "score " + String.format("%.1f", s.score) + " (alert at " + alertThreshold + ") | "
            + s.oreMined + " valuable ores / " + s.totalMined + " total blocks mined";
    }

    public void reset(UUID playerId) {
        stats.remove(playerId);
    }
}
