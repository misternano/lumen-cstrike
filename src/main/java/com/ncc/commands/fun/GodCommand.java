package com.ncc.commands.fun;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class GodCommand extends Command {

    public GodCommand() {
        super("god");
        setCondition(CommandAccess.require("cstrike.command.god"));

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return;
            }

            boolean enabled = Main.abilityManager.toggleGod(player);
            player.sendMessage(Component.text("God mode set to ", NamedTextColor.GRAY)
                    .append(Component.text(enabled ? "ENABLED" : "DISABLED",
                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED,
                            TextDecoration.BOLD)));
        });
    }
}
