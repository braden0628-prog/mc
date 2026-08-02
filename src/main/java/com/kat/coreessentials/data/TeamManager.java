package com.kat.coreessentials.data;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TeamManager {

    public record Invite(String teamKey, UUID inviter, long expiresAtMillis) {
    }

    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, Invite> pendingInvites = new ConcurrentHashMap<>();
    private final Set<UUID> teamChatMode = ConcurrentHashMap.newKeySet();
    private static final long INVITE_EXPIRE_MILLIS = 60_000L;
    private static final int MAX_MEMBERS = 4;

    // Curated so the color is always readable in chat - skips near-black/white.
    private static final NamedTextColor[] TEAM_COLORS = {
        NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW,
        NamedTextColor.GREEN, NamedTextColor.DARK_GREEN, NamedTextColor.AQUA,
        NamedTextColor.DARK_AQUA, NamedTextColor.BLUE, NamedTextColor.LIGHT_PURPLE,
        NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_RED
    };

    public TeamManager(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "teams.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    // --- lookups -----------------------------------------------------

    /** Returns the lowercase team key the player belongs to, or null. */
    public String getTeamOf(UUID player) {
        if (!yaml.contains("teams")) {
            return null;
        }
        for (String key : yaml.getConfigurationSection("teams").getKeys(false)) {
            List<String> members = yaml.getStringList("teams." + key + ".members");
            if (members.contains(player.toString())) {
                return key;
            }
        }
        return null;
    }

    public boolean teamExists(String key) {
        return yaml.contains("teams." + key.toLowerCase());
    }

    public String getDisplayName(String key) {
        return yaml.getString("teams." + key.toLowerCase() + ".display-name", key);
    }

    public UUID getOwner(String key) {
        String raw = yaml.getString("teams." + key.toLowerCase() + ".owner");
        return raw == null ? null : UUID.fromString(raw);
    }

    public List<UUID> getMembers(String key) {
        List<UUID> result = new ArrayList<>();
        for (String raw : yaml.getStringList("teams." + key.toLowerCase() + ".members")) {
            result.add(UUID.fromString(raw));
        }
        return result;
    }

    /** Returns the team's assigned color (chosen randomly at creation), defaulting to white. */
    public NamedTextColor getColor(String key) {
        String name = yaml.getString("teams." + key.toLowerCase() + ".color");
        if (name == null) {
            return NamedTextColor.WHITE;
        }
        NamedTextColor color = NamedTextColor.NAMES.value(name);
        return color == null ? NamedTextColor.WHITE : color;
    }

    public int maxMembers() {
        return MAX_MEMBERS;
    }

    public boolean isFull(String key) {
        return getMembers(key).size() >= MAX_MEMBERS;
    }

    /** All existing team keys (lowercase), for tab-completion etc. */
    public Set<String> getAllTeamKeys() {
        if (!yaml.contains("teams")) {
            return java.util.Collections.emptySet();
        }
        return yaml.getConfigurationSection("teams").getKeys(false);
    }

    // --- mutations -----------------------------------------------------

    public boolean createTeam(UUID owner, String displayName) {
        String key = displayName.toLowerCase();
        if (teamExists(key) || getTeamOf(owner) != null) {
            return false;
        }
        yaml.set("teams." + key + ".display-name", displayName);
        yaml.set("teams." + key + ".owner", owner.toString());
        NamedTextColor color = TEAM_COLORS[ThreadLocalRandom.current().nextInt(TEAM_COLORS.length)];
        yaml.set("teams." + key + ".color", NamedTextColor.NAMES.keyOrThrow(color));
        List<String> members = new ArrayList<>();
        members.add(owner.toString());
        yaml.set("teams." + key + ".members", members);
        save();
        return true;
    }

    public void invite(String teamKey, UUID inviter, UUID target) {
        pendingInvites.put(target, new Invite(teamKey.toLowerCase(), inviter, System.currentTimeMillis() + INVITE_EXPIRE_MILLIS));
    }

    public Invite getValidInvite(UUID target) {
        Invite invite = pendingInvites.get(target);
        if (invite == null) {
            return null;
        }
        if (System.currentTimeMillis() > invite.expiresAtMillis()) {
            pendingInvites.remove(target);
            return null;
        }
        return invite;
    }

    public void clearInvite(UUID target) {
        pendingInvites.remove(target);
    }

    /** Adds the player to the team tied to their pending invite. Returns the team key, or null if no valid invite. */
    public String acceptInvite(UUID target) {
        Invite invite = getValidInvite(target);
        if (invite == null) {
            return null;
        }
        clearInvite(target);
        if (!teamExists(invite.teamKey())) {
            return null;
        }
        if (getTeamOf(target) != null) {
            return null;
        }
        List<String> members = yaml.getStringList("teams." + invite.teamKey() + ".members");
        members.add(target.toString());
        yaml.set("teams." + invite.teamKey() + ".members", members);
        save();
        return invite.teamKey();
    }

    /** Returns true if player left successfully; disbands the team if it becomes empty. */
    public boolean leaveTeam(UUID player) {
        String key = getTeamOf(player);
        if (key == null) {
            return false;
        }
        List<String> members = yaml.getStringList("teams." + key + ".members");
        members.remove(player.toString());
        clearTeamChat(player);

        UUID owner = getOwner(key);
        if (owner != null && owner.equals(player)) {
            if (members.isEmpty()) {
                yaml.set("teams." + key, null);
                save();
                return true;
            }
            // Promote the next member to owner.
            yaml.set("teams." + key + ".owner", members.get(0));
        }
        yaml.set("teams." + key + ".members", members);
        save();
        return true;
    }

    public boolean disbandTeam(UUID owner) {
        String key = getTeamOf(owner);
        if (key == null || !owner.equals(getOwner(key))) {
            return false;
        }
        getMembers(key).forEach(this::clearTeamChat);
        yaml.set("teams." + key, null);
        save();
        return true;
    }

    public boolean kick(UUID owner, UUID target) {
        String key = getTeamOf(owner);
        if (key == null || !owner.equals(getOwner(key))) {
            return false;
        }
        List<String> members = yaml.getStringList("teams." + key + ".members");
        if (!members.remove(target.toString())) {
            return false;
        }
        clearTeamChat(target);
        yaml.set("teams." + key + ".members", members);
        save();
        return true;
    }

    // --- team chat mode -----------------------------------------------------

    /** Flips the player's persistent team-chat mode. Returns the new state (true = now in team chat). */
    public boolean toggleTeamChat(UUID player) {
        if (teamChatMode.remove(player)) {
            return false;
        }
        teamChatMode.add(player);
        return true;
    }

    public boolean isInTeamChat(UUID player) {
        return teamChatMode.contains(player);
    }

    public void clearTeamChat(UUID player) {
        teamChatMode.remove(player);
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[CoreEssentials] Failed to save teams.yml: " + e.getMessage());
        }
    }
}
