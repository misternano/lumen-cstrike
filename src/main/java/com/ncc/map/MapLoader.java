package com.ncc.map;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MapLoader {

    private final File mapsDir = new File("/app/maps");
    private final Random random = new Random();

    public File getRandomMap() {
        File[] maps = mapsDir.listFiles(File::isDirectory);

        if (maps == null || maps.length == 0) {
            throw new RuntimeException("No maps found in /maps");
        }

        return maps[random.nextInt(maps.length)];
    }

    public List<File> getAllMaps() {
        File[] maps = mapsDir.listFiles(File::isDirectory);
        return maps == null ? List.of() : Arrays.asList(maps);
    }
}
