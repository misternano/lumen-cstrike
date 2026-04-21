package com.ncc.game.gun;

import com.ncc.game.items.ItemType;
import net.minestom.server.item.Material;

public enum GunDefinition {
    GLOCK("Glock-18", ItemType.PISTOL, Material.GOLDEN_HOE, 20, 120, 300, 150, 6.0f, 15.0f,
            new double[]{0.0, 0.05, -0.04, 0.08, -0.06, 0.09, -0.08, 0.10},
            new double[]{0.40, 0.42, 0.44, 0.46, 0.48, 0.50, 0.52, 0.54}),
    USP_S("USP-S", ItemType.PISTOL, Material.STONE_HOE, 12, 48, 320, 160, 7.2f, 18.0f,
            new double[]{0.0, 0.04, -0.03, 0.05, -0.04, 0.06, -0.05, 0.06},
            new double[]{0.34, 0.36, 0.38, 0.40, 0.42, 0.44, 0.46, 0.48}),
    AK47("AK-47", ItemType.ASSAULT, Material.DIAMOND_HOE, 30, 90, 100, 1800, 9.5f, 22.0f,
            new double[]{0.0, 0.18, -0.22, 0.35, 0.48, 0.42, -0.18, -0.45, -0.58, -0.50, 0.18, 0.40, 0.58, 0.70, 0.52, -0.18, -0.52, -0.74, -0.58, -0.22},
            new double[]{0.80, 0.88, 0.95, 1.00, 1.06, 1.12, 1.15, 1.18, 1.22, 1.24, 1.28, 1.30, 1.32, 1.34, 1.30, 1.22, 1.16, 1.10, 1.04, 0.96}),
    M4A1("M4A1-S", ItemType.ASSAULT, Material.IRON_HOE, 20, 80, 90, 1700, 8.7f, 20.0f,
            new double[]{0.0, 0.12, -0.10, 0.18, 0.24, 0.20, -0.10, -0.20, -0.26, -0.22, 0.10, 0.20, 0.28, 0.30, 0.24, -0.10},
            new double[]{0.68, 0.72, 0.76, 0.80, 0.84, 0.88, 0.90, 0.92, 0.95, 0.98, 1.00, 1.02, 1.04, 1.06, 1.02, 0.98});

    private final String displayName;
    private final ItemType itemType;
    private final Material material;
    private final int magazineSize;
    private final int reserveAmmo;
    private final int fireIntervalMs;
    private final int reloadTimeMs;
    private final float baseDamage;
    private final float headshotDamage;
    private final double[] recoilYaw;
    private final double[] recoilPitch;

    GunDefinition(String displayName, ItemType itemType, Material material, int magazineSize, int reserveAmmo, int fireIntervalMs,
                  int reloadTimeMs, float baseDamage, float headshotDamage, double[] recoilYaw, double[] recoilPitch) {
        this.displayName = displayName;
        this.itemType = itemType;
        this.material = material;
        this.magazineSize = magazineSize;
        this.reserveAmmo = reserveAmmo;
        this.fireIntervalMs = fireIntervalMs;
        this.reloadTimeMs = reloadTimeMs;
        this.baseDamage = baseDamage;
        this.headshotDamage = headshotDamage;
        this.recoilYaw = recoilYaw;
        this.recoilPitch = recoilPitch;
    }

    public String displayName() {
        return displayName;
    }

    public ItemType itemType() {
        return itemType;
    }

    public Material material() {
        return material;
    }

    public int magazineSize() {
        return magazineSize;
    }

    public int reserveAmmo() {
        return reserveAmmo;
    }

    public int fireIntervalMs() {
        return fireIntervalMs;
    }

    public int reloadTimeMs() {
        return reloadTimeMs;
    }

    public float baseDamage() {
        return baseDamage;
    }

    public float headshotDamage() {
        return headshotDamage;
    }

    public double[] recoilYaw() {
        return recoilYaw;
    }

    public double[] recoilPitch() {
        return recoilPitch;
    }
}
