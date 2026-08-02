package com.kat.coreessentials;

import com.kat.coreessentials.chat.TeamChatListener;
import com.kat.coreessentials.combat.CombatActionBarTask;
import com.kat.coreessentials.combat.CombatListener;
import com.kat.coreessentials.combat.CombatManager;
import com.kat.coreessentials.combat.TeamFriendlyFireListener;
import com.kat.coreessentials.combat.TeleportDelayManager;
import com.kat.coreessentials.combat.TeleportExecutor;
import com.kat.coreessentials.combat.TeleportMoveListener;
import com.kat.coreessentials.commands.FarmLoadCommand;
import com.kat.coreessentials.commands.HomeCommand;
import com.kat.coreessentials.commands.RtpCommand;
import com.kat.coreessentials.commands.SetSpawnCommand;
import com.kat.coreessentials.commands.SpawnCommand;
import com.kat.coreessentials.commands.TeamCommand;
import com.kat.coreessentials.commands.TeamInfoCommand;
import com.kat.coreessentials.commands.TeamMsgCommand;
import com.kat.coreessentials.commands.TphCommand;
import com.kat.coreessentials.commands.TprCommand;
import com.kat.coreessentials.commands.XrayCheckCommand;
import com.kat.coreessentials.data.HomeManager;
import com.kat.coreessentials.data.RtpUnlockManager;
import com.kat.coreessentials.data.SpawnManager;
import com.kat.coreessentials.data.TeamManager;
import com.kat.coreessentials.data.TeleportRequestManager;
import com.kat.coreessentials.enchant.FarmerTradeListener;
import com.kat.coreessentials.enchant.LibrarianTradeListener;
import com.kat.coreessentials.enchant.MasonTradeListener;
import com.kat.coreessentials.enchant.VeinMinerListener;
import com.kat.coreessentials.farmload.FarmGrowthTask;
import com.kat.coreessentials.farmload.FarmLoadManager;
import com.kat.coreessentials.gui.RtpMenuListener;
import com.kat.coreessentials.leaves.QuickLeafDecayListener;
import com.kat.coreessentials.villager.TradeCycleGui;
import com.kat.coreessentials.villager.VillagerInteractListener;
import com.kat.coreessentials.xray.XrayDetector;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public class CoreEssentials extends JavaPlugin {

    private HomeManager homeManager;
    private TeamManager teamManager;
    private SpawnManager spawnManager;
    private TeleportRequestManager teleportRequestManager;
    private CombatManager combatManager;
    private TeleportDelayManager teleportDelayManager;
    private RtpUnlockManager rtpUnlockManager;
    private RtpMenuListener rtpMenuListener;
    private XrayDetector xrayDetector;
    private FarmLoadManager farmLoadManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.homeManager = new HomeManager(this);
        this.teamManager = new TeamManager(this);
        this.spawnManager = new SpawnManager(this);
        this.teleportRequestManager = new TeleportRequestManager(getConfig().getLong("tpr.expire-seconds", 60));
        this.combatManager = new CombatManager();
        this.teleportDelayManager = new TeleportDelayManager();
        this.rtpUnlockManager = new RtpUnlockManager(this);
        this.farmLoadManager = new FarmLoadManager(this);
        this.rtpMenuListener = new RtpMenuListener(this, rtpUnlockManager, combatManager, teleportDelayManager);
        this.xrayDetector = new XrayDetector(
            this,
            getConfig().getDouble("xray.alert-threshold", 20.0),
            getConfig().getLong("xray.alert-cooldown-minutes", 5)
        );
        TeleportExecutor.delaySeconds = getConfig().getInt("teleport.delay-seconds", 5);

        registerPermissions();

        Bukkit.getPluginManager().registerEvents(new VeinMinerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LibrarianTradeListener(), this);
        Bukkit.getPluginManager().registerEvents(new FarmerTradeListener(), this);
        Bukkit.getPluginManager().registerEvents(new MasonTradeListener(), this);
        Bukkit.getPluginManager().registerEvents(new TeamChatListener(teamManager), this);
        Bukkit.getPluginManager().registerEvents(new TeamFriendlyFireListener(teamManager), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(
            combatManager, teleportDelayManager,
            getConfig().getLong("combat.pvp-seconds", 30),
            getConfig().getLong("combat.pve-seconds", 15)
        ), this);
        Bukkit.getPluginManager().registerEvents(new TeleportMoveListener(teleportDelayManager), this);
        Bukkit.getPluginManager().registerEvents(rtpMenuListener, this);
        Bukkit.getPluginManager().registerEvents(xrayDetector, this);
        if (getConfig().getBoolean("leaf-decay.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new QuickLeafDecayListener(
                this,
                getConfig().getInt("leaf-decay.sustain-radius", 4),
                getConfig().getInt("leaf-decay.min-delay-ticks", 5),
                getConfig().getInt("leaf-decay.max-delay-ticks", 40)
            ), this);
        }
        if (getConfig().getBoolean("trade-cycling.enabled", true)) {
            TradeCycleGui tradeCycleGui = new TradeCycleGui(
                this,
                getConfig().getInt("trade-cycling.good-price-threshold", 20),
                getConfig().getBoolean("trade-cycling.warn-before-rerolling-good", true),
                getConfig().getBoolean("trade-cycling.effects", true)
            );
            Bukkit.getPluginManager().registerEvents(tradeCycleGui, this);
            Bukkit.getPluginManager().registerEvents(new VillagerInteractListener(tradeCycleGui), this);
        }
        Bukkit.getScheduler().runTaskTimer(this, new CombatActionBarTask(combatManager), 0L, 20L);
        if (getConfig().getBoolean("farmload.enabled", true)) {
            long intervalTicks = getConfig().getLong("farmload.interval-seconds", 15) * 20L;
            Bukkit.getScheduler().runTaskTimer(this, new FarmGrowthTask(
                this, farmLoadManager,
                getConfig().getDouble("farmload.grow-chance", 0.15),
                getConfig().getDouble("farmload.player-nearby-radius", 128)
            ), intervalTicks, intervalTicks);
        }

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(TprCommand.build(teleportRequestManager), "Send a teleport request to another player.");
            commands.registrar().register(TphCommand.build(teleportRequestManager), "Ask another player to teleport to you.");
            commands.registrar().register(TprCommand.buildAccept(this, teleportRequestManager, combatManager, teleportDelayManager));
            commands.registrar().register(TprCommand.buildDeny(teleportRequestManager));
            commands.registrar().register(HomeCommand.build(this, homeManager, combatManager, teleportDelayManager), "Manage and teleport to your homes.");
            commands.registrar().register(TeamCommand.build(teamManager), "Manage your team.");
            commands.registrar().register(TeamInfoCommand.build(teamManager), "View a team's name and roster.");
            commands.registrar().register(TeamMsgCommand.build(teamManager), "Send a message to your team.");
            commands.registrar().register(RtpCommand.build(rtpMenuListener), "Open the random teleport destination menu.");
            commands.registrar().register(SpawnCommand.build(this, spawnManager, combatManager, teleportDelayManager), "Teleport to spawn.");
            commands.registrar().register(SetSpawnCommand.build(spawnManager), "Set the server spawn location (op only).");
            commands.registrar().register(XrayCheckCommand.build(xrayDetector), "Check a player's X-ray detection score.");
            commands.registrar().register(FarmLoadCommand.build(farmLoadManager), "Register chunks around you for offline crop growth.");
        });

        getLogger().info("CoreEssentials enabled.");
    }

    private void registerPermissions() {
        var pm = Bukkit.getPluginManager();
        registerIfAbsent(pm, "coreessentials.tpr", PermissionDefault.TRUE);
        registerIfAbsent(pm, "coreessentials.home", PermissionDefault.TRUE);
        registerIfAbsent(pm, "coreessentials.team", PermissionDefault.TRUE);
        registerIfAbsent(pm, "coreessentials.rtp", PermissionDefault.TRUE);
        registerIfAbsent(pm, "coreessentials.rtp.bypasscooldown", PermissionDefault.OP);
        registerIfAbsent(pm, "coreessentials.xray.alerts", PermissionDefault.OP);
        registerIfAbsent(pm, "coreessentials.xray.bypass", PermissionDefault.OP);
        registerIfAbsent(pm, "coreessentials.farmload", PermissionDefault.TRUE);
        registerIfAbsent(pm, "coreessentials.villagertrade.cycle", PermissionDefault.TRUE);
    }

    private void registerIfAbsent(org.bukkit.plugin.PluginManager pm, String node, PermissionDefault def) {
        if (pm.getPermission(node) == null) {
            pm.addPermission(new Permission(node, def));
        }
    }
}
