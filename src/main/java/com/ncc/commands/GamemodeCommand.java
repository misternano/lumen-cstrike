package com.ncc.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

import java.util.Arrays;
import java.util.List;

public class GamemodeCommand extends Command {

    private static final List<String> MODES = Arrays.asList(
            "survival", "creative", "adventure", "spectator"
    );

    public GamemodeCommand() {
        super("gamemode", "gm");

        var modeArg = ArgumentType.Word("mode")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    MODES.forEach(mode ->
                            suggestion.addEntry(new SuggestionEntry(mode))
                    );
                });

        var playerArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(p ->
                            suggestion.addEntry(new SuggestionEntry(p.getUsername()))
                    );
                });

        setDefaultExecutor((sender, context) -> {
            sender.sendMessage(Component.text("Usage: /gamemode <survival|creative|adventure|spectator> [player]", NamedTextColor.RED));
        });

        addSyntax((sender, context) -> {
           if (!(sender instanceof Player p)) {
               sender.sendMessage("Only players can use this command.");
               return;
           }

           String modeName = context.get(modeArg);

           GameMode mode = parseGamemode(modeName);
           if (mode == null) {
               p.sendMessage(Component.text("Invalid gamemode. <survival|creative|adventure|spectator>", NamedTextColor.RED));
               return;
           }

           p.setGameMode(mode);

           p.sendMessage(Component.text("Gamemode set to ", NamedTextColor.GRAY)
                   .append(Component.text(mode.name().toUpperCase(), NamedTextColor.GREEN, TextDecoration.BOLD)));
        }, modeArg);

        addSyntax((sender, context) -> {

            String modeName = context.get(modeArg);
            String targetName = context.get(playerArg);

            GameMode mode = parseGamemode(modeName);
            if (mode == null) {
                sender.sendMessage(Component.text("Invalid gamemode. <survival|creative|adventure|spectator>", NamedTextColor.RED));
                return;
            }

            Player target = MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                    .filter(p -> p.getUsername().equalsIgnoreCase(targetName)).findFirst().orElse(null);

            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            target.setGameMode(mode);

            sender.sendMessage(Component.text("Gamemode set of ", NamedTextColor.GRAY)
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(mode.name().toUpperCase(), NamedTextColor.GREEN, TextDecoration.BOLD)));
        }, modeArg, playerArg);
    }

    private GameMode parseGamemode(String input) {
        return switch (input.toLowerCase()) {
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
