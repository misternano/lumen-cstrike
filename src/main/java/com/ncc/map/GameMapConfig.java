package com.ncc.map;

import net.minestom.server.coordinate.Pos;

import java.util.List;

public class GameMapConfig {

    public String name;

    public List<Spawn> lobbySpawns;
    public List<Spawn> ctSpawns;
    public List<Spawn> tSpawns;

    public BombSites bombSites;

    public static class Spawn {
        public int x;
        public int y;
        public int z;
        public float yaw;
        public float pitch;
    }

    public static class BombSites {
        public Site A;
        public Site B;
    }

    public static class Site {
        public Pos center;
        public int radius;
    }

    public static class MapPos {
        public int x;
        public int y;
        public int z;
    }
}
