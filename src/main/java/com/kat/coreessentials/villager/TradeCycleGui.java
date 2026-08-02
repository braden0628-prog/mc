package com.kat.coreessentials.villager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom chest GUI for cycling a villager's trades. A server-side plugin
 * can't draw new buttons into the vanilla trade screen, so this opens our
 * own inventory instead - every slot is click-locked, and the re-roll
 * arrow is a real clickable item.
 *
 * Re-rolling works by resetting the villager's profession to NONE and back,
 * which makes vanilla itself regenerate a genuine trade list (correct
 * prices, correct enchantment weighting) rather than us approximating it.
 * Level and experience are preserved across the reset.
 */
public class TradeCycleGui implements Listener {

    private static final int SIZE = 27;
    private static final int REROLL_SLOT = 22;
    private static final int TRADE_SLOT = 26;
    private static final int MAX_TRADE_DISPLAY = 18;

    private final JavaPlugin plugin;
    private final int goodPriceThreshold;
    private final boolean warnBeforeRerolling;
    private final boolean effectsEnabled;

    public TradeCycleGui(JavaPlugin plugin, int goodPriceThreshold, boolean warnBeforeRerolling, boolean effectsEnabled) {
        this.plugin = plugin;
        this.goodPriceThreshold = goodPriceThreshold;
        this.warnBeforeRerolling = warnBeforeRerolling;
        this.effectsEnabled = effectsEnabled;
    }

