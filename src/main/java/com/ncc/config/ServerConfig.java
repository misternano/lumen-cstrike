package com.ncc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Maps maps = new Maps();
    public Match match = new Match();
    public Bomb bomb = new Bomb();
    public FallDamage fallDamage = new FallDamage();

    public static ServerConfig load(Path path) {
        try {
            if (Files.notExists(path)) {
                ServerConfig defaults = new ServerConfig();
                defaults.save(path);
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(path)) {
                ServerConfig config = GSON.fromJson(reader, ServerConfig.class);
                return config == null ? new ServerConfig() : config.withDefaults();
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to load server config from " + path, exception);
        }
    }

    private void save(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(this, writer);
        }
    }

    private ServerConfig withDefaults() {
        if (maps == null) maps = new Maps();
        if (match == null) match = new Match();
        if (bomb == null) bomb = new Bomb();
        if (fallDamage == null) fallDamage = new FallDamage();
        return this;
    }

    public static final class Maps {
        public String directory = "maps";
        public String selection = "random";
        public String startupMap = "";

        public Path directoryPath() {
            String value = directory == null || directory.isBlank() ? "maps" : directory;
            return Path.of(value);
        }
    }

    public static final class Match {
        public int minimumPlayers = 1;
        public int maxPlayers = 10;
        public int startingMoney = 800;
        public int countdownSeconds = 10;
        public int goCountdownSeconds = 3;
        public int postRoundSeconds = 15;
    }

    public static final class Bomb {
        public int plantTicks = 60;
        public int defuseTicks = 10 * 20;
        public int defuseWithKitTicks = 5 * 20;
        public int fuseTicks = 40 * 20;
        public double explosionDamageRadius = 7.5;
        public float explosionMaxDamage = 24f;
    }

    public static final class FallDamage {
        public boolean enabled = true;
        public double sourceUnitsPerBlock = 40.0;
        public double sourceGravity = 800.0;
        public double safeFallSpeed = 580.0;
        public double fatalFallSpeed = 1024.0;
        public float minecraftHealthDivisor = 5.0f;
    }
}
