package com.ncc.permissions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;
import java.util.Set;

public enum Rank {
    DIRECTOR("Director", "[DIRECTOR] ", NamedTextColor.RED, Set.of(
            "*"
    )),
    SUPERVISOR("Supervisor", "[SUPERVISOR] ", NamedTextColor.DARK_PURPLE, Set.of(
            "cstrike.command.*",
            "cstrike.rank.manage",
            "cstrike.rank.view"
    )),
    HEAD_ADMINISTRATOR("Head Administrator", "[HEAD ADMINISTRATOR] ", NamedTextColor.GOLD, Set.of(
            "cstrike.command.*",
            "cstrike.rank.manage",
            "cstrike.rank.view"
    )),
    ADMINISTRATOR("Administrator", "[ADMINISTRATOR] ", NamedTextColor.BLUE, Set.of(
            "cstrike.command.maps",
            "cstrike.command.match",
            "cstrike.command.team.self",
            "cstrike.command.team.others",
            "cstrike.command.teleport",
            "cstrike.rank.view"
    )),
    MODERATOR("Moderator", "[MODERATOR] ", NamedTextColor.DARK_GREEN, Set.of(
            "cstrike.command.maps",
            "cstrike.command.teleport",
            "cstrike.rank.view"
    )),
    PLAYER("", "", NamedTextColor.WHITE, Set.of(
            "cstrike.command.maps",
            "cstrike.rank.view"
    ));

    private final String displayName;
    private final String prefix;
    private final NamedTextColor nameColor;
    private final Set<String> permissions;

    Rank(String displayName, String prefix, NamedTextColor nameColor, Set<String> permissions) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.nameColor = nameColor;
        this.permissions = permissions;
    }

    public String displayName() {
        return displayName;
    }

    public NamedTextColor nameColor() {
        return nameColor;
    }

    public Component prefixComponent() {
        if (prefix.isEmpty()) {
            return Component.empty();
        }

        return Component.text(prefix, nameColor);
    }

    public boolean hasPermission(String node) {
        if (permissions.contains("*") || permissions.contains(node)) {
            return true;
        }

        for (String permission : permissions) {
            if (!permission.endsWith("*")) {
                continue;
            }

            String prefix = permission.substring(0, permission.length() - 1);
            if (node.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    public static Rank fromInput(String input) {
        return Rank.valueOf(input.trim().toUpperCase(Locale.ROOT));
    }
}
