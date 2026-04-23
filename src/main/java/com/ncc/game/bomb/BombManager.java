package com.ncc.game.bomb;

import com.ncc.Main;
import com.ncc.config.ServerConfig;
import com.ncc.game.TeamSide;
import com.ncc.map.GameMapConfig;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BombManager {

    private final GameMapConfig cfg;
    private final ServerConfig.Bomb config;
    private Instance instance;

    private BombState state = BombState.NONE;

    private Player planter;
    private Player defuser;
    private ItemEntity bombEntity;

    private final Set<UUID> plantingPlayers = new HashSet<>();
    private final Set<UUID> defusingPlayers = new HashSet<>();

    private String site;

    private int plantProgress;
    private int defuseProgress;
    private int activeDefuseTime;

    private static final RegistryKey<DamageType> EXPLOSION_DAMAGE_TYPE = RegistryKey.unsafeOf("explosion");
    private int bombFuseProgress;
    private int nextBeepAt;

    private Pos bombPos;

    public BombManager(GameMapConfig cfg, Instance instance, ServerConfig.Bomb config) {
        this.cfg = cfg;
        this.instance = instance;
        this.config = config;
        this.activeDefuseTime = config.defuseTicks;
    }

    // --- Plant Management
    public boolean isPlanting(Player p) {
        return plantingPlayers.contains(p.getUuid());
    }

    public void setPlanting(Player p, boolean value, String site) {

        if (value) {
            if (state != BombState.NONE && state != BombState.PLANTING) return;
            if (site == null) return;

            plantingPlayers.add(p.getUuid());

            if (planter == null) {
                planter = p;
                this.site = site;
                state = BombState.PLANTING;
            }
        } else {
            plantingPlayers.remove(p.getUuid());

            if (planter != null && planter.equals(p)) {
                cancelPlant();
            }
        }
    }

    private void finishPlant() {
        state = BombState.PLANTED;
        bombPos = planter.getPosition();
        planter = null;
        plantProgress = 0;

        bombFuseProgress = 0;
        nextBeepAt = 0;

        spawnBombModel();
    }

    private void spawnBombModel() {

        if (bombPos == null || instance == null) return;

        ItemStack bombItem = ItemStack.of(Material.ANCIENT_DEBRIS);

        bombEntity = new ItemEntity(bombItem);

        bombEntity.setInstance(instance, bombPos.add(0, 0.5, 0));

        bombEntity.setPickupDelay(Duration.ofDays(Integer.MAX_VALUE));
        bombEntity.setVelocity(new Vec(0, 0, 0));
    }

    public void cancelPlant() {

        state = BombState.NONE;

        planter = null;
        site = null;
        plantProgress = 0;
    }

    // --- Defuse Management
    public boolean isDefusing(Player p) {
        return defusingPlayers.contains(p.getUuid());
    }

    public void setDefusing(Player p, boolean value) {

        if (value) {
            if (state != BombState.PLANTED && state != BombState.DEFUSING) return;

            defusingPlayers.add(p.getUuid());

            if (defuser == null) {
                defuser = p;
                activeDefuseTime = playerHasDefuseKit(p) ? config.defuseWithKitTicks : config.defuseTicks;
                state = BombState.DEFUSING;
            }
        } else {
            defusingPlayers.remove(p.getUuid());

            if (defuser != null && defuser.equals(p)) {
                cancelDefuse();
            }
        }
    }

    public void cancelDefuse() {

        state = BombState.PLANTED;

        defuser = null;
        defuseProgress = 0;
        activeDefuseTime = config.defuseTicks;
    }

    private void finishDefuse() {
        resetBombState();

        // TODO: CT WIN ROUND
    }

    // --- Tick Management
    public void tick() {

        switch (state) {

            case PLANTING -> {

                if (planter == null || planter.isDead()) {
                    cancelPlant();
                    return;
                }

                if (!plantingPlayers.contains(planter.getUuid())) {
                    cancelPlant();
                    return;
                }

                if (!isInSite(planter, site)) {
                    cancelPlant();
                    return;
                }

                plantProgress++;
                sendPlantBar(planter);

                if (plantProgress >= config.plantTicks) {
                    finishPlant();
                }
            }

            case DEFUSING -> {
                if (tickBombFuse()) {
                    return;
                }

                animateBombModel();

                if (defuser == null || defuser.isDead()) {
                    cancelDefuse();
                    return;
                }

                if (!defusingPlayers.contains(defuser.getUuid())) {
                    cancelDefuse();
                    return;
                }

                if (!isNearBomb(defuser)) {
                    cancelDefuse();
                    return;
                }

                defuseProgress++;
                sendDefuseBar(defuser);

                if (defuseProgress >= activeDefuseTime) {
                    finishDefuse();
                }
            }

            case PLANTED -> {
                if (tickBombFuse()) {
                    return;
                }

                animateBombModel();
            }

            default -> {}
        }
    }

    // --- Utilities :: Bomb
    private boolean tickBombFuse() {
        bombFuseProgress++;
        playBombTickIfNeeded();

        if (bombFuseProgress >= config.fuseTicks) {
            explodeBomb();
            return true;
        }

        return false;
    }

    private void animateBombModel() {
        if (bombEntity == null || bombPos == null) return;

        double t = System.currentTimeMillis() / 200.0;
        double yOffset = 0.3 + Math.sin(t) * 0.05;

        bombEntity.teleport(bombPos.add(0.5, yOffset, 0.5));
    }

    private void playBombTickIfNeeded() {
        if (bombFuseProgress < nextBeepAt) return;

        int remaining = config.fuseTicks - bombFuseProgress;
        int interval;

        if (remaining > 600) interval = 20;
        else if (remaining > 400) interval = 12;
        else if (remaining > 200) interval = 7;
        else interval = 4;

        nextBeepAt = bombFuseProgress + interval;

        for (Player p : instance.getPlayers()) {
            float pitch = remaining > 40 ? 1.2f : 1.8f;
            p.playSound(Sound.sound(
                    SoundEvent.BLOCK_NOTE_BLOCK_HAT,
                    Sound.Source.MASTER,
                    0.9f,
                    pitch
            ));
        }
    }

    private void explodeBomb() {
        if (instance == null || bombPos == null) {
            resetBombState();
            return;
        }

        Pos center = bombPos.add(0.5, 0.6, 0.5);

        for (Player player : instance.getPlayers()) {
            player.playSound(Sound.sound(
                    SoundEvent.ENTITY_GENERIC_EXPLODE,
                    Sound.Source.MASTER,
                    1.7f,
                    0.9f
            ), center);

            player.playSound(Sound.sound(
                    SoundEvent.ENTITY_DRAGON_FIREBALL_EXPLODE,
                    Sound.Source.MASTER,
                    1.1f,
                    1.15f
            ), center);

            sendExplosionBurst(player, center);
        }

        applyExplosionDamage(center);
        scheduleAftershock(center);
        resetBombState();

        MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    if (Main.gameManager != null) {
                        Main.gameManager.endRound(TeamSide.T);
                    }
                })
                .delay(TaskSchedule.tick(16))
                .schedule();
    }

    private void sendExplosionBurst(Player player, Pos center) {
        player.sendPacket(new ParticlePacket(Particle.EXPLOSION_EMITTER, true, true, center, Vec.ZERO, 0f, 1));
        player.sendPacket(new ParticlePacket(Particle.EXPLOSION, true, true, center, new Vec(0.45, 0.45, 0.45), 0.02f, 14));
        player.sendPacket(new ParticlePacket(Particle.FLAME, true, true, center, new Vec(1.2, 0.7, 1.2), 0.08f, 80));
        player.sendPacket(new ParticlePacket(Particle.SMOKE, true, true, center, new Vec(1.4, 0.9, 1.4), 0.04f, 60));
        player.sendPacket(new ParticlePacket(Particle.LARGE_SMOKE, true, true, center, new Vec(1.8, 1.0, 1.8), 0.03f, 32));
        player.sendPacket(new ParticlePacket(Particle.CAMPFIRE_SIGNAL_SMOKE, true, true, center, new Vec(0.8, 1.4, 0.8), 0.02f, 24));
        player.sendPacket(new ParticlePacket(Particle.LAVA, true, true, center, new Vec(1.0, 0.3, 1.0), 0.15f, 26));
        player.sendPacket(new ParticlePacket(Particle.DUST_PLUME, true, true, center, new Vec(2.2, 0.25, 2.2), 0.04f, 40));
    }

    private void scheduleAftershock(Pos center) {
        for (int step = 1; step <= 4; step++) {
            final int wave = step;

            MinecraftServer.getSchedulerManager()
                    .buildTask(() -> {
                        if (instance == null) return;

                        double radius = 1.5 + wave * 0.9;
                        double height = 0.2 + wave * 0.08;

                        for (Player player : instance.getPlayers()) {
                            player.sendPacket(new ParticlePacket(
                                    Particle.SMOKE,
                                    true,
                                    true,
                                    center.add(0, height, 0),
                                    new Vec(radius, 0.15, radius),
                                    0.02f,
                                    28 + wave * 8
                            ));
                            player.sendPacket(new ParticlePacket(
                                    Particle.FLAME,
                                    true,
                                    true,
                                    center.add(0, height * 0.6, 0),
                                    new Vec(radius * 0.55, 0.12, radius * 0.55),
                                    0.03f,
                                    12 + wave * 4
                            ));
                        }
                    })
                    .delay(TaskSchedule.tick(wave * 2))
                    .schedule();
        }
    }

    private void applyExplosionDamage(Pos center) {
        double radiusSquared = config.explosionDamageRadius * config.explosionDamageRadius;

        for (Player player : instance.getPlayers()) {
            var pos = player.getPosition();

            double dx = pos.x() - center.x();
            double dy = pos.y() - center.y();
            double dz = pos.z() - center.z();
            double distanceSquared = dx * dx + dy * dy + dz * dz;

            if (distanceSquared > radiusSquared) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            float falloff = (float) Math.max(0.15, 1.0 - (distance / config.explosionDamageRadius));
            float damage = config.explosionMaxDamage * falloff;

            player.damage(EXPLOSION_DAMAGE_TYPE, damage);
        }
    }

    private void resetBombState() {
        state = BombState.NONE;

        bombPos = null;
        planter = null;
        defuser = null;
        site = null;

        plantProgress = 0;
        defuseProgress = 0;
        activeDefuseTime = config.defuseTicks;
        bombFuseProgress = 0;
        nextBeepAt = 0;

        plantingPlayers.clear();
        defusingPlayers.clear();

        if (bombEntity != null) {
            bombEntity.remove();
            bombEntity = null;
        }
    }

    // --- Utilities :: Gameplay
    private boolean isNearBomb(Player p) {

        if (bombPos == null) return false;

        var pos = p.getPosition();

        double dx = pos.x() - bombPos.x();
        double dz = pos.z() - bombPos.z();

        return (dx * dx + dz * dz) <= 1.5;
    }

    private boolean isInSite(Player p, String siteName) {

        GameMapConfig.Site site;

        if (siteName.equalsIgnoreCase("A")) {
            site = cfg.bombSites.A;
        } else if (siteName.equalsIgnoreCase("B")) {
            site = cfg.bombSites.B;
        } else {
            return false;
        }

        var pos = p.getPosition();

        double dx = pos.x() - site.center.x();
        double dz = pos.z() - site.center.z();

        return (dx * dx + dz * dz) <= (site.radius * site.radius);
    }

    private boolean playerHasDefuseKit(Player player) {
        var inventory = player.getInventory();

        for (int slot = 0; slot < 46; slot++) {
            if (com.ncc.game.items.ItemRegistry.isDefuseKit(inventory.getItemStack(slot))) {
                return true;
            }
        }

        return false;
    }

    // --- Utilities :: UI
    private void sendPlantBar(Player p) {
        sendBar(p, "Planting", plantProgress, config.plantTicks);
    }

    private void sendDefuseBar(Player p) {
        sendBar(p, "Defusing", defuseProgress, activeDefuseTime);
    }

    private void sendBar(Player p, String label, int prog, int max) {

        int bars = 20;
        int filled = (prog * bars) / max;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "§a|" : "§8|");
        }

        p.sendActionBar(Component.text(label + ": " + sb));
    }

    public BombState getState() {
        return state;
    }
}
