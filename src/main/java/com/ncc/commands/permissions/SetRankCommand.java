package com.ncc.commands.permissions;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import com.ncc.permissions.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public final class SetRankCommand extends Command {

    public SetRankCommand() {
        super("setrank");

        var playerArg = ArgumentType.String("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        var rankArg = ArgumentType.String("rank")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    for (String rankId : Main.permissionService.getRankIds()) {
                        suggestion.addEntry(new SuggestionEntry(rankId));
                    }
                });

        setDefaultExecutor((sender, context) -> sendHelp(sender));

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            Player target = findPlayer(context.get(playerArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found. They must be online to set their rank.", NamedTextColor.RED));
                return;
            }

            Rank rank = Main.permissionService.getRankById(context.get(rankArg));
            if (rank == null) {
                sender.sendMessage(Component.text("Invalid rank. Available: " + String.join(", ", Main.permissionService.getRankIds()), NamedTextColor.RED));
                return;
            }

            Main.permissionService.setRank(target, rank);
            Main.gameManager.refreshPlayerFormatting(target);

            sender.sendMessage(Component.text("Set rank for ", NamedTextColor.GRAY)
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(rank.displayName(), rank.nameColor(), TextDecoration.BOLD)));
        }, playerArg, rankArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Setrank command", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("/setrank \"player\" \"rank\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Ranks: " + String.join(", ", Main.permissionService.getRankIds()), NamedTextColor.GRAY)));
    }
}
