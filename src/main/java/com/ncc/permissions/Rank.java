package com.ncc.permissions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Rank {

    private final String id;
    private final String displayName;
    private final String prefix;
    private final TextColor nameColor;
    private final int weight;
    private final String parentId;
    private final Map<String, Boolean> permissions;
    private Rank parent;

    public Rank(String id, String displayName, String prefix, TextColor nameColor, int weight, String parentId, Map<String, Boolean> permissions) {
        this.id = id;
        this.displayName = displayName;
        this.prefix = prefix;
        this.nameColor = nameColor;
        this.weight = weight;
        this.parentId = parentId == null || parentId.isBlank() ? null : normalizeId(parentId);
        this.permissions = Collections.unmodifiableMap(new LinkedHashMap<>(permissions));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public TextColor nameColor() {
        return nameColor;
    }

    public int weight() {
        return weight;
    }

    public String prefix() {
        return prefix;
    }

    public String parentId() {
        return parentId;
    }

    public Rank parent() {
        return parent;
    }

    public void setParent(Rank parent) {
        this.parent = parent;
    }

    public Map<String, Boolean> permissions() {
        return permissions;
    }

    public Component prefixComponent() {
        if (prefix.isEmpty()) {
            return Component.empty();
        }

        return Component.text(prefix, nameColor);
    }

    public boolean hasPermission(String node) {
        Boolean localResult = resolveLocalPermission(node);
        if (localResult != null) {
            return localResult;
        }

        return parent != null && parent.hasPermission(node);
    }

    private Boolean resolveLocalPermission(String node) {
        Boolean exact = permissions.get(node);
        if (exact != null) {
            return exact;
        }

        Boolean bestResult = null;
        int bestLength = -1;
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            String permission = entry.getKey();
            if (!permission.endsWith("*")) {
                continue;
            }

            String prefix = permission.substring(0, permission.length() - 1);
            if (node.startsWith(prefix) && prefix.length() > bestLength) {
                bestResult = entry.getValue();
                bestLength = prefix.length();
            }
        }

        return bestResult;
    }

    public static String normalizeId(String input) {
        return input.trim().toLowerCase(Locale.ROOT);
    }

    public static Map<String, Boolean> grantedPermissions(Set<String> permissions) {
        Map<String, Boolean> mapped = new LinkedHashMap<>();
        for (String permission : new LinkedHashSet<>(permissions)) {
            mapped.put(permission, true);
        }
        return mapped;
    }
}
