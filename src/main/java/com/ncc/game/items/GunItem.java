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
                        Component.text("Ammo: ", NamedTextColor.GRAY)
                                .append(Component.text(clampedAmmo + "/" + reserve, NamedTextColor.WHITE)),
                        Component.text("[F] Reload", NamedTextColor.DARK_GRAY),
                        Component.text("[R/LClick] Shoot", NamedTextColor.DARK_GRAY)
                ))
                .set(DataComponents.ITEM_MODEL, definition.itemModel())
                .set(DataComponents.CHARGED_PROJECTILES, List.of(ItemStack.of(net.minestom.server.item.Material.ARROW)))
                .set(DataComponents.MAX_DAMAGE, definition.magazineSize())
                .set(DataComponents.DAMAGE, durabilityDamage)
                .set(DataComponents.MAX_STACK_SIZE, 1)
                .build();
    }
}
