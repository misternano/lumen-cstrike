package com.ncc.commands.utility;

import com.ncc.commands.CommandAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

public class TeleportSilentCommand extends Command {

    public TeleportSilentCommand() {
        super("tpo", "stp");
        setCondition(CommandAccess.require("cstrike.command.teleport.silent"));

        var targetArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /tpo <player>", NamedTextColor.RED)));

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return;
            }

            Player target = findPlayer(context.get(targetArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            if (target.equals(player)) {
                sender.sendMessage(Component.text("You are already there.", NamedTextColor.RED));
                return;
            }

            player.teleport(target.getPosition());
            player.sendMessage(Component.text("Silently teleported to ", NamedTextColor.GRAY)
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW)));
        }, targetArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }
}
