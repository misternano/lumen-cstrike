package com.ncc.listener;

import com.ncc.Main;
import com.ncc.game.items.ItemRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;

public final class GunListeners {

    private GunListeners() {
    }

    public static void register(GlobalEventHandler events) {
        events.addListener(PlayerHandAnimationEvent.class, event -> {
            if (event.getHand() != PlayerHand.MAIN) return;
            Main.gameManager.getGunManager().tryShoot(event.getPlayer());
        });

        events.addListener(PlayerUseItemEvent.class, event -> {
            if (event.getHand() != PlayerHand.MAIN) return;

            if (ItemRegistry.isBuyMenu(event.getItemStack())) {
                event.setCancelled(true);
                Main.gameManager.openBuyMenu(event.getPlayer());
                return;
            }

            if (!ItemRegistry.isGun(event.getItemStack())) return;

            event.setCancelled(true);
            Main.gameManager.getGunManager().tryShoot(event.getPlayer());
        });

        events.addListener(PlayerSwapItemEvent.class, event -> {
            if (!ItemRegistry.isGun(event.getMainHandItem()) && !ItemRegistry.isGun(event.getOffHandItem())) return;

            event.setCancelled(true);
            if (ItemRegistry.isGun(event.getOffHandItem()) && !ItemRegistry.isGun(event.getMainHandItem())) {
                event.setMainHandItem(event.getOffHandItem());
            } else {
                event.setMainHandItem(event.getMainHandItem());
            }
            event.setOffHandItem(ItemStack.AIR);

            if (ItemRegistry.isGun(event.getMainHandItem())) {
                Main.gameManager.getGunManager().tryReload(event.getPlayer());
            }

            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
                event.getPlayer().getInventory().setItemStack(PlayerInventoryUtils.OFFHAND_SLOT, ItemStack.AIR);
                event.getPlayer().getInventory().update(event.getPlayer());
            });
        });
    }
}
