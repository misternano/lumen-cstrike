package com.ncc;

import com.ncc.commands.*;
import com.ncc.game.GameManager;
import com.ncc.game.items.ItemRegistry;
import com.ncc.listener.BombListeners;
import com.ncc.listener.BuyMenuListeners;
import com.ncc.listener.ConnectionListeners;
import com.ncc.listener.GunListeners;
import com.ncc.map.*;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;

import java.io.File;

public class Main {

    public static InstanceContainer INSTANCE;
    public static GameMapConfig cfg;
    public static GameManager gameManager;

    public static Team ctTeam;
    public static Team tTeam;

    public static final int MAX_PLAYERS = 10;

    public static void main(String[] args) {

        // --- Server Initialization
        System.out.println("Starting CSTRIKE instance...");

        MinecraftServer server = MinecraftServer.init(
//                new Auth.Velocity("ubBS6I42IRUm0pQyDYVRaR3JIOZUySZKBGhHiI/OKRc=")
        );

        ItemRegistry.init();

        // --- Map Loading
        MapLoader mapLoader = new MapLoader();
        File map = mapLoader.getRandomMap();

        String mapName = map.getName().replace(".polar", "");

        System.out.println("Loading map: " + map.getName());

        INSTANCE = MinecraftServer.getInstanceManager().createInstanceContainer();

        try {
            INSTANCE.setChunkLoader(new PolarLoader(map.toPath()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load map: " + map.getName(), e);
        }

        File json = new File(map.getParent(), map.getName().replace(".polar", ".json"));
        cfg = MapConfigLoader.load(json);

        if (cfg == null) {
            throw new RuntimeException("Map config failed to load: " + json.getAbsolutePath());
        }

        MinecraftServer.getInstanceManager().registerInstance(INSTANCE);

        // --- Team Creation & Styling
        ctTeam = MinecraftServer.getTeamManager().createTeam("CT");
        tTeam = MinecraftServer.getTeamManager().createTeam("T");

        ctTeam.setNameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
        tTeam.setNameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS);

        ctTeam.setPrefix(Component.text("[CT] ", NamedTextColor.BLUE));
        tTeam.setPrefix(Component.text("[T] ", NamedTextColor.RED));

        // --- Game Manager
        gameManager = new GameManager(cfg, INSTANCE);
        gameManager.switchMap(INSTANCE, cfg, mapName);

        // --- Command Registrar
        MinecraftServer.getCommandManager().register(new GamemodeCommand());
        MinecraftServer.getCommandManager().register(new MoneyCommand());
        MinecraftServer.getCommandManager().register(new MapCommand());
        MinecraftServer.getCommandManager().register(new MapsCommand());
        MinecraftServer.getCommandManager().register(new MatchCommand());
        MinecraftServer.getCommandManager().register(new GiveItemCommand());
        MinecraftServer.getCommandManager().register(new GiveWeaponCommand());
        MinecraftServer.getCommandManager().register(new TeamCommand());
        MinecraftServer.getCommandManager().register(new TeleportCommand());

        // --- Event Handling
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        ConnectionListeners.register(events);
        BombListeners.register(events);
        GunListeners.register(events);
        BuyMenuListeners.register(events);

        // --- Initialize instance
        server.start("0.0.0.0", 25566);
    }
}
