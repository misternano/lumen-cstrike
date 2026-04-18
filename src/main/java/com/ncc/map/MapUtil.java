package com.ncc.map;

import net.minestom.server.coordinate.Pos;

public class MapUtil {
    public static Pos toPos(GameMapConfig.Spawn s) {
        return new Pos(
                s.x,
                s.y,
                s.z,
                s.yaw,
                s.pitch
        );
    }
}
