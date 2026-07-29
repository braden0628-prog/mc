package com.kat.coreessentials;

import com.kat.coreessentials.enchant.VeinMiner;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper plugin bootstrapper.
 *
 * Custom enchantments on modern Paper (26.x) are real, data-driven registry
 * entries - not a fake lore/PDC trick. They MUST be registered here, during
 * the server bootstrap phase, via RegistryEvents.ENCHANTMENT.compose().
 * Once registered this way, Vein Miner behaves like a real vanilla
 * enchantment: it shows up at the enchanting table, can be combined via
 * anvil, and can be offered by librarian villagers.
 *
 * Vein Miner is supported on BOTH pickaxes (chains ores) and axes (chains
 * logs) - since Paper doesn't expose a ready-made "pickaxes OR axes" tag,
 * the supported set is built explicitly from every pickaxe/axe item key.
 */
public class CoreEssentialsBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
            var supportedTools = RegistrySet.keySet(
                RegistryKey.ITEM,
                ItemTypeKeys.WOODEN_PICKAXE, ItemTypeKeys.STONE_PICKAXE, ItemTypeKeys.IRON_PICKAXE,
                ItemTypeKeys.GOLDEN_PICKAXE, ItemTypeKeys.DIAMOND_PICKAXE, ItemTypeKeys.NETHERITE_PICKAXE,
                ItemTypeKeys.WOODEN_AXE, ItemTypeKeys.STONE_AXE, ItemTypeKeys.IRON_AXE,
                ItemTypeKeys.GOLDEN_AXE, ItemTypeKeys.DIAMOND_AXE, ItemTypeKeys.NETHERITE_AXE
            );

            event.registry().register(
                VeinMiner.TYPED_KEY,
                builder -> builder
                    .description(Component.text("Vein Miner"))
                    .supportedItems(supportedTools)
                    .anvilCost(4)
                    .maxLevel(3)
                    .weight(2)
                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(15, 9))
                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(45, 9))
                    .activeSlots(EquipmentSlotGroup.MAINHAND)
            );
        }));
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        return new CoreEssentials();
    }
}
