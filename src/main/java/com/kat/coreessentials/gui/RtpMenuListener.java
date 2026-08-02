package com.kat.coreessentials.gui;

import com.kat.coreessentials.combat.CombatManager;
import com.kat.coreessentials.combat.TeleportDelayManager;
import com.kat.coreessentials.combat.TeleportExecutor;
import com.kat.coreessentials.data.RtpUnlockManager;
import com.kat.coreessentials.util.SafeLocationFinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RtpMenuListener implements Listener {

    private enum Dimension {
        OVERWORLD(RtpUnlockManager.Dimension.OVERWORLD, Material.DIRT, null, null,
            "Overworld", World.Environment.NORMAL),
        NETHER(RtpUnlockManager.Dimension.NETHER, Material.NETHERRACK,
            NamespacedKey.minecraft("story/enter_the_nether"), "Enter the Nether first!",
            "Nether", World.Environment.NETHER),
        END(RtpUnlockManager.Dimension.END, Material.END_STONE,
            NamespacedKey.minecraft("end/kill_dragon"), "Defeat the Dragon first!",
            "End", World.Environment.THE_END);

        final RtpUnlockManager.Dimension key;
        final Material costMaterial;
        final NamespacedKey advancementKey;
        final String requirementMessage;
        final String displayName;
        final World.Environment environment;

        Dimension(RtpUnlockManager.Dimension key, Material costMaterial, NamespacedKey advancementKey,
                  String requirementMessage, String displayName, World.Environment environment) {
            this.key = key;
            this.costMaterial = costMaterial;
            this.advancementKey = advancementKey;
            this.requirementMessage = requirementMessage;
            this.displayName = displayName;
            this.environment = environment;
        }
    }

    private final JavaPlugin plugin;
    private final RtpUnlockManager unlocks;
    private final CombatManager combat;
    private final TeleportDelayManager delays;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public RtpMenuListener(JavaPlugin plugin, RtpUnlockManager unlocks, CombatManager combat, TeleportDelayManager delays) {
        this.plugin = plugin;
        this.unlocks = unlocks;
        this.combat = combat;
        this.delays = delays;
    }

    public void open(Player player) {
        RtpMenuHolder holder = new RtpMenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Random Teleport"));
        holder.setInventory(inv);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(2, buildIcon(Dimension.OVERWORLD, player));
        inv.setItem(4, buildIcon(Dimension.NETHER, player));
        inv.setItem(6, buildIcon(Dimension.END, player));

        player.openInventory(inv);
    }

    private ItemStack buildIcon(Dimension dimension, Player player) {
        ItemStack item = new ItemStack(dimension.costMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(dimension.displayName + " RTP", NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(buildLore(player, dimension));
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> buildLore(Player player, Dimension dimension) {
        List<Component> lore = new ArrayList<>();

        if (unlocks.isUnlocked(player.getUniqueId(), dimension.key)) {
            lore.add(Component.text("Click to teleport!", NamedTextColor.GREEN));
            return lore;
        }

        if (dimension.advancementKey != null) {
            Advancement advancement = Bukkit.getAdvancement(dimension.advancementKey);
            boolean done = advancement != null && player.getAdvancementProgress(advancement).isDone();
            if (!done) {
                lore.add(Component.text("Locked", NamedTextColor.RED));
                lore.add(Component.text(dimension.requirementMessage, NamedTextColor.RED));
                return lore;
            }
        }

        lore.add(Component.text("Costs 1 " + prettyName(dimension.costMaterial) + " to unlock", NamedTextColor.GRAY));
        lore.add(Component.text("permanently.", NamedTextColor.GRAY));
        boolean hasItem = player.getInventory().containsAtLeast(new ItemStack(dimension.costMaterial), 1);
        if (hasItem) {
            lore.add(Component.text("Click to unlock!", NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("You need 1 " + prettyName(dimension.costMaterial) + ".", NamedTextColor.RED));
        }
        return lore;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof RtpMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Dimension dimension = switch (event.getSlot()) {
            case 2 -> Dimension.OVERWORLD;
            case 4 -> Dimension.NETHER;
            case 6 -> Dimension.END;
            default -> null;
        };
        if (dimension == null) {
            return;
        }

        handleClick(player, dimension);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RtpMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleClick(Player player, Dimension dimension) {
        if (unlocks.isUnlocked(player.getUniqueId(), dimension.key)) {
            attemptTeleport(player, dimension);
            return;
        }

        if (dimension.advancementKey != null) {
            Advancement advancement = Bukkit.getAdvancement(dimension.advancementKey);
            boolean done = advancement != null && player.getAdvancementProgress(advancement).isDone();
            if (!done) {
                player.closeInventory();
                player.sendMessage(Component.text(dimension.requirementMessage, NamedTextColor.RED));
                return;
            }
        }

        if (!tryConsume(player, dimension.costMaterial)) {
            player.closeInventory();
            player.sendMessage(Component.text(
                "You need 1 " + prettyName(dimension.costMaterial) + " in your inventory to unlock this.",
                NamedTextColor.RED
            ));
            return;
        }

        unlocks.setUnlocked(player.getUniqueId(), dimension.key);
        player.sendMessage(Component.text(dimension.displayName + " RTP unlocked permanently!", NamedTextColor.GREEN));
        attemptTeleport(player, dimension);
    }

    private void attemptTeleport(Player player, Dimension dimension) {
        long cooldownSeconds = plugin.getConfig().getLong("rtp.cooldown-seconds", 5);
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && !player.hasPermission("coreessentials.rtp.bypasscooldown")) {
            long remaining = (last + cooldownSeconds * 1000L - now) / 1000L;
            if (remaining > 0) {
                player.closeInventory();
                player.sendMessage(Component.text("Wait " + remaining + "s before using /rtp again.", NamedTextColor.RED));
                return;
            }
        }

        World world = findWorld(dimension.environment);
        if (world == null) {
            player.closeInventory();
            player.sendMessage(Component.text("That dimension isn't available on this server.", NamedTextColor.RED));
            return;
        }

        int minRadius = plugin.getConfig().getInt("rtp.min-radius", 100);
        int maxRadius = plugin.getConfig().getInt("rtp.radius", 1000);
        int maxAttempts = plugin.getConfig().getInt("rtp.max-attempts", 20);

        player.closeInventory();
        player.sendMessage(Component.text("Searching for a safe location...", NamedTextColor.YELLOW));

        Location safe = SafeLocationFinder.findSafeLocation(world, minRadius, maxRadius, maxAttempts);
        if (safe == null) {
            player.sendMessage(Component.text("Couldn't find a safe location, try again.", NamedTextColor.RED));
            return;
        }

        boolean scheduled = TeleportExecutor.request(plugin, combat, delays, player, safe, "Teleported!");
        if (scheduled) {
            cooldowns.put(player.getUniqueId(), now);
        }
    }

    private static World findWorld(World.Environment environment) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == environment) {
                return world;
            }
        }
        return null;
    }

    private static boolean tryConsume(Player player, Material material) {
        ItemStack toRemove = new ItemStack(material, 1);
        if (!player.getInventory().containsAtLeast(toRemove, 1)) {
            return false;
        }
        player.getInventory().removeItem(toRemove);
        return true;
    }

    private static String prettyName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
