package com.ncc.map;

import java.io.File;
import java.util.List;
import java.util.Random;

public class MapLoader {

    private final File mapsDir = new File("maps");
    private final Random random = new Random();

    // --- Load maps
    public MapLoader() {
        if (!mapsDir.exists() || !mapsDir.isDirectory()) {
            throw new IllegalStateException("Maps directory not found: " + mapsDir.getAbsolutePath());
        }
    }

    // --- Select random map for instance
    public File getRandomMap() {
        File[] files = listMaps();

        if (files == null || files.length == 0) {
            throw new IllegalStateException("No .polar maps found in " + mapsDir.getAbsolutePath());
        }

        return files[random.nextInt(files.length)];
    }

    public List<File> getAll() {
        File[] files = listMaps();
        return files == null ? List.of() : List.of(files);
    }

    private File[] listMaps() {
        return mapsDir.listFiles((d, name) -> name.endsWith(".polar"));
    }
}