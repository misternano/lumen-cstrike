package com.ncc.listener;

import com.ncc.Main;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.timer.TaskSchedule;

public final class AbilityListeners {

    private AbilityListeners() {
    }

    public static void register(GlobalEventHandler events) {
        events.addListener(EntityDamageEvent.class, event -> {
            if (event.getEntity() instanceof Player player && Main.abilityManager.isGodEnabled(player)) {
                event.setCancelled(true);
            }
        });

        events.addListener(PlayerDisconnectEvent.class, event ->
                Main.abilityManager.remove(event.getPlayer()));

        MinecraftServer.getSchedulerManager()
                .buildTask(() -> Main.abilityManager.tickParticles())
                .repeat(TaskSchedule.tick(2))
                .schedule();
    }
}
