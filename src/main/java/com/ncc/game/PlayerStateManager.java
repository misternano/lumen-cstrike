package com.ncc.game;

import com.ncc.Main;
import com.ncc.skin.Skins;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class PlayerStateManager {

    private final Map<UUID, TeamSide> teams = new HashMap<>();
    private final Map<UUID, Integer> money = new HashMap<>();
    private final Map<UUID, PlayerSkin> assignedSkins = new HashMap<>();
    private final Random random = new Random();
    private final int startMoney;

    public PlayerStateManager(int startMoney) {
        this.startMoney = startMoney;
    }

    public void assignTeam(Player player) {
        setTeam(player, balancedTeam());
    }

    public void setTeam(Player player, TeamSide side) {
        teams.put(player.getUuid(), side);
        money.putIfAbsent(player.getUuid(), startMoney);

        PlayerSkin skin = side == TeamSide.CT
                ? Skins.CT_POOL.get(random.nextInt(Skins.CT_POOL.size()))
                : Skins.T_POOL.get(random.nextInt(Skins.T_POOL.size()));

        assignedSkins.put(player.getUuid(), skin);
        player.setSkin(skin);
        updateNametag(player);
    }

    public PlayerSkin getAssignedSkin(Player player) { return assignedSkins.get(player.getUuid()); }

    public TeamSide getTeam(Player player) {
        return teams.get(player.getUuid());
    }

    public int getMoney(Player player) {
        return money.getOrDefault(player.getUuid(), 0);
    }

    public void addMoney(Player player, int amount) {
        money.put(player.getUuid(), getMoney(player) + amount);
        updateNametag(player);
    }

    public void setMoney(Player player, int amount) {
        money.put(player.getUuid(), Math.max(0, amount));
        updateNametag(player);
    }

    public boolean spendMoney(Player player, int amount) {
        int current = getMoney(player);
        if (current < amount) {
            return false;
        }

        money.put(player.getUuid(), current - amount);
        updateNametag(player);
        return true;
    }

    public int playerCount() {
        return teams.size();
    }

    public void clear() {
        teams.clear();
        money.clear();
        assignedSkins.clear();
    }

    public void refreshFormatting(Player player) {
        updateNametag(player);
    }

    public void remove(Player player) {
        teams.remove(player.getUuid());
        money.remove(player.getUuid());
        assignedSkins.remove(player.getUuid());
        Main.nametagService.remove(player);
    }

    private TeamSide balancedTeam() {
        long ctCount = teams.values().stream().filter(team -> team == TeamSide.CT).count();
        long tCount = teams.values().stream().filter(team -> team == TeamSide.T).count();

        if (ctCount < tCount) return TeamSide.CT;
        if (tCount < ctCount) return TeamSide.T;

        return random.nextBoolean() ? TeamSide.CT : TeamSide.T;
    }

    private void updateNametag(Player player) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
            TeamSide side = teams.get(player.getUuid());
            if (side == null) {
                return;
            }

            int moneyValue = money.getOrDefault(player.getUuid(), 0);
            Main.nametagService.apply(player, side, moneyValue);
            Main.moneyBelowNameService.updateMoney(player, moneyValue);
            Component name = Main.chatFormatService.buildDisplayName(player, side, moneyValue);

            player.setDisplayName(name);
        });
    }
}
