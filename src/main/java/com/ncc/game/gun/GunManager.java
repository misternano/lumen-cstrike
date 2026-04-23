package com.ncc.game.gun;

import com.ncc.Main;
import com.ncc.game.TeamSide;
import com.ncc.game.items.GunItem;
import com.ncc.game.items.ItemRegistry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.sound.CustomSoundEvent;
import net.minestom.server.sound.SoundEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GunManager {

    public static final byte PRIMARY_SLOT = 0;
    public static final byte SECONDARY_SLOT = 1;
    public static final byte KNIFE_SLOT = 2;

    private static final int RECOVERY_DELAY_MS = 180;
    private static final double RECOVERY_YAW_PER_TICK = 0.35;
    private static final double RECOVERY_PITCH_PER_TICK = 0.55;
    private static final double SHOT_RANGE = 50.0;
    private static final double TRACE_STEP = 0.25;
    private static final RegistryKey<DamageType> BULLET_DAMAGE_TYPE = RegistryKey.unsafeOf("player_attack");

    private final Map<UUID, EnumMap<GunDefinition, GunState>> states = new HashMap<>();

    public void giveDefaultLoadout(Player player, TeamSide side) {
        player.getInventory().setItemStack(KNIFE_SLOT, ItemRegistry.KNIFE);

        if (side == TeamSide.T) {
            giveWeapon(player, GunDefinition.GLOCK);
        } else if (side == TeamSide.CT) {
            giveWeapon(player, GunDefinition.USP_S);
        }
    }

    public void giveWeapon(Player player, GunDefinition definition) {
        GunState state = state(player, definition);
        state.ammo = definition.magazineSize();
        state.reserve = definition.reserveAmmo();
        state.reloadEndAt = 0L;
        state.nextShotAt = 0L;
        state.lastShotAt = 0L;
        state.recoilYaw = 0.0;
        state.recoilPitch = 0.0;
        state.shotIndex = 0;

        player.getInventory().setItemStack(slotFor(definition), GunItem.create(definition, state.ammo, state.reserve));
    }

    public void tickPlayer(Player player) {
        EnumMap<GunDefinition, GunState> playerStates = states.get(player.getUuid());
        if (playerStates == null) return;

        long now = System.currentTimeMillis();

        for (Map.Entry<GunDefinition, GunState> entry : playerStates.entrySet()) {
            GunDefinition definition = entry.getKey();
            GunState state = entry.getValue();

            if (state.reloadEndAt > 0L && now >= state.reloadEndAt) {
                completeReload(player, definition, state);
            }

            recoverRecoil(state, now);
            syncItem(player, definition, state);
        }

        GunDefinition heldGun = ItemRegistry.getGunDefinition(player.getItemInMainHand());
        if (heldGun != null) {
            GunState state = state(player, heldGun);
            String text = state.reloadEndAt > 0L
                    ? "§e" + heldGun.displayName() + "  §8|  §7RELOADING"
                    : "§e" + heldGun.displayName() + "  §8|  §7" + state.ammo + "/" + state.reserve;
            player.sendActionBar(Component.text(text, NamedTextColor.GOLD));
        }
    }

    public void tryShoot(Player player) {
        GunDefinition definition = ItemRegistry.getGunDefinition(player.getItemInMainHand());
        if (definition == null) return;
        if (Main.gameManager == null || Main.gameManager.isFrozen()) return;

        GunState state = state(player, definition);
        long now = System.currentTimeMillis();

        if (state.reloadEndAt > 0L) return;
        if (now < state.nextShotAt) return;

        if (state.ammo <= 0) {
            playDryFire(player);
            if (state.reserve > 0) {
                startReload(player, definition, state);
            }
            return;
        }

        state.nextShotAt = now + definition.fireIntervalMs();
        state.lastShotAt = now;

        int patternIndex = Math.min(state.shotIndex, definition.recoilPitch().length - 1);
        double yawKick = definition.recoilYaw()[patternIndex];
        double pitchKick = definition.recoilPitch()[patternIndex];
        state.shotIndex = Math.min(state.shotIndex + 1, definition.recoilPitch().length - 1);
        state.recoilYaw += yawKick;
        state.recoilPitch += pitchKick;

        applyVisualRecoil(player, yawKick, pitchKick);

        state.ammo--;
        syncItem(player, definition, state);

        Instance instance = player.getInstance();
        if (instance == null) return;

        Pos start = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vec direction = directionFromView(player.getPosition().yaw(), player.getPosition().pitch());
        ShotHit hit = traceShot(player, start, direction);
        Pos end = hit != null ? hit.position : start.add(direction.mul(SHOT_RANGE));

        playShotEffects(definition, player, start, end);

        if (hit != null && hit.target != null) {
            float damage = isHeadshot(hit) ? definition.headshotDamage() : definition.baseDamage();
            hit.target.damage(BULLET_DAMAGE_TYPE, damage);
        }
    }

    public void tryReload(Player player) {
        GunDefinition definition = ItemRegistry.getGunDefinition(player.getItemInMainHand());
        if (definition == null) return;

        GunState state = state(player, definition);
        if (state.reloadEndAt > 0L) return;
        if (state.ammo >= definition.magazineSize()) return;
        if (state.reserve <= 0) return;

        startReload(player, definition, state);
    }

    private void startReload(Player player, GunDefinition definition, GunState state) {
        state.reloadEndAt = System.currentTimeMillis() + definition.reloadTimeMs();
        player.playSound(Sound.sound(
                customSound(definition.reloadSound()),
                Sound.Source.MASTER,
                0.8f,
                1.1f
        ));
        player.sendActionBar(Component.text(definition.displayName() + "  §8|  §7RELOADING"));
    }

    private void completeReload(Player player, GunDefinition definition, GunState state) {
        int needed = definition.magazineSize() - state.ammo;
        int loaded = Math.min(needed, state.reserve);

        state.ammo += loaded;
        state.reserve -= loaded;
        state.reloadEndAt = 0L;
        state.shotIndex = 0;

        syncItem(player, definition, state);
    }

    private void syncItem(Player player, GunDefinition definition, GunState state) {
        int slot = slotFor(definition);
        if (slot < 0) return;
        if (!ItemRegistry.isGun(player.getInventory().getItemStack(slot))) return;

        player.getInventory().setItemStack(slot, GunItem.create(definition, state.ammo, state.reserve));
    }

    private void recoverRecoil(GunState state, long now) {
        if (state.reloadEndAt > 0L) return;
        if (now - state.lastShotAt < RECOVERY_DELAY_MS) return;
        if (Math.abs(state.recoilYaw) < 0.01 && Math.abs(state.recoilPitch) < 0.01) {
            state.recoilYaw = 0.0;
            state.recoilPitch = 0.0;
            state.shotIndex = 0;
            return;
        }

        double yawRecover = Math.min(Math.abs(state.recoilYaw), RECOVERY_YAW_PER_TICK);
        double pitchRecover = Math.min(Math.abs(state.recoilPitch), RECOVERY_PITCH_PER_TICK);

        state.recoilYaw -= Math.signum(state.recoilYaw) * yawRecover;
        state.recoilPitch -= Math.signum(state.recoilPitch) * pitchRecover;
    }

    private void applyVisualRecoil(Player player, double yawKick, double pitchKick) {
        float newYaw = (float) (player.getPosition().yaw() + yawKick);
        float newPitch = clampPitch((float) (player.getPosition().pitch() - pitchKick));
        player.setView(newYaw, newPitch);
    }

    private ShotHit traceShot(Player shooter, Pos start, Vec direction) {
        Instance instance = shooter.getInstance();
        if (instance == null) return null;

        for (double distance = 0.0; distance <= SHOT_RANGE; distance += TRACE_STEP) {
            Pos point = start.add(direction.mul(distance));

            if (!instance.getBlock(point).isAir() && instance.getBlock(point).isSolid()) {
                return new ShotHit(null, point);
            }

            for (Player target : instance.getPlayers()) {
                if (target.equals(shooter)) continue;
                if (target.isDead()) continue;
                if (Main.gameManager != null && Main.gameManager.getTeam(target) == Main.gameManager.getTeam(shooter)) continue;

                if (containsPoint(target, point)) {
                    return new ShotHit(target, point);
                }
            }
        }

        return null;
    }

    private boolean containsPoint(Player target, Pos point) {
        var box = target.getBoundingBox();
        var base = target.getPosition();

        double minX = base.x() + box.minX();
        double maxX = base.x() + box.maxX();
        double minY = base.y() + box.minY();
        double maxY = base.y() + box.maxY();
        double minZ = base.z() + box.minZ();
        double maxZ = base.z() + box.maxZ();

        return point.x() >= minX && point.x() <= maxX
                && point.y() >= minY && point.y() <= maxY
                && point.z() >= minZ && point.z() <= maxZ;
    }

    private boolean isHeadshot(ShotHit hit) {
        if (hit.target == null) return false;
        double headLine = hit.target.getPosition().y() + hit.target.getEyeHeight() - 0.18;
        return hit.position.y() >= headLine;
    }

    private void playShotEffects(GunDefinition definition, Player shooter, Pos start, Pos end) {
        Instance instance = shooter.getInstance();
        if (instance == null) return;

        Vec direction = end.sub(start).asVec().normalize();
        Pos muzzle = start.add(direction.mul(0.7));

        instance.playSoundExcept(null, Sound.sound(
                customSound(definition.fireSound()),
                Sound.Source.MASTER,
                0.85f,
                0.65f
        ), muzzle);

        sendParticle(shooter, Particle.SMOKE, muzzle, new Vec(0.01, 0.01, 0.01), 0.0f, 1);
        broadcastParticleExcept(instance, shooter, Particle.FLAME, muzzle, new Vec(0.025, 0.025, 0.025), 0.0f, 2);
        broadcastParticleExcept(instance, shooter, Particle.SMOKE, muzzle, new Vec(0.02, 0.02, 0.02), 0.0f, 2);

        Vec path = end.sub(start).asVec();
        double length = Math.max(1.0, path.length());
        direction = path.normalize();

        for (double distance = 0.5; distance < length; distance += 2.0) {
            Pos tracerPos = start.add(direction.mul(distance));
            broadcastParticle(instance, Particle.DUST_PLUME, tracerPos, new Vec(0.02, 0.02, 0.02), 0.01f, 1);
        }

        broadcastParticle(instance, Particle.FLASH, end, Vec.ZERO, 0f, 1);
        broadcastParticle(instance, Particle.SMOKE, end, new Vec(0.08, 0.08, 0.08), 0.01f, 4);
    }

    private void broadcastParticle(Instance instance, Particle particle, Pos pos, Vec offset, float speed, int count) {
        ParticlePacket packet = new ParticlePacket(particle, true, true, pos, offset, speed, count);
        for (Player viewer : instance.getPlayers()) {
            viewer.sendPacket(packet);
        }
    }

    private void broadcastParticleExcept(Instance instance, Player excluded, Particle particle, Pos pos, Vec offset, float speed, int count) {
        ParticlePacket packet = new ParticlePacket(particle, true, true, pos, offset, speed, count);
        for (Player viewer : instance.getPlayers()) {
            if (viewer.equals(excluded)) continue;
            viewer.sendPacket(packet);
        }
    }

    private void sendParticle(Player player, Particle particle, Pos pos, Vec offset, float speed, int count) {
        player.sendPacket(new ParticlePacket(particle, true, true, pos, offset, speed, count));
    }

    private void playDryFire(Player player) {
        player.playSound(Sound.sound(
                SoundEvent.BLOCK_DISPENSER_FAIL,
                Sound.Source.MASTER,
                0.75f,
                1.4f
        ));
    }

    private SoundEvent customSound(String soundKey) {
        return new CustomSoundEvent(Key.key(soundKey), null);
    }

    private Vec directionFromView(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double xz = Math.cos(pitchRad);

        double x = -xz * Math.sin(yawRad);
        double y = -Math.sin(pitchRad);
        double z = xz * Math.cos(yawRad);

        return new Vec(x, y, z).normalize();
    }

    private float clampPitch(float pitch) {
        return Math.max(-89.0f, Math.min(89.0f, pitch));
    }

    private GunState state(Player player, GunDefinition definition) {
        return states
                .computeIfAbsent(player.getUuid(), ignored -> new EnumMap<>(GunDefinition.class))
                .computeIfAbsent(definition, ignored -> new GunState(definition.magazineSize(), definition.reserveAmmo()));
    }

    private int slotFor(GunDefinition definition) {
        return switch (definition) {
            case GLOCK, USP_S -> SECONDARY_SLOT;
            case AK47, M4A1 -> PRIMARY_SLOT;
        };
    }

    private static final class GunState {
        private int ammo;
        private int reserve;
        private int shotIndex;
        private long nextShotAt;
        private long lastShotAt;
        private long reloadEndAt;
        private double recoilYaw;
        private double recoilPitch;

        private GunState(int ammo, int reserve) {
            this.ammo = ammo;
            this.reserve = reserve;
        }
    }

    private record ShotHit(Player target, Pos position) {
    }
}
