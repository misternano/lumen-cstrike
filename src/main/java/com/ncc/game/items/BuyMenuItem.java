package com.ncc.game.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public final class BuyMenuItem {

    private BuyMenuItem() {
    }

    public static ItemStack create() {
        return ItemStack.builder(Material.NETHER_STAR)
                .customName(Component.text("Buy Menu", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(
                        Component.text("Right click to buy gear", NamedTextColor.WHITE),
                        Component.text("Available in lobby and freeze time", NamedTextColor.GRAY, TextDecoration.ITALIC)
                )
                .build();
    }
}
