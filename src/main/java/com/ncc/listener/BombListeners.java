package com.ncc.listener;

import com.ncc.Main;
import com.ncc.game.GameManager;
import com.ncc.game.TeamSide;
import com.ncc.game.bomb.BombManager;
import com.ncc.game.gun.GunManager;
import com.ncc.game.items.ItemRegistry;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;

public final class BombListeners {

    private BombListeners() {
    }

    public static void register(GlobalEventHandler events) {
        events.addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            GameManager gameManager = Main.gameManager;
            BombManager bombManager = gameManager.getBombManager();

            if (gameManager == null) return;

            var from = player.getPosition();
            var to = event.getNewPosition();

            if (gameManager.isFrozen()) {
                event.setNewPosition(from.withYaw(to.yaw()).withPitch(to.pitch()).withY(to.y()));
            }

            if (bombManager.isPlanting(player) && event.getNewPosition().distance(player.getPosition()) > 0.1) {
                bombManager.setPlanting(player, false, null);
            }
        });

        events.addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            BombManager bombManager = Main.gameManager.getBombManager();
            GunManager gunManager = Main.gameManager.getGunManager();

            if (ItemRegistry.isGun(player.getItemInOffHand())) {
                player.getInventory().setItemStack(PlayerInventoryUtils.OFFHAND_SLOT, ItemStack.AIR);
                player.getInventory().update(player);
            }

            if (Main.gameManager.getTeam(player) == TeamSide.T) {
                boolean holdingBomb = ItemRegistry.isBomb(player.getItemInMainHand());
                String site = Main.gameManager.getBombSiteAt(player);

                if (holdingBomb && site != null) {
                    bombManager.setPlanting(player, true, site);
                } else {
                    bombManager.setPlanting(player, false, null);
                }
            }

            if (Main.gameManager.getTeam(player) == TeamSide.CT) {
                bombManager.setDefusing(player, player.isSneaking());
            }

            if (!bombManager.isPlanting(player) && !bombManager.isDefusing(player)) {
                gunManager.tickPlayer(player);
            }
        });
    }
}