    public void open(Player player, Villager villager) {
        TradeCycleHolder holder = new TradeCycleHolder(villager);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
            Component.text(professionName(villager), NamedTextColor.DARK_GREEN));
        holder.setInventory(inv);
        render(inv, holder);
        player.openInventory(inv);
    }

    /** Redraws the whole GUI from the villager's current trades. */
    private void render(Inventory inv, TradeCycleHolder holder) {
        inv.clear();
        Villager villager = holder.getVillager();

        List<MerchantRecipe> recipes = villager.getRecipes();
        for (int i = 0; i < recipes.size() && i < MAX_TRADE_DISPLAY; i++) {
            inv.setItem(i, displayItemFor(recipes.get(i)));
        }

        inv.setItem(REROLL_SLOT, holder.isAwaitingConfirm() ? confirmButton(villager) : rerollButton(villager));
        inv.setItem(TRADE_SLOT, tradeButton());
    }

    /** A display-only copy of a trade's result, with its cost in the lore. */
    private ItemStack displayItemFor(MerchantRecipe recipe) {
        ItemStack display = recipe.getResult().clone();
        ItemMeta meta = display.getItemMeta();

        List<Component> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.lore());
        }
        lore.add(Component.empty());
        lore.add(Component.text("Cost:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        for (ItemStack ingredient : recipe.getIngredients()) {
            if (ingredient == null || ingredient.getType().isAir()) {
                continue;
            }
            lore.add(Component.text("  " + ingredient.getAmount() + "x " + prettyMaterial(ingredient.getType()),
                NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        }

        TradeQuality.Hit hit = TradeQuality.evaluate(recipe);
        if (hit != null) {
            lore.add(Component.empty());
            lore.add(Component.text("★ Good trade", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack rerollButton(Villager villager) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Re-roll Trades", NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (hasBeenTradedWith(villager)) {
            lore.add(Component.text("This villager has already been", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("traded with - trades are locked.", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Click to generate a fresh set", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("of trades for this villager.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack confirmButton(Villager villager) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚠ Are you sure?", NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("You're about to lose:", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        for (MerchantRecipe recipe : villager.getRecipes()) {
            TradeQuality.Hit hit = TradeQuality.evaluate(recipe);
            if (hit != null) {
                lore.add(Component.text("  " + hit.displayName() + " (" + hit.emeraldCost() + " emeralds)",
                    NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click again to confirm re-roll.", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tradeButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Open Trades", NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Open the real trade screen to buy.", NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeCycleHolder holder)) {
            return;
        }
        // Nothing in this GUI is ever takeable - cancel every click, including
        // shift-clicks and number-key swaps from the player's own inventory.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        int slot = event.getSlot();
        if (slot == TRADE_SLOT) {
            Villager villager = holder.getVillager();
            player.closeInventory();
            player.openMerchant(villager, true);
            return;
        }
        if (slot == REROLL_SLOT) {
            handleRerollClick(player, holder, event.getInventory());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TradeCycleHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // A pending "are you sure?" dies with the GUI so it can't leak into a later roll.
        if (event.getInventory().getHolder() instanceof TradeCycleHolder holder) {
            holder.setAwaitingConfirm(false);
        }
    }

    private void handleRerollClick(Player player, TradeCycleHolder holder, Inventory inv) {
        Villager villager = holder.getVillager();

        if (!villager.isValid()) {
            player.sendMessage(Component.text("That villager is gone.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        if (hasBeenTradedWith(villager)) {
            player.sendMessage(Component.text(
                "This villager has already been traded with - its trades are locked for good.",
                NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        Villager.Profession profession = villager.getProfession();
        if (profession == Villager.Profession.NONE || profession == Villager.Profession.NITWIT) {
            player.sendMessage(Component.text("This villager has no profession to re-roll.", NamedTextColor.RED));
            return;
        }

        // First click on a set containing something good just arms the warning.
        if (warnBeforeRerolling && !holder.isAwaitingConfirm() && hasGoodTrade(villager)) {
            holder.setAwaitingConfirm(true);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            render(inv, holder);
            return;
        }

        holder.setAwaitingConfirm(false);
        reroll(villager);
        render(inv, holder);
        playResultEffects(player, villager);
    }

    /**
     * Resets the profession to NONE and back, which makes vanilla regenerate
     * a real trade list. Level and experience are preserved so a leveled-up
     * villager isn't knocked back to Novice.
     */
    private void reroll(Villager villager) {
        Villager.Profession profession = villager.getProfession();
        int level = villager.getVillagerLevel();
        int experience = villager.getVillagerExperience();

        villager.setProfession(Villager.Profession.NONE);
        villager.setProfession(profession);

        villager.setVillagerLevel(level);
        villager.setVillagerExperience(experience);
    }

    private void playResultEffects(Player player, Villager villager) {
        if (!effectsEnabled) {
            return;
        }

        TradeQuality.Hit best = null;
        for (MerchantRecipe recipe : villager.getRecipes()) {
            TradeQuality.Hit hit = TradeQuality.evaluate(recipe);
            if (hit == null) {
                continue;
            }
            // Prefer the cheapest good trade, since that's the more exciting outcome.
            if (best == null || hit.emeraldCost() < best.emeraldCost()) {
                best = hit;
            }
        }

        if (best == null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            return;
        }

        boolean great = best.emeraldCost() > 0 && best.emeraldCost() <= goodPriceThreshold;
        if (great) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            villager.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                villager.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0.3);
            player.showTitle(Title.title(
                Component.text("GREAT ROLL!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(best.displayName() + " - only " + best.emeraldCost() + " emeralds!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(600))
            ));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            villager.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                villager.getLocation().add(0, 1, 0), 25, 0.5, 0.8, 0.5, 0.1);
            player.sendActionBar(Component.text("Good roll! " + best.displayName()
                + " (" + best.emeraldCost() + " emeralds)", NamedTextColor.GREEN));
        }
    }

    private static boolean hasGoodTrade(Villager villager) {
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (TradeQuality.evaluate(recipe) != null) {
                return true;
            }
        }
        return false;
    }

    /** Vanilla locks trades once a villager has been traded with - mirror that rule. */
    private static boolean hasBeenTradedWith(Villager villager) {
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (recipe.getUses() > 0) {
                return true;
            }
        }
        return false;
    }

    private static String professionName(Villager villager) {
        return prettyMaterialLike(villager.getProfession().toString());
    }

    private static String prettyMaterial(Material material) {
        return prettyMaterialLike(material.toString());
    }

    private static String prettyMaterialLike(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }
}
