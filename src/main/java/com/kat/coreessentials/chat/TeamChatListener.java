package com.kat.coreessentials.chat;

import com.kat.coreessentials.data.TeamManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Every chat message gets a channel tag: "[All]" for normal public chat
 * (followed by "[TeamName]" too, in that team's color, if the sender is on
 * a team), or just "[Team]" (in the team's color) when the sender has
 * team-chat mode toggled on, which also restricts who actually sees the
 * message. Runs at HIGH priority so our renderer wins even if some other
 * plugin also touches chat.
 */
public class TeamChatListener implements Listener {

    private final TeamManager teams;

    public TeamChatListener(TeamManager teams) {
        this.teams = teams;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String teamKey = teams.getTeamOf(sender.getUniqueId());

        if (teamKey != null && teams.isInTeamChat(sender.getUniqueId())) {
            routeToTeamOnly(event, teamKey);
        } else {
            if (teamKey == null) {
                // No longer in a team but mode was left on somehow - fall back to normal chat.
                teams.clearTeamChat(sender.getUniqueId());
            }
            applyAllTag(event, teamKey);
        }
    }

    private void routeToTeamOnly(AsyncChatEvent event, String teamKey) {
        NamedTextColor color = teams.getColor(teamKey);

        event.viewers().clear();
        for (UUID memberId : teams.getMembers(teamKey)) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                event.viewers().add(member);
            }
        }
        event.viewers().add(Bukkit.getConsoleSender());

        event.renderer((source, sourceDisplayName, message, viewer) -> channelTag("Team", color)
            .append(sourceDisplayName)
            .append(Component.text(": ", NamedTextColor.WHITE))
            .append(message)
        );
    }

    private void applyAllTag(AsyncChatEvent event, String teamKey) {
        Component prefix = channelTag("All", NamedTextColor.GRAY);
        if (teamKey != null) {
            prefix = prefix.append(channelTag(teams.getDisplayName(teamKey), teams.getColor(teamKey)));
        }
        Component finalPrefix = prefix;

        event.renderer((source, sourceDisplayName, message, viewer) -> finalPrefix
            .append(sourceDisplayName)
            .append(Component.text(": ", NamedTextColor.WHITE))
            .append(message)
        );
    }

    /** Builds "[Label] " with only the label colored - brackets always plain white. */
    private static Component channelTag(String label, NamedTextColor color) {
        return Component.text("[", NamedTextColor.WHITE)
            .append(Component.text(label, color))
            .append(Component.text("] ", NamedTextColor.WHITE));
    }
}
