package com.ncc.commands.permissions;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public final class OpCommand extends Command {

    public OpCommand() {
        super("op");

        var playerArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /op <player>", NamedTextColor.RED)));

        addConditionalSyntax(CommandAccess.require("cstrike.command.op"), (sender, context) -> {
            Player target = findPlayer(context.get(playerArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found. They must be online to op them.", NamedTextColor.RED));
                return;
            }

            Main.operatorService.op(target);

            sender.sendMessage(Component.text("Opped ", NamedTextColor.GRAY)
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW))
                    .append(Component.text(" to permission ", NamedTextColor.GRAY))
                    .append(Component.text("LEVEL 4", NamedTextColor.DARK_RED, TextDecoration.BOLD)));

            target.sendMessage(Component.text("You are now opped.", NamedTextColor.GREEN));
        }, playerArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }
}
