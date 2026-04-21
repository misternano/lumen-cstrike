package com.ncc.game;

import com.ncc.game.gun.GunManager;
import com.ncc.game.items.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LoadoutManager {

    private static final int BOMB_SLOT = 7;

    private final Random random = new Random();

    public void prepareLobbyInventory(Player player, BuyMenuManager buyMenuManager) {
        player.getInventory().clear();
        buyMenuManager.giveBuyMenuItem(player);
    }

    public void giveRoundLoadout(List<Player> players, PlayerStateManager playerStateManager, BuyMenuManager buyMenuManager, GunManager gunManager) {
        List<Player> ts = new ArrayList<>();

        for (Player player : players) {
            TeamSide side = playerStateManager.getTeam(player);

            player.getInventory().clear();
            buyMenuManager.giveBuyMenuItem(player);
            gunManager.giveDefaultLoadout(player, side);

            if (side == TeamSide.T) {
                ts.add(player);
            }
        }

        if (ts.isEmpty()) {
            return;
        }

        Player bombCarrier = ts.get(random.nextInt(ts.size()));
        bombCarrier.getInventory().setItemStack(BOMB_SLOT, ItemRegistry.BOMB);
        bombCarrier.sendMessage(Component.text("You have the ", NamedTextColor.GRAY)
                .append(Component.text("bomb", NamedTextColor.DARK_RED)));
    }
}
