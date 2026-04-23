package com.ncc.listener;

import com.ncc.Main;
import com.ncc.config.ServerConfig;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.registry.RegistryKey;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FallDamageListeners {

    private static final RegistryKey<DamageType> FALL_DAMAGE_TYPE = RegistryKey.unsafeOf("fall");

    private static final Map<UUID, FallState> FALLING = new ConcurrentHashMap<>();

    private FallDamageListeners() {
    }

    public static void register(GlobalEventHandler events) {
        events.addListener(PlayerTickEvent.class, event -> tick(event.getPlayer()));
        events.addListener(PlayerDisconnectEvent.class, event -> FALLING.remove(event.getPlayer().getUuid()));
    }

    private static void tick(Player player) {
        ServerConfig.FallDamage config = Main.config.fallDamage;
        if (!config.enabled || shouldIgnore(player)) {
            reset(player);
            return;
        }

        UUID uuid = player.getUuid();
        double y = player.getPosition().y();

        if (!player.isOnGround()) {
            FALLING.compute(uuid, (ignored, state) -> {
                if (state == null) {
                    return new FallState(y);
                }

                state.highestY = Math.max(state.highestY, y);
                return state;
            });
            return;
        }

        FallState state = FALLING.remove(uuid);
        if (state == null) {
            return;
        }

        float damage = calculateDamage(config, state.highestY - y);
        if (damage > 0.0f) {
            player.damage(FALL_DAMAGE_TYPE, damage);
        }
    }

    private static float calculateDamage(ServerConfig.FallDamage config, double dropBlocks) {
        if (dropBlocks <= 0.0) {
            return 0.0f;
        }

        double sourceDistance = dropBlocks * config.sourceUnitsPerBlock;
        double impactSpeed = Math.sqrt(2.0 * config.sourceGravity * sourceDistance);
        if (impactSpeed <= config.safeFallSpeed) {
            return 0.0f;
        }

        double speedRange = Math.max(1.0, config.fatalFallSpeed - config.safeFallSpeed);
        double csDamage = 100.0 * ((impactSpeed - config.safeFallSpeed) / speedRange);
        double clampedCsDamage = Math.min(100.0, csDamage);

        return (float) (clampedCsDamage / Math.max(0.1f, config.minecraftHealthDivisor));
    }

    private static boolean shouldIgnore(Player player) {
        GameMode gameMode = player.getGameMode();
        return player.isDead()
                || player.isFlying()
                || gameMode == GameMode.CREATIVE
                || gameMode == GameMode.SPECTATOR;
    }

    private static void reset(Player player) {
        FALLING.remove(player.getUuid());
    }

    private static final class FallState {
        private double highestY;

        private FallState(double highestY) {
            this.highestY = highestY;
        }
    }
}
