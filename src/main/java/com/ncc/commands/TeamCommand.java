package com.ncc.commands;

import com.ncc.Main;
import com.ncc.game.TeamSide;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.Arrays;
import java.util.List;

public class TeamCommand extends Command {

    private static final List<String> TEAMS = Arrays.asList(
            "t", "ct"
    );

    public TeamCommand() {
        super("team");

        var teamArg = ArgumentType.Word("team")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    TEAMS.forEach(mode ->
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
            sender.sendMessage(Component.text("Usage: /gamemode <t|ct> [player]", NamedTextColor.RED));
        });

        addSyntax((sender, context) -> {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return;
            }

            String teamName = context.get(teamArg).toUpperCase();

            applyTeam(player, teamName, sender);
        }, teamArg);

        addSyntax((sender, context) -> {

            String teamName = context.get(teamArg).toUpperCase();
            String targetName = context.get(playerArg);

            Player target = MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                    .filter(p -> p.getUsername().equalsIgnoreCase(targetName)).findFirst().orElse(null);

            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            applyTeam(target, teamName, sender);
        }, teamArg, playerArg);
    }

    private void applyTeam(Player target, String teamName, Object sender) {

        TeamSide side;

        try {
            side = TeamSide.valueOf(teamName);
        } catch (Exception e) {
            if (sender instanceof Player p) {
                p.sendMessage(Component.text("Invalid team. <t|ct>", NamedTextColor.RED));
            }
            return;
        }

        Main.gameManager.setTeam(target, side);

        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(p ->
                    p.sendMessage(Component.text(target.getUsername(), NamedTextColor.YELLOW)
                            .append(Component.text(" joined ", NamedTextColor.GRAY))
                            .append(Component.text(side.name().toUpperCase(), NamedTextColor.GREEN, TextDecoration.BOLD)))
                );
    }
}
