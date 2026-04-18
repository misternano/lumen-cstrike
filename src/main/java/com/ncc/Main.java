package com.ncc;

import com.ncc.map.MapLoader;
import com.ncc.map.WorldLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;

public class Main {

    private static volatile InstanceContainer INSTANCE;
    private static volatile boolean READY = false;

    public static void main(String[] args) {

        System.out.println("Starting CSTRIKE instance...");

        MinecraftServer server = MinecraftServer.init(
//                new Auth.Velocity("ubBS6I42IRUm0pQyDYVRaR3JIOZUySZKBGhHiI/OKRc=")
        );

        MapLoader mapLoader = new MapLoader();
        File map = mapLoader.getRandomMap();

        System.out.println("Loading map: " + map.getName());

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        MinecraftServer.getInstanceManager().registerInstance(instance);

        WorldLoader.paste(map, instance);

        INSTANCE = instance;
        READY = true;

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();

            if (!READY || INSTANCE == null) {
                player.kick("Instance loading...");
                return;
            }

            event.setSpawningInstance(INSTANCE);
            player.setRespawnPoint(new Pos(0, 80, 0));
            player.setGameMode(GameMode.CREATIVE);
        });

        server.start("0.0.0.0", 25565);
    }
}