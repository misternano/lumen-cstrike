package com.ncc.permissions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minestom.server.entity.Player;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OperatorService {

    public static final int OP_PERMISSION_LEVEL = 4;
    public static final int DEFAULT_PERMISSION_LEVEL = 0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path opsPath;
    private final Map<UUID, String> operators = new ConcurrentHashMap<>();

    public OperatorService(Path configDirectory) {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to initialize operator config directory", exception);
        }

        this.opsPath = configDirectory.resolve("ops.json");
        load();
    }

    public boolean isOperator(Player player) {
        return operators.containsKey(player.getUuid());
    }

    public void apply(Player player) {
        player.setPermissionLevel(isOperator(player) ? OP_PERMISSION_LEVEL : DEFAULT_PERMISSION_LEVEL);
    }

    public void applyAndRefreshCommands(Player player) {
        apply(player);
        player.refreshCommands();
    }

    public synchronized void op(Player player) {
        operators.put(player.getUuid(), player.getUsername());
        save();
        applyAndRefreshCommands(player);
    }

    public synchronized void deop(Player player) {
        operators.remove(player.getUuid());
        save();
        applyAndRefreshCommands(player);
    }

    private void load() {
        if (Files.notExists(opsPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(opsPath)) {
            OpsFile file = GSON.fromJson(reader, OpsFile.class);
            if (file == null || file.operators == null) {
                return;
            }

            for (Map.Entry<String, String> entry : file.operators.entrySet()) {
                try {
                    operators.put(UUID.fromString(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load operators from " + opsPath, exception);
        }
    }

    private void save() {
        OpsFile file = new OpsFile();
        file.operators = new LinkedHashMap<>();
        operators.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> file.operators.put(entry.getKey().toString(), entry.getValue()));

        try (Writer writer = Files.newBufferedWriter(opsPath)) {
            GSON.toJson(file, writer);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to save operators to " + opsPath, exception);
        }
    }

    private static final class OpsFile {
        private Map<String, String> operators = new LinkedHashMap<>();
    }
}
