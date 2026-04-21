package com.ncc.listener;

import com.ncc.Main;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryPreClickEvent;

public final class BuyMenuListeners {

    private BuyMenuListeners() {
    }

    public static void register(GlobalEventHandler events) {
        events.addListener(InventoryPreClickEvent.class, event -> {
            if (!Main.gameManager.isBuyMenu(event.getInventory())) return;

            event.setCancelled(true);
            Main.gameManager.handleBuyMenuClick(event.getPlayer(), event.getSlot());
        });
    }
}
