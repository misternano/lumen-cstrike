package com.ncc.commands;

import com.ncc.game.gun.GunManager;
import com.ncc.game.items.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;

import java.util.List;

public class GiveItemCommand extends Command {

    private static final List<String> ITEMS = List.of("bomb", "defusekit", "knife");

    public GiveItemCommand() {
        super("giveitem");

        var playerArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        var itemArg = ArgumentType.Word("item")
                .setSuggestionCallback((sender, context, suggestion) ->
                        ITEMS.forEach(item -> suggestion.addEntry(new SuggestionEntry(item))));

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /giveitem <player> <bomb|defuse|knife>", NamedTextColor.RED)));

        addSyntax((sender, context) -> {
            Player target = findPlayer(context.get(playerArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            String itemName = context.get(itemArg);
            ItemStack itemStack = parseItem(itemName);
            if (itemStack == null) {
                sender.sendMessage(Component.text("Invalid item. <bomb|defuse|knife>", NamedTextColor.RED));
                return;
            }

            giveItem(target, itemName.toLowerCase(), itemStack);

            sender.sendMessage(Component.text("Gave ", NamedTextColor.GRAY)
                    .append(Component.text(prettyName(itemName), NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW)));
        }, playerArg, itemArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    private ItemStack parseItem(String input) {
        return switch (input.toLowerCase()) {
            case "bomb" -> ItemRegistry.BOMB;
            case "defuse", "defusekit", "kit" -> ItemRegistry.DEFUSE;
            case "knife" -> ItemRegistry.KNIFE;
            default -> null;
        };
    }

    private void giveItem(Player target, String itemName, ItemStack itemStack) {
        if ("knife".equals(itemName)) {
            target.getInventory().setItemStack(GunManager.KNIFE_SLOT, itemStack);
            return;
        }

        int preferredSlot = "bomb".equals(itemName) ? 8 : findOpenSlot(target);
        target.getInventory().setItemStack(preferredSlot, itemStack);
    }

    private int findOpenSlot(Player target) {
        for (int slot = 0; slot < 9; slot++) {
            if (target.getInventory().getItemStack(slot).isAir()) {
                return slot;
            }
        }

        return 8;
    }

    private String prettyName(String itemName) {
        return switch (itemName.toLowerCase()) {
            case "defuse", "defusekit", "kit" -> "Defuse Kit";
            case "knife" -> "Knife";
            default -> "Bomb";
        };
    }
}
