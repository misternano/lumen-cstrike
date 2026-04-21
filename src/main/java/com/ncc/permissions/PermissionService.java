package com.ncc.permissions;

import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionService {

    private final Map<UUID, Rank> ranks = new ConcurrentHashMap<>();

    public Rank getRank(Player player) {
        return ranks.getOrDefault(player.getUuid(), Rank.PLAYER);
    }

    public void setRank(Player player, Rank rank) {
        ranks.put(player.getUuid(), rank);
    }

    public boolean hasPermission(CommandSender sender, String node) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        return getRank(player).hasPermission(node);
    }
}
