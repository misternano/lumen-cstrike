package com.ncc.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.builder.Command;

import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class MapsCommand extends Command {

    public MapsCommand() {
        super("maps");
        setCondition(CommandAccess.require("cstrike.command.maps"));

        setDefaultExecutor((sender, context) -> {

            File folder = new File("maps");

            if (!folder.exists() || !folder.isDirectory()) {
                sender.sendMessage(Component.text("Maps folder not found.", NamedTextColor.RED));
                return;
            }

            File[] polars = folder.listFiles((dir, name) -> name.endsWith(".polar"));

            if (polars == null || polars.length == 0) {
                sender.sendMessage(Component.text("No maps found.", NamedTextColor.RED));
                return;
            }

            var validMaps = Arrays.stream(polars)
                    .map(f -> f.getName().replace(".polar", ""))
                    .filter(name -> new File(folder, name + ".json").exists())
                    .sorted()
                    .toList();

            if (validMaps.isEmpty()) {
                sender.sendMessage(Component.text("No valid maps (missing .json configs).", NamedTextColor.RED));
                return;
            }

            sender.sendMessage(Component.text("Available maps:", NamedTextColor.GRAY));

            validMaps.forEach(name -> {

                Component line = Component.text(" » ", NamedTextColor.DARK_GRAY)
                        .append(
                                Component.text(name.toUpperCase(), NamedTextColor.GREEN, TextDecoration.BOLD)
                                        .clickEvent(ClickEvent.runCommand("/map " + name))
                                        .hoverEvent(HoverEvent.showText(
                                                Component.text("Click to swap map", NamedTextColor.YELLOW)
                                        ))
                        );

                sender.sendMessage(line);
            });
        });
    }
}
