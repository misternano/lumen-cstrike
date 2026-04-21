package com.ncc.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class TeleportCommand extends Command {

    public TeleportCommand() {
        super("tp");
        setCondition(CommandAccess.require("cstrike.command.teleport"));

        var playerArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        var targetArg = ArgumentType.Word("target")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /tp <player> <target>", NamedTextColor.RED)));

        addSyntax((sender, context) -> {
            Player player = findPlayer(context.get(playerArg));
            Player target = findPlayer(context.get(targetArg));

            if (player == null || target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            player.teleport(target.getPosition());
            sender.sendMessage(Component.text("Teleported ", NamedTextColor.GRAY)
                    .append(Component.text(player.getUsername(), NamedTextColor.YELLOW))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(target.getUsername(), NamedTextColor.GREEN)));
        }, playerArg, targetArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }
}
