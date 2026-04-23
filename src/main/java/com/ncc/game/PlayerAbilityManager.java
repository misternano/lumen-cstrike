package com.ncc.game;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerAbilityManager {

    private static final int GROUND_ORBIT_POINTS = 3;
    private static final int FIRE_HALO_POINTS = 8;
    private static final int FORCEFIELD_COLUMNS = 9;
    private static final double GROUND_ORBIT_RADIUS = 0.68;
    private static final double FIRE_HALO_RADIUS = 0.38;
    private static final double FORCEFIELD_RADIUS = 1.15;
    private static final double FORCEFIELD_RANGE = 10.0;
    private static final double FORCEFIELD_RANGE_SQUARED = FORCEFIELD_RANGE * FORCEFIELD_RANGE;
    private static final double FORCEFIELD_PUSH_STRENGTH = 2.4;
    private static final double FORCEFIELD_LIFT = 0.55;
    private static final long FORCEFIELD_HIT_COOLDOWN_MS = 650L;

    private final Set<UUID> flyEnabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> godEnabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> forcefieldEnabled = ConcurrentHashMap.newKeySet();
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> forcefieldHitCooldowns = new ConcurrentHashMap<>();
    private long particleTick;

    public boolean toggleFly(Player player) {
        return setFly(player, !isFlyEnabled(player));
    }

    public boolean setFly(Player player, boolean enabled) {
        if (enabled) {
            flyEnabled.add(player.getUuid());
            player.setAllowFlying(true);
            player.setFlying(true);
            return true;
        }

        flyEnabled.remove(player.getUuid());
        if (!hasNativeFlight(player)) {
            player.setFlying(false);
            player.setAllowFlying(false);
        }
        return false;
    }

    public boolean toggleGod(Player player) {
        return setGod(player, !isGodEnabled(player));
    }

    public boolean setGod(Player player, boolean enabled) {
        if (enabled) {
            godEnabled.add(player.getUuid());
            player.setInvulnerable(true);
            return true;
        }

        godEnabled.remove(player.getUuid());
        player.setInvulnerable(false);
        return false;
    }

    public boolean toggleForcefield(Player player) {
        return setForcefield(player, !isForcefieldEnabled(player));
    }

    public boolean setForcefield(Player player, boolean enabled) {
        if (enabled) {
            forcefieldEnabled.add(player.getUuid());
            return true;
        }

        forcefieldEnabled.remove(player.getUuid());
        return false;
    }

    public boolean toggleVanish(Player player) {
        return setVanish(player, !isVanished(player));
    }

    public boolean setVanish(Player player, boolean enabled) {
        if (enabled) {
            vanished.add(player.getUuid());
            spawnVanishEffects(player);
            hidePlayerEntity(player);
            return true;
        }

        vanished.remove(player.getUuid());
        showPlayerEntity(player);
        spawnUnvanishEffects(player);
        return false;
    }

    public boolean isFlyEnabled(Player player) {
        return flyEnabled.contains(player.getUuid());
    }

    public boolean isGodEnabled(Player player) {
        return godEnabled.contains(player.getUuid());
    }

    public boolean isForcefieldEnabled(Player player) {
        return forcefieldEnabled.contains(player.getUuid());
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUuid());
    }

    public void remove(Player player) {
        flyEnabled.remove(player.getUuid());
        godEnabled.remove(player.getUuid());
        forcefieldEnabled.remove(player.getUuid());
        vanished.remove(player.getUuid());
        forcefieldHitCooldowns.remove(player.getUuid());
    }

    public void tickParticles() {
        particleTick++;

        for (Player player : net.minestom.server.MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (isFlyEnabled(player)) {
                if (player.isFlying() && !player.isOnGround()) {
                    spawnFlyingParticles(player);
                } else {
                    spawnGroundedFlyParticles(player);
                }
            }

            if (isGodEnabled(player)) {
                spawnGodHaloParticles(player);
            }

            if (isForcefieldEnabled(player)) {
                applyForcefield(player);
                spawnForcefieldParticles(player);
            }
        }
    }

    private void spawnGroundedFlyParticles(Player player) {
        Pos base = player.getPosition().add(0, 0.22, 0);
        double phase = particleTick * 0.42;

        for (int i = 0; i < GROUND_ORBIT_POINTS; i++) {
            double angle = phase + ((Math.PI * 2) / GROUND_ORBIT_POINTS) * i;
            Pos point = base.add(Math.cos(angle) * GROUND_ORBIT_RADIUS, 0, Math.sin(angle) * GROUND_ORBIT_RADIUS);
            spawnParticle(player, Particle.END_ROD, point, Vec.ZERO, 0.0f, 1);
        }
    }

    private void spawnFlyingParticles(Player player) {
        Pos feet = player.getPosition().add(0, -0.08, 0);
        double phase = particleTick * 0.3;

        spawnParticle(player, Particle.CLOUD, feet, new Vec(0.32, 0.03, 0.32), 0.0f, 10);
        spawnParticle(player, Particle.WHITE_SMOKE, feet.add(0, 0.03, 0), new Vec(0.24, 0.02, 0.24), 0.0f, 4);

        for (int i = 0; i < 5; i++) {
            double angle = phase + ((Math.PI * 2) / 5) * i;
            Pos point = feet.add(Math.cos(angle) * 0.42, 0.02, Math.sin(angle) * 0.42);
            spawnParticle(player, Particle.CLOUD, point, Vec.ZERO, 0.0f, 1);
        }
    }

    private void spawnGodHaloParticles(Player player) {
        Pos center = player.getPosition().add(0, player.getEyeHeight() + 0.24, 0);
        double phase = particleTick * 0.28;

        for (int i = 0; i < FIRE_HALO_POINTS; i++) {
            double angle = phase + ((Math.PI * 2) / FIRE_HALO_POINTS) * i;
            Pos point = center.add(Math.cos(angle) * FIRE_HALO_RADIUS, 0, Math.sin(angle) * FIRE_HALO_RADIUS);
            spawnParticle(player, Particle.SMALL_FLAME, point, Vec.ZERO, 0.0f, 1);
        }
    }

    private void applyForcefield(Player owner) {
        Instance instance = owner.getInstance();
        if (instance == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Player target : instance.getPlayers()) {
            if (target.equals(owner) || target.isDead()) {
                continue;
            }

            Vec delta = target.getPosition().sub(owner.getPosition()).asVec();
            Vec horizontal = new Vec(delta.x(), 0, delta.z());
            if (horizontal.lengthSquared() > FORCEFIELD_RANGE_SQUARED) {
                continue;
            }

            Long nextHitAt = forcefieldHitCooldowns.get(target.getUuid());
            if (nextHitAt != null && now < nextHitAt) {
                continue;
            }

            Vec direction = horizontal.lengthSquared() < 0.001
                    ? new Vec(0, 0, 1)
                    : horizontal.normalize();
            target.setVelocity(direction.mul(FORCEFIELD_PUSH_STRENGTH).add(0, FORCEFIELD_LIFT, 0));
            target.playSound(Sound.sound(
                    SoundEvent.ENTITY_ENDERMAN_TELEPORT,
                    Sound.Source.MASTER,
                    0.85f,
                    1.65f
            ));

            forcefieldHitCooldowns.put(target.getUuid(), now + FORCEFIELD_HIT_COOLDOWN_MS);
        }
    }

    private void spawnForcefieldParticles(Player player) {
        Pos base = player.getPosition();
        double phase = particleTick * 0.22;

        for (int i = 0; i < FORCEFIELD_COLUMNS; i++) {
            double angle = phase + ((Math.PI * 2) / FORCEFIELD_COLUMNS) * i;
            double x = Math.cos(angle) * FORCEFIELD_RADIUS;
            double z = Math.sin(angle) * FORCEFIELD_RADIUS;
            double y = 0.18 + ((i + particleTick) % 6) * 0.34;
            Pos point = base.add(x, y, z);

            spawnParticle(player, Particle.PORTAL, point, new Vec(0.02, 0.02, 0.02), 0.0f, 1);
            if (i % 3 == 0) {
                spawnParticle(player, Particle.REVERSE_PORTAL, point.add(0, 0.18, 0), Vec.ZERO, 0.0f, 1);
            }
        }
    }

    private void spawnVanishEffects(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) {
            return;
        }

        Pos position = player.getPosition();
        spawnLightningStrike(instance, position);

        instance.playSoundExcept(null, Sound.sound(
                SoundEvent.ENTITY_LIGHTNING_BOLT_THUNDER,
                Sound.Source.MASTER,
                1.0f,
                1.35f
        ), position);
        instance.playSoundExcept(null, Sound.sound(
                SoundEvent.ENTITY_ENDERMAN_TELEPORT,
                Sound.Source.MASTER,
                0.8f,
                0.75f
        ), position);

        spawnParticle(player, Particle.LARGE_SMOKE, position.add(0, 1.0, 0), new Vec(0.42, 0.7, 0.42), 0.02f, 22);
        spawnParticle(player, Particle.SMOKE, position.add(0, 0.6, 0), new Vec(0.55, 0.35, 0.55), 0.01f, 18);
        spawnParticle(player, Particle.FLASH, position.add(0, 1.0, 0), Vec.ZERO, 0.0f, 1);
    }

    private void spawnLightningStrike(Instance instance, Pos position) {
        Entity lightning = new Entity(EntityType.LIGHTNING_BOLT);
        lightning.setInstance(instance, position);
        lightning.scheduleRemove(Duration.ofSeconds(2));
    }

    private void spawnUnvanishEffects(Player player) {
        Instance instance = player.getInstance();
        if (instance == null) {
            return;
        }

        Pos position = player.getPosition();
        instance.playSoundExcept(null, Sound.sound(
                SoundEvent.ITEM_FIRECHARGE_USE,
                Sound.Source.MASTER,
                0.85f,
                1.25f
        ), position);
        instance.playSoundExcept(null, Sound.sound(
                SoundEvent.ENTITY_ILLUSIONER_MIRROR_MOVE,
                Sound.Source.MASTER,
                0.65f,
                1.45f
        ), position);

        spawnParticle(player, Particle.FLAME, position.add(0, 0.65, 0), new Vec(0.45, 0.55, 0.45), 0.03f, 22);
        spawnParticle(player, Particle.SOUL_FIRE_FLAME, position.add(0, 1.05, 0), new Vec(0.35, 0.5, 0.35), 0.02f, 12);
        spawnParticle(player, Particle.FIREWORK, position.add(0, 1.25, 0), new Vec(0.3, 0.55, 0.3), 0.02f, 10);
    }

    private void hidePlayerEntity(Player player) {
        player.updateViewerRule(viewer -> viewer.equals(player));
        for (Player viewer : net.minestom.server.MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                player.removeViewer(viewer);
            }
        }
    }

    private void showPlayerEntity(Player player) {
        player.updateViewerRule(viewer -> true);
        player.updateViewerRule();
    }

    private void spawnParticle(Player source, Particle particle, Pos pos, Vec offset, float speed, int count) {
        Instance instance = source.getInstance();
        if (instance == null) {
            return;
        }

        ParticlePacket packet = new ParticlePacket(particle, true, true, pos, offset, speed, count);
        for (Player viewer : instance.getPlayers()) {
            viewer.sendPacket(packet);
        }
    }

    private boolean hasNativeFlight(Player player) {
        GameMode gameMode = player.getGameMode();
        return gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
    }
}
