package com.ncc.game.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class DefuseItem {

    public static ItemStack create() {
        return ItemStack.builder(Material.LIGHTNING_ROD)
                .customName(Component.text("Defuse Kit", NamedTextColor.AQUA))
                .build();
    }
}
