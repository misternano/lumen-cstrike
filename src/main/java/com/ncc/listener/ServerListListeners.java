package com.ncc.listener;

import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.server.ServerListPingEvent;
import net.minestom.server.ping.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerListListeners {

    private static final Path SERVER_ICON_PATH = Path.of("config", "server-icon.png");
    private static final byte[] SERVER_ICON = loadServerIcon();

    private ServerListListeners() {
    }

    public static void register(GlobalEventHandler events) {
        if (SERVER_ICON == null) {
            return;
        }

        events.addListener(ServerListPingEvent.class, event -> {
            Status status = Status.builder(event.getStatus())
                    .favicon(SERVER_ICON)
                    .build();

            event.setStatus(status);
        });
    }

    private static byte[] loadServerIcon() {
        if (!Files.exists(SERVER_ICON_PATH)) {
            System.out.println("No server icon found at " + SERVER_ICON_PATH + "; skipping favicon setup.");
            return null;
        }

        try {
            return Files.readAllBytes(SERVER_ICON_PATH);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load server icon from " + SERVER_ICON_PATH, exception);
        }
    }
}
