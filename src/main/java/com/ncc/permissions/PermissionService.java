package com.ncc.permissions;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionService {

    private final Path databasePath;
    private final Map<String, Rank> ranks = new LinkedHashMap<>();
    private final Map<UUID, String> playerRankIds = new ConcurrentHashMap<>();
    private Rank defaultRank;

    public PermissionService(Path configDirectory) {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to initialize permission config directory", exception);
        }

        this.databasePath = configDirectory.resolve("ranks.db");

        initializeDatabase();
        seedDefaultRanksIfNeeded();
        reload();
    }

    public synchronized void reload() {
        loadRanks();
        loadPlayerRanks();
    }

    public Rank getRank(Player player) {
        String rankId = playerRankIds.get(player.getUuid());
        if (rankId == null) {
            return defaultRank;
        }

        return ranks.getOrDefault(rankId, defaultRank);
    }

    public Collection<Rank> getRanks() {
        return List.copyOf(ranks.values());
    }

    public List<String> getRankIds() {
        return new ArrayList<>(ranks.keySet());
    }

    public Rank getRankById(String id) {
        return ranks.get(Rank.normalizeId(id));
    }

    public synchronized void setRank(Player player, Rank rank) {
        playerRankIds.put(player.getUuid(), rank.id());
        savePlayerRank(player.getUuid(), rank.id());
    }

    public boolean hasPermission(CommandSender sender, String node) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        return getRank(player).hasPermission(node);
    }

    private void initializeDatabase() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS metadata (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS ranks (
                        id TEXT PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        prefix TEXT NOT NULL,
                        color TEXT NOT NULL,
                        weight INTEGER NOT NULL DEFAULT 0,
                        parent_id TEXT,
                        sort_order INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS rank_permissions (
                        rank_id TEXT NOT NULL,
                        permission TEXT NOT NULL,
                        effect INTEGER NOT NULL DEFAULT 1,
                        sort_order INTEGER NOT NULL,
                        PRIMARY KEY (rank_id, permission),
                        FOREIGN KEY (rank_id) REFERENCES ranks(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_ranks (
                        player_uuid TEXT PRIMARY KEY,
                        rank_id TEXT NOT NULL,
                        FOREIGN KEY (rank_id) REFERENCES ranks(id) ON DELETE RESTRICT
                    )
                    """);
            addColumnIfMissing(connection, "ranks", "parent_id", "TEXT");
            addColumnIfMissing(connection, "ranks", "weight", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(connection, "rank_permissions", "effect", "INTEGER NOT NULL DEFAULT 1");
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to initialize rank database", exception);
        }
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String definition) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void seedDefaultRanksIfNeeded() {
        if (hasAnyRanks()) {
            return;
        }

        saveDefaultRanks();
        System.out.println("Seeded default ranks in SQLite: " + databasePath);
    }

    private boolean hasAnyRanks() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1 FROM ranks LIMIT 1")) {
            return resultSet.next();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to inspect rank database", exception);
        }
    }

    private void saveDefaultRanks() {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try {
                List<Rank> defaultRanks = defaultRanks();
                upsertMetadata(connection, "default_rank", "player");

                int rankOrder = 0;
                for (Rank rank : defaultRanks) {
                    upsertRank(connection, rank, rankOrder++);
                }

                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to seed default ranks in SQLite", exception);
        }
    }

    private void loadRanks() {
        try (Connection connection = openConnection()) {
            Map<String, RankEntry> entries = new LinkedHashMap<>();

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("""
                         SELECT id, display_name, prefix, color, weight, parent_id
                         FROM ranks
                         ORDER BY weight DESC, sort_order, id
                         """)) {
                while (resultSet.next()) {
                    RankEntry entry = new RankEntry();
                    entry.id = resultSet.getString("id");
                    entry.displayName = resultSet.getString("display_name");
                    entry.prefix = resultSet.getString("prefix");
                    entry.color = resultSet.getString("color");
                    entry.weight = resultSet.getInt("weight");
                    entry.parent = resultSet.getString("parent_id");
                    entry.permissionValues = new LinkedHashMap<>();
                    entries.put(entry.id, entry);
                }
            }

            if (entries.isEmpty()) {
                throw new IllegalStateException("Rank database must define at least one rank.");
            }

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("""
                         SELECT rank_id, permission, effect
                         FROM rank_permissions
                         ORDER BY rank_id, sort_order, permission
                         """)) {
                while (resultSet.next()) {
                    RankEntry entry = entries.get(resultSet.getString("rank_id"));
                    if (entry != null) {
                        entry.permissionValues.put(resultSet.getString("permission"), resultSet.getInt("effect") != 0);
                    }
                }
            }

            Map<String, Rank> loadedRanks = new LinkedHashMap<>();
            for (RankEntry entry : entries.values()) {
                loadedRanks.put(entry.id, rankFromEntry(entry));
            }
            linkParents(loadedRanks);

            String defaultRankId = getMetadata(connection, "default_rank");
            if (defaultRankId == null || !loadedRanks.containsKey(defaultRankId)) {
                defaultRankId = loadedRanks.keySet().iterator().next();
            }

            ranks.clear();
            ranks.putAll(loadedRanks);
            defaultRank = loadedRanks.get(defaultRankId);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load ranks from SQLite", exception);
        }
    }

    private void loadPlayerRanks() {
        playerRankIds.clear();

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT player_uuid, rank_id FROM player_ranks")) {
            while (resultSet.next()) {
                try {
                    UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
                    String rankId = Rank.normalizeId(resultSet.getString("rank_id"));
                    if (ranks.containsKey(rankId)) {
                        playerRankIds.put(uuid, rankId);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load player ranks from SQLite", exception);
        }
    }

    private void savePlayerRank(UUID uuid, String rankId) {
        try (Connection connection = openConnection()) {
            upsertPlayerRank(connection, uuid, rankId);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save player rank to SQLite", exception);
        }
    }

    private Rank rankFromEntry(RankEntry entry) {
        String id = Rank.normalizeId(entry.id);
        TextColor color = parseColor(entry.color);
        Map<String, Boolean> permissions = entry.permissionValues == null ? Map.of() : entry.permissionValues;

        return new Rank(
                id,
                entry.displayName == null ? id : entry.displayName,
                entry.prefix == null ? "" : entry.prefix,
                color,
                entry.weight,
                entry.parent,
                permissions
        );
    }

    private void linkParents(Map<String, Rank> loadedRanks) {
        for (Rank rank : loadedRanks.values()) {
            String parentId = rank.parentId();
            if (parentId == null) {
                continue;
            }

            Rank parent = loadedRanks.get(parentId);
            if (parent != null && !hasLoadedParentCycle(loadedRanks, rank.id(), parentId)) {
                rank.setParent(parent);
            }
        }
    }

    private void upsertRank(Connection connection, Rank rank, int sortOrder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO ranks (id, display_name, prefix, color, weight, parent_id, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    display_name = excluded.display_name,
                    prefix = excluded.prefix,
                    color = excluded.color,
                    weight = excluded.weight,
                    parent_id = excluded.parent_id,
                    sort_order = excluded.sort_order
                """)) {
            statement.setString(1, rank.id());
            statement.setString(2, rank.displayName());
            statement.setString(3, rank.prefix());
            statement.setString(4, colorToString(rank.nameColor()));
            statement.setInt(5, rank.weight());
            statement.setString(6, rank.parentId());
            statement.setInt(7, sortOrder);
            statement.executeUpdate();
        }

        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM rank_permissions WHERE rank_id = ?")) {
            deleteStatement.setString(1, rank.id());
            deleteStatement.executeUpdate();
        }

        int permissionOrder = 0;
        for (Map.Entry<String, Boolean> permission : rank.permissions().entrySet()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO rank_permissions (rank_id, permission, effect, sort_order)
                    VALUES (?, ?, ?, ?)
                    """)) {
                statement.setString(1, rank.id());
                statement.setString(2, permission.getKey());
                statement.setInt(3, permission.getValue() ? 1 : 0);
                statement.setInt(4, permissionOrder++);
                statement.executeUpdate();
            }
        }
    }

    public synchronized Rank ensureRank(String rankId) {
        String normalized = Rank.normalizeId(rankId);
        Rank existing = ranks.get(normalized);
        if (existing != null) {
            return existing;
        }

        try (Connection connection = openConnection()) {
            ensureRankExists(connection, normalized);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to create rank in SQLite", exception);
        }
        reload();
        return ranks.get(normalized);
    }

    public synchronized boolean createRank(String rankId) {
        String normalized = Rank.normalizeId(rankId);
        if (ranks.containsKey(normalized)) {
            return false;
        }

        try (Connection connection = openConnection()) {
            ensureRankExists(connection, normalized);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to create rank in SQLite", exception);
        }
        reload();
        return true;
    }

    public synchronized boolean deleteRank(String rankId) {
        String normalized = Rank.normalizeId(rankId);
        if (!ranks.containsKey(normalized)) {
            return false;
        }
        if (defaultRank != null && defaultRank.id().equals(normalized)) {
            throw new IllegalArgumentException("You cannot delete the default rank.");
        }

        String fallbackRankId = defaultRank == null ? null : defaultRank.id();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement clearParents = connection.prepareStatement("""
                        UPDATE ranks
                        SET parent_id = NULL
                        WHERE parent_id = ?
                        """)) {
                    clearParents.setString(1, normalized);
                    clearParents.executeUpdate();
                }

                if (fallbackRankId != null) {
                    try (PreparedStatement reassignPlayers = connection.prepareStatement("""
                            UPDATE player_ranks
                            SET rank_id = ?
                            WHERE rank_id = ?
                            """)) {
                        reassignPlayers.setString(1, fallbackRankId);
                        reassignPlayers.setString(2, normalized);
                        reassignPlayers.executeUpdate();
                    }
                }

                try (PreparedStatement deleteRank = connection.prepareStatement("""
                        DELETE FROM ranks
                        WHERE id = ?
                        """)) {
                    deleteRank.setString(1, normalized);
                    deleteRank.executeUpdate();
                }

                connection.commit();
            } catch (RuntimeException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete rank from SQLite", exception);
        }
        reload();
        return true;
    }

    public synchronized void setRankPermission(String rankId, String permission, boolean value) {
        String normalized = Rank.normalizeId(rankId);
        String normalizedPermission = permission.trim().toLowerCase(Locale.ROOT);
        if (normalizedPermission.isEmpty()) {
            throw new IllegalArgumentException("Permission cannot be empty.");
        }

        try (Connection connection = openConnection()) {
            ensureRankExists(connection, normalized);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO rank_permissions (rank_id, permission, effect, sort_order)
                    VALUES (?, ?, ?, COALESCE((SELECT MAX(sort_order) + 1 FROM rank_permissions WHERE rank_id = ?), 0))
                    ON CONFLICT(rank_id, permission) DO UPDATE SET effect = excluded.effect
                    """)) {
                statement.setString(1, normalized);
                statement.setString(2, normalizedPermission);
                statement.setInt(3, value ? 1 : 0);
                statement.setString(4, normalized);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update rank permission in SQLite", exception);
        }
        reload();
    }

    public synchronized boolean removeRankPermission(String rankId, String permission) {
        String normalized = Rank.normalizeId(rankId);
        String normalizedPermission = permission.trim().toLowerCase(Locale.ROOT);
        if (normalizedPermission.isEmpty()) {
            throw new IllegalArgumentException("Permission cannot be empty.");
        }

        int affectedRows;
        try (Connection connection = openConnection()) {
            ensureRankExists(connection, normalized);
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM rank_permissions
                    WHERE rank_id = ? AND permission = ?
                    """)) {
                statement.setString(1, normalized);
                statement.setString(2, normalizedPermission);
                affectedRows = statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to remove rank permission from SQLite", exception);
        }
        reload();
        return affectedRows > 0;
    }

    public synchronized void setRankParent(String rankId, String parentId) {
        String normalized = Rank.normalizeId(rankId);
        String normalizedParent = parentId == null || parentId.isBlank() || parentId.equalsIgnoreCase("none")
                ? null
                : Rank.normalizeId(parentId);

        try (Connection connection = openConnection()) {
            ensureRankExists(connection, normalized);
            if (normalizedParent != null) {
                ensureRankExists(connection, normalizedParent);
            }

            Map<String, String> parentIds = readParentIds(connection);
            parentIds.put(normalized, normalizedParent);
            if (normalizedParent != null && hasParentCycle(parentIds, normalized, normalizedParent)) {
                throw new IllegalArgumentException("Rank parent would create an inheritance cycle.");
            }

            try (PreparedStatement statement = connection.prepareStatement("UPDATE ranks SET parent_id = ? WHERE id = ?")) {
                statement.setString(1, normalizedParent);
                statement.setString(2, normalized);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update rank parent in SQLite", exception);
        }
        reload();
    }

    public synchronized void setRankDisplayName(String rankId, String displayName) {
        updateRankField(rankId, "display_name", displayName);
    }

    public synchronized void setRankPrefix(String rankId, String prefix) {
        updateRankField(rankId, "prefix", prefix);
    }

    public synchronized void setRankColor(String rankId, String color) {
        TextColor parsedColor = parseColor(color);
        updateRankField(rankId, "color", colorToString(parsedColor));
    }

    public synchronized void setRankWeight(String rankId, int weight) {
        updateRankField(rankId, "weight", Integer.toString(weight));
    }

    private void updateRankField(String rankId, String column, String value) {
        String normalized = Rank.normalizeId(rankId);
        try (Connection connection = openConnection()) {
            ensureRankExists(connection, normalized);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE ranks SET " + column + " = ? WHERE id = ?")) {
                statement.setString(1, value);
                statement.setString(2, normalized);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update rank " + column + " in SQLite", exception);
        }
        reload();
    }

    private void ensureRankExists(Connection connection, String rankId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT 1 FROM ranks WHERE id = ?")) {
            select.setString(1, rankId);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO ranks (id, display_name, prefix, color, weight, parent_id, sort_order)
                VALUES (?, ?, '', 'white', 0, NULL, COALESCE((SELECT MAX(sort_order) + 1 FROM ranks), 0))
                """)) {
            statement.setString(1, rankId);
            statement.setString(2, titleFromId(rankId));
            statement.executeUpdate();
        }
    }

    private void upsertPlayerRank(Connection connection, UUID uuid, String rankId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_ranks (player_uuid, rank_id)
                VALUES (?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET rank_id = excluded.rank_id
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, rankId);
            statement.executeUpdate();
        }
    }

    private void upsertMetadata(Connection connection, String key, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO metadata (key, value)
                VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    private String getMetadata(Connection connection, String key) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM metadata WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("value") : null;
            }
        }
    }

    private TextColor parseColor(String colorName) {
        String normalized = colorName == null ? "white" : colorName.trim().toLowerCase(Locale.ROOT);
        NamedTextColor namedColor = NamedTextColor.NAMES.value(normalized);
        if (namedColor != null) {
            return namedColor;
        }

        String hex = normalized;
        if (hex.matches("[0-9a-f]{6}")) {
            hex = "#" + hex;
        } else if (hex.startsWith("0x") && hex.length() == 8) {
            hex = "#" + hex.substring(2);
        }

        TextColor color = TextColor.fromHexString(hex);
        if (color == null) {
            throw new IllegalStateException("Unknown rank color: " + colorName);
        }
        return color;
    }

    private String colorToString(TextColor color) {
        if (color instanceof NamedTextColor namedColor) {
            String named = NamedTextColor.NAMES.key(namedColor);
            if (named != null) {
                return named;
            }
        }
        return String.format(Locale.ROOT, "#%06X", color.value());
    }

    private Map<String, String> readParentIds(Connection connection) throws SQLException {
        Map<String, String> parentIds = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT id, parent_id FROM ranks")) {
            while (resultSet.next()) {
                parentIds.put(resultSet.getString("id"), resultSet.getString("parent_id"));
            }
        }
        return parentIds;
    }

    private boolean hasLoadedParentCycle(Map<String, Rank> loadedRanks, String childId, String parentId) {
        Map<String, String> parentIds = new LinkedHashMap<>();
        for (Rank rank : loadedRanks.values()) {
            parentIds.put(rank.id(), rank.parentId());
        }
        return hasParentCycle(parentIds, childId, parentId);
    }

    private boolean hasParentCycle(Map<String, String> parentIds, String childId, String parentId) {
        String current = parentId;
        while (current != null) {
            if (current.equals(childId)) {
                return true;
            }
            current = parentIds.get(current);
        }
        return false;
    }

    private String titleFromId(String id) {
        StringBuilder builder = new StringBuilder();
        for (String part : id.split("[_-]+")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? id : builder.toString();
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private List<Rank> defaultRanks() {
        return List.of(
                rank("director", "Director", "[DIRECTOR] ", "red", 1000, "*"),
                rank("supervisor", "Supervisor", "[SUPERVISOR] ", "dark_purple", 900,
                        "cstrike.command.*",
                        "cstrike.rank.manage",
                        "cstrike.rank.view"),
                rank("head_administrator", "Head Administrator", "[HEAD ADMINISTRATOR] ", "gold", 800,
                        "cstrike.command.*",
                        "cstrike.rank.manage",
                        "cstrike.rank.view"),
                rank("administrator", "Administrator", "[ADMINISTRATOR] ", "blue", 700,
                        "cstrike.command.maps",
                        "cstrike.command.match",
                        "cstrike.command.team.self",
                        "cstrike.command.team.others",
                        "cstrike.command.teleport",
                        "cstrike.rank.view"),
                rank("moderator", "Moderator", "[MODERATOR] ", "dark_green", 600,
                        "cstrike.command.maps",
                        "cstrike.command.teleport",
                        "cstrike.rank.view"),
                rank("player", "Player", "", "white", 0,
                        "cstrike.command.maps",
                        "cstrike.rank.view")
        );
    }

    private Rank rank(String id, String displayName, String prefix, String color, int weight, String... permissions) {
        Map<String, Boolean> permissionValues = new LinkedHashMap<>();
        for (String permission : permissions) {
            permissionValues.put(permission, true);
        }
        return new Rank(id, displayName, prefix, parseColor(color), weight, null, permissionValues);
    }

    private static final class RankEntry {
        private String id;
        private String displayName;
        private String prefix;
        private String color;
        private int weight;
        private String parent;
        private Map<String, Boolean> permissionValues;
    }
}
