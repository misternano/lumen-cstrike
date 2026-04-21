package com.ncc.game.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public final class KnifeItem {

    private KnifeItem() {
    }

    public static ItemStack create() {
        return ItemStack.builder(Material.IRON_SWORD)
                .customName(Component.text("Knife", NamedTextColor.WHITE))
                .build();
    }
}
