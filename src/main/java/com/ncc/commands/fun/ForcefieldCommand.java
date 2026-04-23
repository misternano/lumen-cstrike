package com.ncc.commands.fun;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class ForcefieldCommand extends Command {

    public ForcefieldCommand() {
        super("forcefield", "ff");
        setCondition(CommandAccess.require("cstrike.command.forcefield"));

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return;
            }

            boolean enabled = Main.abilityManager.toggleForcefield(player);
            player.sendMessage(Component.text("Forcefield set to ", NamedTextColor.GRAY)
                    .append(Component.text(enabled ? "ENABLED" : "DISABLED",
                            enabled ? NamedTextColor.GREEN : NamedTextColor.RED,
                            TextDecoration.BOLD)));
        });
    }
}
