package com.ncc.game;

import com.ncc.game.gun.GunDefinition;
import com.ncc.game.gun.GunManager;
import com.ncc.game.items.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.function.Supplier;
import java.util.function.Predicate;

public final class BuyMenuManager {

    private static final int AK47_PRICE = 2700;
    private static final int M4A1_PRICE = 2900;
    private static final int DEFUSE_KIT_PRICE = 400;
    private static final int BUY_MENU_SLOT = 8;

    private final Inventory buyMenu;

    public BuyMenuManager() {
        this.buyMenu = createBuyMenu();
    }

    public boolean canOpenBuyMenu(GameState state) {
        return state == GameState.WAITING || state == GameState.COUNTDOWN || state == GameState.STARTING_ROUND;
    }

    public void openBuyMenu(Player player, GameState state) {
        if (!canOpenBuyMenu(state)) {
            player.sendMessage(Component.text("Buy menu is only available in lobby or freeze time.", NamedTextColor.RED));
            return;
        }

        player.openInventory(buyMenu);
    }

    public boolean isBuyMenu(AbstractInventory inventory) {
        return inventory == buyMenu;
    }

    public void handleBuyMenuClick(Player player, PlayerStateManager playerStateManager, Supplier<GunManager> gunManagerSupplier, int slot) {
        switch (slot) {
            case 2 -> tryBuyAk47(player, playerStateManager, gunManagerSupplier.get());
            case 4 -> tryBuyM4A1(player, playerStateManager, gunManagerSupplier.get());
            case 6 -> tryBuyDefuseKit(player, playerStateManager);
            default -> {
            }
        }
    }

    public void giveBuyMenuItem(Player player) {
        player.getInventory().setItemStack(BUY_MENU_SLOT, ItemRegistry.BUY_MENU);
    }

    private Inventory createBuyMenu() {
        Inventory inventory = new Inventory(InventoryType.CHEST_1_ROW, Component.text("Buy Menu", NamedTextColor.GOLD));

        inventory.setItemStack(2, ItemStack.builder(Material.DIAMOND_HOE)
                .customName(Component.text("AK-47", NamedTextColor.GOLD))
                .lore(
                        Component.text("$" + AK47_PRICE, NamedTextColor.GREEN),
                        Component.text("T-side rifle", NamedTextColor.GRAY)
                )
                .build());

        inventory.setItemStack(4, ItemStack.builder(Material.IRON_HOE)
                .customName(Component.text("M4A1-S", NamedTextColor.AQUA))
                .lore(
                        Component.text("$" + M4A1_PRICE, NamedTextColor.GREEN),
                        Component.text("CT-side rifle", NamedTextColor.GRAY)
                )
                .build());

        inventory.setItemStack(6, ItemStack.builder(Material.LIGHTNING_ROD)
                .customName(Component.text("Defuse Kit", NamedTextColor.AQUA))
                .lore(
                        Component.text("$" + DEFUSE_KIT_PRICE, NamedTextColor.GREEN),
                        Component.text("Cuts defuse time", NamedTextColor.GRAY)
                )
                .build());

        return inventory;
    }

    private void tryBuyAk47(Player player, PlayerStateManager playerStateManager, GunManager gunManager) {
        if (playerStateManager.getTeam(player) != TeamSide.T) {
            player.sendMessage(Component.text("Only T players can buy an AK-47.", NamedTextColor.RED));
            return;
        }

        if (!playerStateManager.spendMoney(player, AK47_PRICE)) {
            player.sendMessage(Component.text("Not enough money for an AK-47.", NamedTextColor.RED));
            return;
        }

        gunManager.giveWeapon(player, GunDefinition.AK47);
        player.sendMessage(Component.text("Purchased ", NamedTextColor.GRAY)
                .append(Component.text("AK-47", NamedTextColor.GREEN, TextDecoration.BOLD)));
        player.closeInventory();
    }

    private void tryBuyM4A1(Player player, PlayerStateManager playerStateManager, GunManager gunManager) {
        if (playerStateManager.getTeam(player) != TeamSide.CT) {
            player.sendMessage(Component.text("Only CT players can buy an M4A1-S.", NamedTextColor.RED));
            return;
        }

        if (!playerStateManager.spendMoney(player, M4A1_PRICE)) {
            player.sendMessage(Component.text("Not enough money for an M4A1-S.", NamedTextColor.RED));
            return;
        }

        gunManager.giveWeapon(player, GunDefinition.M4A1);
        player.sendMessage(Component.text("Purchased ", NamedTextColor.GRAY)
                .append(Component.text("M4A1-S", NamedTextColor.GREEN, TextDecoration.BOLD)));
        player.closeInventory();
    }

    private void tryBuyDefuseKit(Player player, PlayerStateManager playerStateManager) {
        if (hasItem(player, ItemRegistry::isDefuseKit)) {
            player.sendMessage(Component.text("You already have a defuse kit.", NamedTextColor.YELLOW));
            return;
        }

        if (!playerStateManager.spendMoney(player, DEFUSE_KIT_PRICE)) {
            player.sendMessage(Component.text("Not enough money for a defuse kit.", NamedTextColor.RED));
            return;
        }

        player.getInventory().setItemStack(7, ItemRegistry.DEFUSE);
        player.sendMessage(Component.text("Purchased ", NamedTextColor.GRAY)
                .append(Component.text("Defuse Kit", NamedTextColor.GREEN, TextDecoration.BOLD)));
        player.closeInventory();
    }

    private boolean hasItem(Player player, Predicate<ItemStack> matcher) {
        for (int slot = 0; slot < 46; slot++) {
            if (matcher.test(player.getInventory().getItemStack(slot))) {
                return true;
            }
        }

        return false;
    }
}
