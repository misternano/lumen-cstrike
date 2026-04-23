package com.ncc.commands.match;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import com.ncc.map.GameMapConfig;
import com.ncc.map.MapConfigLoader;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;

public class MapCommand extends Command {

    public MapCommand() {
        super("map");
        setCondition(CommandAccess.require("cstrike.command.map"));

        var mapArg = ArgumentType.Word("map").setSuggestionCallback((sender, context, suggestion) -> {

            File folder = Main.config.maps.directoryPath().toFile();

            if (!folder.exists()) return;

            File[] polars = folder.listFiles((dir, name) -> name.endsWith(".polar"));
            if (polars == null) return;

            for (File f : polars) {

                String name = f.getName().replace(".polar", "");

                if (new File(folder, name + ".json").exists()) {
                    suggestion.addEntry(new SuggestionEntry(name));
                }
            }
        });

        setDefaultExecutor((sender, context) -> {

            if (Main.gameManager == null) {
                sender.sendMessage(Component.text("Game not initialized.", NamedTextColor.RED));
                return;
            }
            String current = Main.gameManager.getCurrentMapName();

            if (current == null) {
                sender.sendMessage(Component.text("No map loaded.", NamedTextColor.RED));
                return;
            }

            sender.sendMessage(Component.text("Current map: ", NamedTextColor.GRAY).append(Component.text(current.toUpperCase(), NamedTextColor.GREEN, TextDecoration.BOLD)));
        });

        addSyntax((sender, context) -> {

            String mapName = context.get(mapArg);
            File file = new File(Main.config.maps.directoryPath().toFile(), mapName + ".polar");

            if (!file.exists()) {
                sender.sendMessage(Component.text("Map ", NamedTextColor.RED)
                        .append(Component.text(mapName.toUpperCase(), NamedTextColor.DARK_RED, TextDecoration.BOLD))
                        .append(Component.text(" not found", NamedTextColor.RED))
                );
                return;
            }

            try {

                InstanceContainer newInstance =
                        MinecraftServer.getInstanceManager().createInstanceContainer();

                newInstance.setChunkLoader(new PolarLoader(file.toPath()));

                File json = new File(file.getParent(), mapName + ".json");
                GameMapConfig cfg = MapConfigLoader.load(json);

                if (cfg == null) {
                    sender.sendMessage(Component.text("Map config invalid.", NamedTextColor.RED));
                    return;
                }

                MinecraftServer.getInstanceManager().registerInstance(newInstance);

                if (Main.gameManager == null) {
                    sender.sendMessage(Component.text("GameManager not initialized yet!", NamedTextColor.RED));
                    return;
                }

                Main.gameManager.switchMap(newInstance, cfg, mapName);

                MinecraftServer.getConnectionManager().getOnlinePlayers()
                        .forEach(p -> p.sendMessage(Component.text("Switched map to ", NamedTextColor.GRAY)
                                .append(Component.text(mapName.toUpperCase(), NamedTextColor.GREEN, TextDecoration.BOLD))));

            } catch (Exception e) {
                sender.sendMessage(Component.text("Failed: ", NamedTextColor.RED)
                        .append(Component.text(e.getMessage(), NamedTextColor.DARK_RED)));
                e.printStackTrace();
            }
        }, mapArg);
    }
}
