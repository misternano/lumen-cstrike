package com.ncc.permissions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.BelowNameTag;

import java.util.Collection;

public final class MoneyBelowNameService {

    private final BelowNameTag belowNameTag = new BelowNameTag(
            "money",
            Component.text("$", NamedTextColor.GREEN)
    );

    public void addViewer(Player player) {
        belowNameTag.addViewer(player);
    }

    public void removeViewer(Player player) {
        belowNameTag.removeViewer(player);
    }

    public void updateMoney(Player player, int money) {
        belowNameTag.updateScore(player, Math.max(0, money));
    }

    public void syncAll(Collection<Player> players, MoneyLookup moneyLookup) {
        for (Player viewer : players) {
            belowNameTag.addViewer(viewer);
        }

        for (Player target : players) {
            updateMoney(target, moneyLookup.getMoney(target));
        }
    }

    @FunctionalInterface
    public interface MoneyLookup {
        int getMoney(Player player);
    }
}
