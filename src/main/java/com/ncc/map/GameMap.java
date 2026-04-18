package com.ncc.map;

import net.minestom.server.instance.InstanceContainer;

public class GameMap {
    private final String name;
    private final MapGenerator generator;

    public interface MapGenerator {
        void generate(InstanceContainer instance);
    }

    public GameMap(String name, MapGenerator generator) {
        this.name = name;
        this.generator = generator;
    }

    public String getName() {
        return name;
    }

    public void apply(InstanceContainer instance) {
        generator.generate(instance);
    }
}
