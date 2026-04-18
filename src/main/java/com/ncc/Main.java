package com.ncc;

import com.ncc.map.MapLoader;
import com.ncc.map.WorldImporter;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;

public class Main {

    private static InstanceContainer INSTANCE;

    public static void main(String[] args) {

        System.out.println("Starting CSTRIKE instance...");

        MinecraftServer server = MinecraftServer.init(
                new Auth.Velocity("ubBS6I42IRUm0pQyDYVRaR3JIOZUySZKBGhHiI/OKRc=")
        );

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();

            if (INSTANCE == null) return;

            event.setSpawningInstance(INSTANCE);
            player.setRespawnPoint(new Pos(-95, 75, -302));
            player.setGameMode(GameMode.CREATIVE);
        });

        MapLoader mapLoader = new MapLoader();
        File map = mapLoader.getRandomMap();

        System.out.println("Loading map: " + map.getName());

        WorldImporter.loadWorldAsync(map).thenAccept(instance -> {
            INSTANCE = instance;
            MinecraftServer.getInstanceManager().registerInstance(instance);
        });

        server.start("0.0.0.0", 25565);
    }
}