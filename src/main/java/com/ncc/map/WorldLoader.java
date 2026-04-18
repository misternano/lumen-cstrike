package com.ncc.map;

import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;

public class WorldLoader {

    public static InstanceContainer load(File file) {
        try {

            InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();

            instance.setChunkLoader(new PolarLoader(file.toPath()));

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load polar world: " + file.getName(), e);
        }
    }
}