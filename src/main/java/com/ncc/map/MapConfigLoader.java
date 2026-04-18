package com.ncc.map;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;

public class MapConfigLoader {

    private static final Gson GSON = new Gson();

    public static GameMapConfig load(File file) {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, GameMapConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load map config: " + file.getName(), e);
        }
    }
}
