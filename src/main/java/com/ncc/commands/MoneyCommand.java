package com.ncc.commands;

import com.ncc.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class MoneyCommand extends Command {

    public MoneyCommand() {
        super("money");

        var playerArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        var amountArg = ArgumentType.Integer("amount");

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /money <player> <amount>", NamedTextColor.RED)));

        addSyntax((sender, context) -> {
            Player target = findPlayer(context.get(playerArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            int amount = context.get(amountArg);
            Main.gameManager.setMoney(target, amount);

            sender.sendMessage(Component.text("Set money for ", NamedTextColor.GRAY)
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text("$" + Math.max(0, amount), NamedTextColor.GREEN, TextDecoration.BOLD)));
        }, playerArg, amountArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }
}
