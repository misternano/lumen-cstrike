package com.ncc.commands;

import com.ncc.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.condition.CommandCondition;

public final class CommandAccess {

    private CommandAccess() {
    }

    public static CommandCondition require(String permission) {
        return (sender, commandString) -> {
            if (Main.permissionService.hasPermission(sender, permission)) {
                return true;
            }

            if (commandString != null) {
                sender.sendMessage(Component.text("Insufficient permission.", NamedTextColor.RED));
            }
            return false;
        };
    }
}
