package com.ncc.game.items;

import com.ncc.game.gun.GunDefinition;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class ItemRegistry {

    public static ItemStack BOMB;
    public static ItemStack DEFUSE;
    public static ItemStack GLOCK;
    public static ItemStack USP_S;
    public static ItemStack AK47;
    public static ItemStack M4A1;
    public static ItemStack KNIFE;
    public static ItemStack BUY_MENU;

    public static void init() {
        BOMB = BombItem.create();
        DEFUSE = DefuseItem.create();
        GLOCK = GunItem.create(GunDefinition.GLOCK, GunDefinition.GLOCK.magazineSize(), GunDefinition.GLOCK.reserveAmmo());
        USP_S = GunItem.create(GunDefinition.USP_S, GunDefinition.USP_S.magazineSize(), GunDefinition.USP_S.reserveAmmo());
        AK47 = GunItem.create(GunDefinition.AK47, GunDefinition.AK47.magazineSize(), GunDefinition.AK47.reserveAmmo());
        M4A1 = GunItem.create(GunDefinition.M4A1, GunDefinition.M4A1.magazineSize(), GunDefinition.M4A1.reserveAmmo());
        KNIFE = KnifeItem.create();
        BUY_MENU = BuyMenuItem.create();
    }

    public static boolean isBomb(ItemStack itemStack) {
        return itemStack != null && itemStack.material() == Material.COPPER_NAUTILUS_ARMOR;
    }

    public static boolean isDefuseKit(ItemStack itemStack) {
        return itemStack != null && itemStack.material() == Material.LIGHTNING_ROD;
    }

    public static boolean isGun(ItemStack itemStack) {
        return getGunDefinition(itemStack) != null;
    }

    public static ItemType getItemType(ItemStack itemStack) {
        GunDefinition gunDefinition = getGunDefinition(itemStack);
        if (gunDefinition != null) {
            return gunDefinition.itemType();
        }

        if (isKnife(itemStack)) return ItemType.KNIFE;
        if (isBomb(itemStack) || isDefuseKit(itemStack)) return ItemType.EQUIPMENT;

        return null;
    }

    public static GunDefinition getGunDefinition(ItemStack itemStack) {
        if (itemStack == null) return null;
        String itemModel = itemStack.get(DataComponents.ITEM_MODEL);
        if (itemModel != null) {
            for (GunDefinition definition : GunDefinition.values()) {
                if (definition.itemModel().equals(itemModel)) {
                    return definition;
                }
            }
        }

        Material material = itemStack.material();
        if (material == Material.GOLDEN_HOE) return GunDefinition.GLOCK;
        if (material == Material.STONE_HOE) return GunDefinition.USP_S;
        if (material == Material.DIAMOND_HOE) return GunDefinition.AK47;
        if (material == Material.IRON_HOE) return GunDefinition.M4A1;

        return null;
    }

    public static boolean isKnife(ItemStack itemStack) {
        return itemStack != null && itemStack.material() == Material.IRON_SWORD;
    }

    public static boolean isBuyMenu(ItemStack itemStack) {
        return itemStack != null && itemStack.material() == Material.NETHER_STAR;
    }
}
