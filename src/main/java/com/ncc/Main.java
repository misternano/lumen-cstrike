package com.ncc;

import com.ncc.commands.MapCommand;
import com.ncc.game.GameManager;
import com.ncc.map.GameMapConfig;
import com.ncc.map.MapConfigLoader;
import com.ncc.map.MapLoader;
import com.ncc.map.MapUtil;
import com.ncc.skin.Skins;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;

import java.io.File;
import java.util.Random;

public class Main {

    public static InstanceContainer INSTANCE;
    public static GameMapConfig CFG;
    public static GameManager gameManager;
    private static Team HIDDEN_TEAM;

    public static final int MAX_PLAYERS = 10;
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {

        System.out.println("Starting CSTRIKE instance...");

        MinecraftServer server = MinecraftServer.init(
                 new Auth.Velocity("ubBS6I42IRUm0pQyDYVRaR3JIOZUySZKBGhHiI/OKRc=")
        );

        // map loading
        MapLoader mapLoader = new MapLoader();
        File map = mapLoader.getRandomMap();

        System.out.println("Loading map: " + map.getName());

        // initialize instance
        INSTANCE = MinecraftServer.getInstanceManager().createInstanceContainer();

        try {
            INSTANCE.setChunkLoader(new PolarLoader(map.toPath()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load map: " + map.getName(), e);
        }

        // load configs
        File json = new File(map.getParent(), map.getName().replace(".polar", ".json"));
        CFG = MapConfigLoader.load(json);

        if (CFG == null) {
            throw new RuntimeException("Map config failed to load: " + json.getAbsolutePath());
        }

        // register instance
        MinecraftServer.getInstanceManager().registerInstance(INSTANCE);

        HIDDEN_TEAM = MinecraftServer.getTeamManager().createTeam("hidden");
        HIDDEN_TEAM.setNameTagVisibility(TeamsPacket.NameTagVisibility.NEVER);

        GameManager gameManager = new GameManager(CFG);
        Main.gameManager = gameManager;

        MinecraftServer.getCommandManager().register(new MapCommand());

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {

            Player player = event.getPlayer();

            if (INSTANCE.getPlayers().size() >= MAX_PLAYERS) {
                player.kick("Instance is full");
                return;
            }

            event.setSpawningInstance(INSTANCE);

            player.setGameMode(GameMode.CREATIVE);
            player.setSkin(Skins.T);
            player.setTeam(HIDDEN_TEAM);
        });


        events.addListener(PlayerSpawnEvent.class, event -> {

            Player player = event.getPlayer();

            if (!event.isFirstSpawn()) return;

            var spawn = CFG.lobbySpawns.get(RANDOM.nextInt(CFG.lobbySpawns.size()));
            var pos = MapUtil.toPos(spawn);

            player.teleport(pos);
            player.setRespawnPoint(pos);

            gameManager.handleJoin(player);
        });

        server.start("0.0.0.0", 25565);
    }
}