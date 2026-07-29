package com.kat.coreessentials;

import com.kat.coreessentials.chat.TeamChatListener;
import com.kat.coreessentials.combat.CombatListener;
import com.kat.coreessentials.combat.CombatManager;
import com.kat.coreessentials.combat.TeleportDelayManager;
import com.kat.coreessentials.combat.TeleportExecutor;
import com.kat.coreessentials.commands.HomeCommand;
import com.kat.coreessentials.commands.RtpCommand;
import com.kat.coreessentials.commands.SetSpawnCommand;
import com.kat.coreessentials.commands.SpawnCommand;
import com.kat.coreessentials.commands.TeamCommand;
import com.kat.coreessentials.commands.TprCommand;
import com.kat.coreessentials.data.HomeManager;
import com.kat.coreessentials.data.SpawnManager;
import com.kat.coreessentials.data.TeamManager;
import com.kat.coreessentials.data.TeleportRequestManager;
import com.kat.coreessentials.enchant.LibrarianTradeListener;
import com.kat.coreessentials.enchant.VeinMinerListener;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.homeManager = new HomeManager(this);
        this.teamManager = new TeamManager(this);
        this.spawnManager = new SpawnManager(this);
        this.teleportRequestManager = new TeleportRequestManager(getConfig().getLong("tpr.expire-seconds", 60));
        this.combatManager = new CombatManager(getConfig().getLong("combat.tag-seconds", 30));
        this.teleportDelayManager = new TeleportDelayManager();
        TeleportExecutor.delaySeconds = getConfig().getInt("teleport.delay-seconds", 5);

        registerPermissions();

        Bukkit.getPluginManager().registerEvents(new VeinMinerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LibrarianTradeListener(), this);
        Bukkit.getPluginManager().registerEvents(new TeamChatListener(teamManager), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(combatManager, teleportDelayManager), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(TprCommand.build(teleportRequestManager), "Send a teleport request to another player.");
            commands.registrar().register(TprCommand.buildAccept(this, teleportRequestManager, combatManager, teleportDelayManager));
            commands.registrar().register(TprCommand.buildDeny(teleportRequestManager));
            commands.registrar().register(HomeCommand.build(this, homeManager, combatManager, teleportDelayManager), "Manage and teleport to your homes.");
            commands.registrar().register(TeamCommand.build(teamManager), "Manage your team.");
            commands.registrar().register(RtpCommand.build(this, combatManager, teleportDelayManager), "Teleport to a random safe location.");
            commands.registrar().register(SpawnCommand.build(this, spawnManager, combatManager, teleportDelayManager), "Teleport to spawn.");
            commands.registrar().register(SetSpawnCommand.build(spawnManager), "Set the server spawn location (op only).");
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
    }

    private void registerIfAbsent(org.bukkit.plugin.PluginManager pm, String node, PermissionDefault def) {
        if (pm.getPermission(node) == null) {
            pm.addPermission(new Permission(node, def));
        }
    }
}
