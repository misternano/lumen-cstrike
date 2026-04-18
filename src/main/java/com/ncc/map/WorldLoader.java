package com.ncc.map;

import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;

public class WorldLoader {

    public static void paste(File file, InstanceContainer instance) {
        try {

            PolarWorld world = PolarLoader.load()

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch schematics: " + file.getName(), e);
        }
    }
}

