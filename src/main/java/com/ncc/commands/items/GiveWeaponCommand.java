package com.ncc.commands.items;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import com.ncc.game.gun.GunDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.List;

public class GiveWeaponCommand extends Command {

    private static final List<String> WEAPONS = List.of("glock", "usp-s", "ak47", "m4a1", "m4a1-s");

    public GiveWeaponCommand() {
        super("giveweapon");
        setCondition(CommandAccess.require("cstrike.command.giveweapon"));

        var playerArg = ArgumentType.Word("player")
                .setSuggestionCallback((sender, context, suggestion) ->
                        MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player ->
                                suggestion.addEntry(new SuggestionEntry(player.getUsername()))));

        var weaponArg = ArgumentType.Word("weapon")
                .setSuggestionCallback((sender, context, suggestion) ->
                        WEAPONS.forEach(weapon -> suggestion.addEntry(new SuggestionEntry(weapon))));

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("Usage: /giveweapon <player> <glock|usp|ak|m4>", NamedTextColor.RED)));

        addSyntax((sender, context) -> {
            Player target = findPlayer(context.get(playerArg));
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }

            GunDefinition definition = parseWeapon(context.get(weaponArg));
            if (definition == null) {
                sender.sendMessage(Component.text("Invalid weapon. <glock|usp|ak|m4>", NamedTextColor.RED));
                return;
            }

            Main.gameManager.getGunManager().giveWeapon(target, definition);

            sender.sendMessage(Component.text("Gave ", NamedTextColor.GRAY)
                    .append(Component.text(definition.displayName(), NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(target.getUsername(), NamedTextColor.YELLOW)));
        }, playerArg, weaponArg);
    }

    private Player findPlayer(String username) {
        return MinecraftServer.getConnectionManager().getOnlinePlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    private GunDefinition parseWeapon(String input) {
        return switch (input.toLowerCase()) {
            case "glock", "glock18" -> GunDefinition.GLOCK;
            case "usp", "usps", "usp-s" -> GunDefinition.USP_S;
            case "ak", "ak47", "ak-47" -> GunDefinition.AK47;
            case "m4", "m4a1", "m4a1-s" -> GunDefinition.M4A1;
            default -> null;
        };
    }
}
