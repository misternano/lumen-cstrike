package com.ncc.commands;

import com.ncc.Main;
import com.ncc.permissions.OperatorService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.Player;

public final class CommandAccess {

    private CommandAccess() {
    }

    public static CommandCondition require(String permission) {
        return (sender, commandString) -> {
            if (sender instanceof Player player && player.getPermissionLevel() >= OperatorService.OP_PERMISSION_LEVEL) {
                return true;
            }

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
