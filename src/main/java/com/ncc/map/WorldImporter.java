package com.ncc.map;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class WorldImporter {

    public static CompletableFuture<InstanceContainer> loadWorldAsync(File worldFolder) {

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();

        instance.setChunkLoader(new AnvilLoader(worldFolder.toPath()));

        return CompletableFuture.runAsync(() -> {
            int radius = 25;

            for (int x = -radius; x<= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    instance.loadChunk(x, z).join();
                }
            }
        }).thenApply(v -> instance);
    }
}
