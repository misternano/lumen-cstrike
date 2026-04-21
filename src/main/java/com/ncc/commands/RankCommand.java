package com.ncc.commands;

import com.ncc.Main;
import com.ncc.permissions.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class RankCommand extends Command {

    public RankCommand() {
        super("rank");

        var playerArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        var rankArg = ArgumentType.Word("rank")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    for (Rank rank : Rank.values()) {
                        suggestion.addEntry(new SuggestionEntry(rank.name().toLowerCase()));
                    }
                });

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Usage: /rank <player> [rank]", NamedTextColor.RED));
                return;
            }

            Rank rank = Main.permissionService.getRank(player);
            sender.sendMessage(Component.text("Your rank: ", NamedTextColor.GRAY)
                    .append(Component.text(rank.name(), rank.nameColor(), TextDecoration.BOLD)));
        });

        addConditionalSyntax(CommandAccess.require("cstrike.rank.view"), (sender, context) -> {
            Player target = findPlayer(context.get(playerArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            Rank rank = Main.permissionService.getRank(target);
            sender.sendMessage(Component.text(target.getUsername(), NamedTextColor.YELLOW)
                    .append(Component.text(" is ", NamedTextColor.GRAY))
                    .append(Component.text(rank.name(), rank.nameColor(), TextDecoration.BOLD)));
        }, playerArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            Player target = findPlayer(context.get(playerArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            Rank rank;
            try {
                rank = Rank.fromInput(context.get(rankArg));
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(Component.text("Invalid rank. <director|supervisor|administrator|moderator>", NamedTextColor.RED));
                return;
            }

            Main.permissionService.setRank(target, rank);
            Main.gameManager.refreshPlayerFormatting(target);

            sender.sendMessage(Component.text("Set rank for ", NamedTextColor.GRAY)
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(rank.name(), rank.nameColor(), TextDecoration.BOLD)));
        }, playerArg, rankArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }
}
