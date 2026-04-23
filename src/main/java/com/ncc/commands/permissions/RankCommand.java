package com.ncc.commands.permissions;

import com.ncc.Main;
import com.ncc.commands.CommandAccess;
import com.ncc.permissions.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

import java.util.Map;

public class RankCommand extends Command {

    public RankCommand() {
        super("rank");

        var rankArg = ArgumentType.String("rank")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    for (String rankId : Main.permissionService.getRankIds()) {
                        suggestion.addEntry(new SuggestionEntry(rankId));
                    }
                });
        var parentRankArg = ArgumentType.String("parent_rank")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    suggestion.addEntry(new SuggestionEntry("none"));
                    for (String rankId : Main.permissionService.getRankIds()) {
                        suggestion.addEntry(new SuggestionEntry(rankId));
                    }
                });
        var permissionNodeArg = ArgumentType.String("permission_node");
        var permissionValueArg = ArgumentType.String("permission_value")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    suggestion.addEntry(new SuggestionEntry("TRUE"));
                    suggestion.addEntry(new SuggestionEntry("FALSE"));
                });
        var weightValueArg = ArgumentType.String("weight_value")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    suggestion.addEntry(new SuggestionEntry("1000"));
                    suggestion.addEntry(new SuggestionEntry("100"));
                    suggestion.addEntry(new SuggestionEntry("0"));
                });
        var colorValueArg = ArgumentType.String("color_value")
                .setSuggestionCallback((sender, context, suggestion) -> {
                    suggestion.addEntry(new SuggestionEntry("red"));
                    suggestion.addEntry(new SuggestionEntry("#FF0000"));
                    suggestion.addEntry(new SuggestionEntry("FF0000"));
                });
        var displayNameValueArg = ArgumentType.String("displayname_value");
        var prefixValueArg = ArgumentType.String("prefix_value");

        setDefaultExecutor((sender, context) -> sendHelp(sender));

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            boolean created = Main.permissionService.createRank(rankId);
            if (!created) {
                sender.sendMessage(Component.text("Rank ", NamedTextColor.GRAY)
                        .append(Component.text(Rank.normalizeId(rankId), NamedTextColor.AQUA))
                        .append(Component.text(" already exists.", NamedTextColor.GRAY)));
                return;
            }

            Rank rank = Main.permissionService.getRankById(rankId);
            sender.sendMessage(Component.text("Created rank ", NamedTextColor.GRAY)
                    .append(Component.text(rank.id(), NamedTextColor.AQUA))
                    .append(Component.text(".", NamedTextColor.GRAY)));
            refreshFormatting();
        }, ArgumentType.Literal("create"), rankArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            try {
                boolean deleted = Main.permissionService.deleteRank(rankId);
                if (!deleted) {
                    sender.sendMessage(Component.text("Rank not found.", NamedTextColor.RED));
                    return;
                }
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                return;
            }

            sender.sendMessage(Component.text("Deleted rank ", NamedTextColor.GRAY)
                    .append(Component.text(Rank.normalizeId(rankId), NamedTextColor.AQUA))
                    .append(Component.text(".", NamedTextColor.GRAY)));
            refreshFormatting();
        }, ArgumentType.Literal("delete"), rankArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            if (Main.permissionService.getRanks().isEmpty()) {
                sender.sendMessage(Component.text("No ranks found.", NamedTextColor.RED));
                return;
            }

            Component message = Component.text("Ranks:", NamedTextColor.GOLD, TextDecoration.BOLD);
            for (Rank rank : Main.permissionService.getRanks()) {
                message = message.append(Component.newline())
                        .append(Component.text("- ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(rank.id(), NamedTextColor.AQUA))
                        .append(Component.text(" (", NamedTextColor.GRAY))
                        .append(Component.text(rank.displayName(), rank.nameColor()))
                        .append(Component.text(")", NamedTextColor.GRAY))
                        .append(Component.text(" weight=", NamedTextColor.GRAY))
                        .append(Component.text(Integer.toString(rank.weight()), NamedTextColor.YELLOW));

                if (rank.parentId() != null) {
                    message = message.append(Component.text(" -> parent ", NamedTextColor.GRAY))
                            .append(Component.text(rank.parentId(), NamedTextColor.YELLOW));
                }
            }
            sender.sendMessage(message);
        }, ArgumentType.Literal("list"));

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            String permission = context.get(permissionNodeArg);
            Boolean value = parseBoolean(context.get(permissionValueArg));
            if (value == null) {
                sender.sendMessage(Component.text("Permission value must be TRUE or FALSE.", NamedTextColor.RED));
                return;
            }

            Rank rank = Main.permissionService.ensureRank(rankId);
            Main.permissionService.setRankPermission(rank.id(), permission, value);
            sender.sendMessage(Component.text("Set ", NamedTextColor.GRAY)
                    .append(Component.text(permission, NamedTextColor.YELLOW))
                    .append(Component.text(" for rank ", NamedTextColor.GRAY))
                    .append(Component.text(rank.id(), NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(value ? "TRUE" : "FALSE", value ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)));
            refreshFormatting();
        }, rankArg, ArgumentType.Literal("permission"), ArgumentType.Literal("set"), permissionNodeArg, permissionValueArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            String permission = context.get(permissionNodeArg);
            boolean removed = Main.permissionService.removeRankPermission(rankId, permission);
            sender.sendMessage(Component.text(removed ? "Removed " : "No local permission found for ", NamedTextColor.GRAY)
                    .append(Component.text(permission, NamedTextColor.YELLOW))
                    .append(Component.text(" on rank ", NamedTextColor.GRAY))
                    .append(Component.text(Rank.normalizeId(rankId), NamedTextColor.AQUA)));
            refreshFormatting();
        }, rankArg, ArgumentType.Literal("permission"), ArgumentType.Literal("remove"), permissionNodeArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            Rank rank = Main.permissionService.ensureRank(context.get(rankArg));
            Map<String, Boolean> permissions = rank.permissions();
            if (permissions.isEmpty()) {
                sender.sendMessage(Component.text("Rank ", NamedTextColor.GRAY)
                        .append(Component.text(rank.id(), NamedTextColor.AQUA))
                        .append(Component.text(" has no local permissions.", NamedTextColor.GRAY)));
                return;
            }

            Component message = Component.text("Local permissions for ", NamedTextColor.GRAY)
                    .append(Component.text(rank.id(), NamedTextColor.AQUA))
                    .append(Component.text(":", NamedTextColor.GRAY));
            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                message = message.append(Component.newline())
                        .append(Component.text("- ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(entry.getKey(), NamedTextColor.YELLOW))
                        .append(Component.text(" = ", NamedTextColor.GRAY))
                        .append(Component.text(entry.getValue() ? "TRUE" : "FALSE",
                                entry.getValue() ? NamedTextColor.GREEN : NamedTextColor.RED));
            }
            sender.sendMessage(message);
        }, rankArg, ArgumentType.Literal("permission"), ArgumentType.Literal("list"));

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            String parentId = context.get(parentRankArg);
            try {
                Main.permissionService.setRankParent(rankId, parentId);
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                return;
            }

            Rank rank = Main.permissionService.getRankById(rankId);
            String parentDisplay = rank.parentId() == null ? "none" : rank.parentId();
            sender.sendMessage(Component.text("Set parent of ", NamedTextColor.GRAY)
                    .append(Component.text(rank.id(), NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(parentDisplay, NamedTextColor.YELLOW)));
            refreshFormatting();
        }, rankArg, ArgumentType.Literal("parent"), ArgumentType.Literal("set"), parentRankArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            String displayName = context.get(displayNameValueArg);
            if (displayName.isBlank()) {
                sender.sendMessage(Component.text("Display name cannot be empty.", NamedTextColor.RED));
                return;
            }

            Main.permissionService.setRankDisplayName(rankId, displayName);
            sender.sendMessage(Component.text("Set display name for ", NamedTextColor.GRAY)
                    .append(Component.text(Rank.normalizeId(rankId), NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(displayName, NamedTextColor.YELLOW)));
            refreshFormatting();
        }, rankArg, ArgumentType.Literal("displayname"), displayNameValueArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            String prefix = context.get(prefixValueArg);
            Main.permissionService.setRankPrefix(rankId, prefix);
            sender.sendMessage(Component.text("Set prefix for ", NamedTextColor.GRAY)
                    .append(Component.text(Rank.normalizeId(rankId), NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(prefix.isEmpty() ? "<empty>" : prefix, NamedTextColor.YELLOW)));
            refreshFormatting();
        }, rankArg, ArgumentType.Literal("prefix"), prefixValueArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            String weightInput = context.get(weightValueArg);
            int weight;
            try {
                weight = Integer.parseInt(weightInput);
            } catch (NumberFormatException exception) {
                sender.sendMessage(Component.text("Weight must be a whole number.", NamedTextColor.RED));
                return;
            }

            Main.permissionService.setRankWeight(rankId, weight);
            sender.sendMessage(Component.text("Set weight for ", NamedTextColor.GRAY)
                    .append(Component.text(Rank.normalizeId(rankId), NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(Integer.toString(weight), NamedTextColor.YELLOW)));
            refreshFormatting();
        }, rankArg, ArgumentType.Literal("weight"), weightValueArg);

        addConditionalSyntax(CommandAccess.require("cstrike.rank.manage"), (sender, context) -> {
            String rankId = context.get(rankArg);
            String color = context.get(colorValueArg);
            try {
                Main.permissionService.setRankColor(rankId, color);
            } catch (IllegalStateException exception) {
                sender.sendMessage(Component.text("Invalid color. Use a named color or hex like #44AAFF.", NamedTextColor.RED));
                return;
            }

            Rank rank = Main.permissionService.getRankById(rankId);
            sender.sendMessage(Component.text("Set color for ", NamedTextColor.GRAY)
                    .append(Component.text(rank.id(), NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GRAY))
                    .append(Component.text(color, rank.nameColor(), TextDecoration.BOLD)));
            refreshFormatting();
        }, rankArg, ArgumentType.Literal("color"), colorValueArg);
    }

    private Boolean parseBoolean(String input) {
        if (input.equalsIgnoreCase("true")) {
            return true;
        }
        if (input.equalsIgnoreCase("false")) {
            return false;
        }
        return null;
    }

    private void refreshFormatting() {
        MinecraftServer.getConnectionManager().getOnlinePlayers()
                .forEach(player -> Main.gameManager.refreshPlayerFormatting(player));
    }

    private void sendHelp(CommandSender sender) {
        Component message = Component.text("Rank commands", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("/rank create \"rank\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank delete \"rank\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank list", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" permission set \"permission\" \"TRUE/FALSE\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" permission remove \"permission\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" permission list", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" parent set \"rank|none\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" displayname \"display name\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" prefix \"prefix\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" weight \"number\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("/rank \"rank\" color \"named-color|#RRGGBB\"", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Ranks: " + String.join(", ", Main.permissionService.getRankIds()), NamedTextColor.GRAY));

        if (sender instanceof Player player) {
            Rank rank = Main.permissionService.getRank(player);
            message = message.append(Component.newline())
                    .append(Component.text("Your rank: ", NamedTextColor.GRAY))
                    .append(Component.text(rank.displayName(), rank.nameColor(), TextDecoration.BOLD));
        }

        sender.sendMessage(message);
    }
}
