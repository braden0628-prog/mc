package com.kat.coreessentials.chat;

import com.kat.coreessentials.data.TeamManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class TeamChatListener implements Listener {

    private final TeamManager teams;

    public TeamChatListener(TeamManager teams) {
        this.teams = teams;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String teamKey = teams.getTeamOf(sender.getUniqueId());

        if (teams.isInTeamChat(sender.getUniqueId())) {
            if (teamKey == null) {
                // No longer in a team but mode was left on somehow - fall back to normal chat.
                teams.clearTeamChat(sender.getUniqueId());
            } else {
                routeToTeamOnly(event, teamKey, sender);
                return;
            }
        }

        applyTeamTagRenderer(event, teamKey);
    }

    private void routeToTeamOnly(AsyncChatEvent event, String teamKey, Player sender) {
        NamedTextColor color = teams.getColor(teamKey);
        String display = teams.getDisplayName(teamKey);

        event.viewers().clear();
        for (UUID memberId : teams.getMembers(teamKey)) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                event.viewers().add(member);
            }
        }
        event.viewers().add(Bukkit.getConsoleSender());

        event.renderer((source, sourceDisplayName, message, viewer) ->
            Component.text("[" + display + "] ", color)
                .append(sourceDisplayName)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(message)
        );
    }

    private void applyTeamTagRenderer(AsyncChatEvent event, String teamKey) {
        if (teamKey == null) {
            return;
        }
        NamedTextColor color = teams.getColor(teamKey);
        String display = teams.getDisplayName(teamKey);

        event.renderer((source, sourceDisplayName, message, viewer) ->
            Component.text("[" + display + "] ", color)
                .append(sourceDisplayName)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(message)
        );
    }
}
