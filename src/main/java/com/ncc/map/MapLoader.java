package com.ncc.map;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MapLoader {

    private final File mapsDir = new File("schems");
    private final Random random = new Random();

    public File getRandomMap() {
        File[] files = mapsDir.listFiles((d, name) -> name.endsWith(".schem"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No schematics found");
        }

        return files[random.nextInt(files.length)];
    }

    public List<File> getAll() {
        File[] files = mapsDir.listFiles((d, name) -> name.endsWith(".schem"));
        return files == null ? List.of() : Arrays.asList(files);
    }
}
