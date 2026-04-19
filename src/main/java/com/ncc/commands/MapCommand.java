package com.ncc.commands;

import com.ncc.Main;
import com.ncc.map.GameMapConfig;
import com.ncc.map.MapConfigLoader;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;

public class MapCommand extends Command {

    public MapCommand() {
        super("map");

        var mapArg = ArgumentType.Word("map");

        addSyntax((sender, context) -> {

            String mapName = context.get(mapArg);
            File file = new File("maps", mapName + ".polar");

            if (!file.exists()) {
                sender.sendMessage("§cMap not found: " + mapName);
                return;
            }

            try {

                InstanceContainer newInstance =
                        MinecraftServer.getInstanceManager().createInstanceContainer();

                newInstance.setChunkLoader(new PolarLoader(file.toPath()));

                File json = new File(file.getParent(), mapName + ".json");
                GameMapConfig cfg = MapConfigLoader.load(json);

                if (cfg == null) {
                    sender.sendMessage("§cMap config invalid");
                    return;
                }

                MinecraftServer.getInstanceManager().registerInstance(newInstance);

                if (Main.gameManager == null) {
                    sender.sendMessage("§cGameManager not initialized yet");
                    return;
                }

                Main.gameManager.switchMap(newInstance, cfg);

                sender.sendMessage("§aSwitched map to " + mapName);

            } catch (Exception e) {
                sender.sendMessage("§cFailed: " + e.getMessage());
                e.printStackTrace();
            }
        }, mapArg);
    }
}