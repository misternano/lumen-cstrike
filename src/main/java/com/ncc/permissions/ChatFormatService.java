package com.ncc.permissions;

import com.ncc.game.TeamSide;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;

public final class ChatFormatService {

    private final PermissionService permissionService;

    public ChatFormatService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public Component buildDisplayName(Player player, TeamSide side, int money) {
        return Component.text()
                .append(buildNametagPrefix(player))
                .append(Component.text(player.getUsername(), NamedTextColor.YELLOW))
                .build();
    }

    public Component buildChatMessage(Player player, TeamSide side, String rawMessage) {
        return Component.text()
                .append(buildChatPrefix(player, side))
                .append(Component.text(player.getUsername(), NamedTextColor.YELLOW))
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(Component.text(rawMessage, NamedTextColor.WHITE))
                .build();
    }

    public Component buildNametagPrefix(Player player) {
        return permissionService.getRank(player).prefixComponent();
    }

    public Component buildChatPrefix(Player player, TeamSide side) {
        return Component.text()
                .append(buildNametagPrefix(player))
                .append(teamPrefix(side))
                .build();
    }

    public TextColor nameColor(Player player) {
        return permissionService.getRank(player).nameColor();
    }

    private Component teamPrefix(TeamSide side) {
        if (side == null) {
            return Component.empty();
        }

        return side == TeamSide.CT
                ? Component.text("[CT] ", NamedTextColor.BLUE)
                : Component.text("[T] ", NamedTextColor.RED);
    }
}
