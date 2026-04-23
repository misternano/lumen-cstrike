package com.ncc.commands.match;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import com.ncc.game.TeamSide;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.List;

public class MatchCommand extends Command {

    private static final List<String> ACTIONS = List.of(
            "start",
            "end",
            "next",
            "reset"
    );

    public MatchCommand() {
        super("match");
        setCondition(CommandAccess.require("cstrike.command.match"));

        var actionArg = ArgumentType.Word("mode")
                .setSuggestionCallback((sender, context, suggestion) ->
                        ACTIONS.forEach(mode -> suggestion.addEntry(new SuggestionEntry(mode))));

        setDefaultExecutor((sender, context) ->
                sender.sendMessage("Usage: /match <start|end|next|reset>"));

        addSyntax((sender, context) -> {

            String actionName = context.get(actionArg).toLowerCase();

            switch (actionName) {
                case "start" -> {
                    Main.gameManager.forceStartRound();
                    broadcast("§bRound force started.");
                }
                case "end" -> {
                    Main.gameManager.endRound(TeamSide.CT);
                    broadcast("§cRound ended.");
                }
                case "next" -> {
                    Main.gameManager.forceNextRound();
                    broadcast("§eNext round started.");
                }
                case "reset" -> {
                    Main.gameManager.resetMatch();
                    broadcast("§bMatch reset.");
                }
                default -> sender.sendMessage(Component.text("Invalid action. <start|end|next|reset>", NamedTextColor.RED));
            }
        }, actionArg);
    }

    private void broadcast(String msg) {
        for (Player p : Main.INSTANCE.getPlayers()) {
            p.sendMessage(msg);
        }
    }
}
