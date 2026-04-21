package com.ncc.game.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class BombItem {

    public static ItemStack create() {
        return ItemStack.builder(Material.COPPER_NAUTILUS_ARMOR)
                .customName(Component.text("Bomb", NamedTextColor.RED))
                .build();
    }
}
