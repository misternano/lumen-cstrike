package com.ncc.permissions;

import com.ncc.game.TeamSide;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NametagService {

    private final ChatFormatService chatFormatService;
    private final Map<UUID, Team> teams = new ConcurrentHashMap<>();

    public NametagService(ChatFormatService chatFormatService) {
        this.chatFormatService = chatFormatService;
    }

    public void apply(Player player, TeamSide side, int money) {
        Team team = teams.computeIfAbsent(player.getUuid(), uuid -> createTeam(player));
        team.updatePrefix(chatFormatService.buildNametagPrefix(player));
        team.updateSuffix(Component.empty());
        team.updateTeamColor(NamedTextColor.YELLOW);
        team.updateNameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS);
        player.setTeam(team);
    }

    public void remove(Player player) {
        Team team = teams.remove(player.getUuid());
        if (team == null) {
            return;
        }

        MinecraftServer.getTeamManager().deleteTeam(team);
    }

    private Team createTeam(Player player) {
        String teamName = "p" + player.getUuid().toString().replace("-", "").substring(0, 15);
        Team team = MinecraftServer.getTeamManager().createTeam(teamName);
        team.setNameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS);
        team.setPrefix(chatFormatService.buildNametagPrefix(player));
        team.setSuffix(Component.empty());
        team.setTeamColor(NamedTextColor.YELLOW);
        return team;
    }
}
