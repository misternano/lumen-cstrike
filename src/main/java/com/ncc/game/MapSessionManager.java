package com.ncc.game;

import com.ncc.map.GameMapConfig;
import com.ncc.map.MapUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;

import java.util.List;
import java.util.Random;

public final class MapSessionManager {

    private final Random random = new Random();

    private GameMapConfig config;
    private InstanceContainer currentInstance;
    private String currentMapName;

    public MapSessionManager(GameMapConfig config, InstanceContainer currentInstance) {
        this.config = config;
        this.currentInstance = currentInstance;
    }

    public void update(InstanceContainer instance, GameMapConfig config, String mapName) {
        this.config = config;
        this.currentInstance = instance;
        this.currentMapName = mapName;
    }

    public GameMapConfig config() {
        return config;
    }

    public InstanceContainer currentInstance() {
        return currentInstance;
    }

    public String currentMapName() {
        return currentMapName;
    }

    public void respawnInLobby(Player player) {
        var spawn = config.lobbySpawns.get(random.nextInt(config.lobbySpawns.size()));
        Pos pos = MapUtil.toPos(spawn);

        player.teleport(pos);
        player.setRespawnPoint(pos);
    }

    public void moveToLobbyInstance(Player player, InstanceContainer instance) {
        var spawn = config.lobbySpawns.get(random.nextInt(config.lobbySpawns.size()));
        Pos pos = MapUtil.toPos(spawn);

        player.setInstance(instance, pos);
        player.setRespawnPoint(pos);
    }

    public void teleportPlayersToTeamSpawns(List<Player> players, PlayerStateManager playerStateManager) {
        int ctIndex = 0;
        int tIndex = 0;

        for (Player player : players) {
            TeamSide side = playerStateManager.getTeam(player);
            if (side == null) {
                continue;
            }

            if (side == TeamSide.CT) {
                var spawn = config.ctSpawns.get(ctIndex++ % config.ctSpawns.size());
                player.teleport(MapUtil.toPos(spawn));
            } else {
                var spawn = config.tSpawns.get(tIndex++ % config.tSpawns.size());
                player.teleport(MapUtil.toPos(spawn));
            }
        }
    }

    public String getBombSiteAt(Player player) {
        Pos pos = player.getPosition();

        if (inSite(pos, config.bombSites.A)) return "A";
        if (inSite(pos, config.bombSites.B)) return "B";

        return null;
    }

    public boolean inSite(Pos pos, GameMapConfig.Site site) {
        double dx = pos.x() - site.center.x();
        double dz = pos.z() - site.center.z();

        return dx * dx + dz * dz <= site.radius * site.radius;
    }
}
