package com.ncc.game.items;

import com.ncc.game.gun.GunDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;

import java.util.List;

public final class GunItem {

    private GunItem() {
    }

    public static ItemStack create(GunDefinition definition, int ammo, int reserve) {
        int clampedAmmo = Math.max(0, Math.min(definition.magazineSize(), ammo));
        int durabilityDamage = definition.magazineSize() - clampedAmmo;

        return ItemStack.builder(definition.material())
                .customName(Component.text(definition.displayName(), NamedTextColor.WHITE)
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("Default", NamedTextColor.GRAY)))
                .lore(List.of(
                        Component.text("§7Ammo: §f" + clampedAmmo + "§8/§f" + reserve),
                        Component.text("Swap hands to reload", NamedTextColor.DARK_GRAY)
                ))
                .set(DataComponents.MAX_DAMAGE, definition.magazineSize())
                .set(DataComponents.DAMAGE, durabilityDamage)
                .set(DataComponents.MAX_STACK_SIZE, 1)
                .build();
    }
}
