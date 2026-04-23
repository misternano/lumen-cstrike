package com.ncc.listener;

import com.ncc.Main;
import com.ncc.skin.Skins;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSkinInitEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

import java.util.Random;

public final class ConnectionListeners {

    private static final Random RANDOM = new Random();

    private ConnectionListeners() {
    }

    public static void register(GlobalEventHandler events) {
        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();

            if (Main.INSTANCE.getPlayers().size() >= Main.config.match.maxPlayers) {
                player.kick("Instance is full");
                return;
            }

            event.setSpawningInstance(Main.INSTANCE);
            player.setGameMode(GameMode.ADVENTURE);
            Main.operatorService.apply(player);
        });

        events.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            if (!event.isFirstSpawn()) return;

            var spawn = Main.cfg.lobbySpawns.get(RANDOM.nextInt(Main.cfg.lobbySpawns.size()));
            var pos = com.ncc.map.MapUtil.toPos(spawn);

            player.teleport(pos);
            player.setRespawnPoint(pos);
            Main.gameManager.handleJoin(player);
            Main.operatorService.applyAndRefreshCommands(player);
        });

        events.addListener(PlayerSkinInitEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerSkin skin = Main.gameManager.getAssignedSkin(player);

            if (skin == null) {
                event.setSkin(Skins.FALLBACK);
                return;
            }

            event.setSkin(skin);
        });

        events.addListener(PlayerChatEvent.class, event -> {
            Player player = event.getPlayer();
            event.setFormattedMessage(Main.chatFormatService.buildChatMessage(
                    player,
                    Main.gameManager.getTeam(player),
                    event.getRawMessage()
            ));
        });

        events.addListener(PlayerDisconnectEvent.class, event ->
                Main.gameManager.handleLeave(event.getPlayer()));
    }
}
