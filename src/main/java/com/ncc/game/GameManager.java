package com.ncc.game;

import com.ncc.Main;
import com.ncc.game.bomb.BombManager;
import com.ncc.game.gun.GunManager;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GameManager {

    private static final int MINIMUM_PLAYERS = 1;
    private static final int MAX_PLAYERS = 10;
    private static final int START_MONEY = 800;
    private static final int POST_ROUND_SECONDS = 15;

    public static GameManager gameManager;

    private final PlayerStateManager playerStateManager;
    private final BuyMenuManager buyMenuManager;
    private final LoadoutManager loadoutManager;
    private final MapSessionManager mapSessionManager;

    private BombManager bombManager;
    private GunManager gunManager;

    private Task gameLoopTask;
    private Task bombLoopTask;

    private GameState state = GameState.WAITING;
    private int countdown = 10;
    private int goCountdown = 4;
    private int postRoundCountdown = POST_ROUND_SECONDS;
    private boolean frozen = false;

    public GameManager(com.ncc.map.GameMapConfig cfg, Instance instance) {
        InstanceContainer container = (InstanceContainer) instance;
        this.playerStateManager = new PlayerStateManager(START_MONEY);
        this.buyMenuManager = new BuyMenuManager();
        this.loadoutManager = new LoadoutManager();
        this.mapSessionManager = new MapSessionManager(cfg, container);
        this.bombManager = new BombManager(cfg, instance);
        this.gunManager = new GunManager();
        startGameLoop();
    }

    public BombManager getBombManager() {
        return bombManager;
    }

    public GunManager getGunManager() {
        return gunManager;
    }

    public void openBuyMenu(Player player) {
        buyMenuManager.openBuyMenu(player, state);
    }

    public boolean isBuyMenu(net.minestom.server.inventory.AbstractInventory inventory) {
        return buyMenuManager.isBuyMenu(inventory);
    }

    public void handleBuyMenuClick(Player player, int slot) {
        buyMenuManager.handleBuyMenuClick(player, playerStateManager, this::getGunManager, slot);
    }

    private void startGameLoop() {
        gameLoopTask = MinecraftServer.getSchedulerManager()
                .buildTask(this::tick)
                .repeat(TaskSchedule.tick(20))
                .schedule();

        bombLoopTask = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    if (state == GameState.RUNNING) {
                        bombManager.tick();
                    }
                })
                .repeat(TaskSchedule.tick(1))
                .schedule();
    }

    private void tick() {
        int playerCount = playerStateManager.playerCount();

        switch (state) {
            case WAITING -> tickWaiting(playerCount);
            case COUNTDOWN -> tickCountdown(playerCount);
            case STARTING_ROUND -> sendGoSequence();
            case RUNNING -> {
            }
            case ENDING_ROUND -> tickEndingRound();
        }
    }

    private void tickWaiting(int playerCount) {
        if (playerCount >= MINIMUM_PLAYERS) {
            state = GameState.COUNTDOWN;
            countdown = 10;
        }

        sendActionBar("§cWaiting for players... §8| §f" + playerCount + "§7/§f" + MAX_PLAYERS);
    }

    private void tickCountdown(int playerCount) {
        if (playerCount < MINIMUM_PLAYERS) {
            state = GameState.WAITING;
            return;
        }

        sendActionBar("§aStarting in " + countdown + "s §8| §f" + playerCount + "§7/§f" + MAX_PLAYERS);

        if (--countdown <= 0) {
            startRoundSetup();
            state = GameState.STARTING_ROUND;
        }
    }

    private void tickEndingRound() {
        sendActionBar("§aNext round in " + postRoundCountdown + "s");

        if (--postRoundCountdown <= 0) {
            resetRound();
        }
    }

    public void startPlant(Player player, String site) {
        if (getTeam(player) != TeamSide.T) {
            return;
        }

        bombManager.setPlanting(player, true, site);
    }

    public void forceStartRound() {
        if (state == GameState.RUNNING) {
            return;
        }

        countdown = 0;
        frozen = true;

        startRoundSetup();
        state = GameState.STARTING_ROUND;
    }

    public void endRound(TeamSide winner) {
        if (state == GameState.ENDING_ROUND) {
            return;
        }

        bombManager.cancelPlant();
        frozen = false;
        postRoundCountdown = POST_ROUND_SECONDS;
        state = GameState.ENDING_ROUND;

        Component winText = winner == null
                ? Component.text("ROUND OVER", NamedTextColor.RED, TextDecoration.BOLD)
                : Component.text(winner.name() + " WIN THE ROUND", NamedTextColor.GOLD, TextDecoration.BOLD);

        for (Player player : online()) {
            player.sendMessage(winText);
            player.showTitle(Title.title(
                    winText,
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(200))
            ));
        }
    }

    public void resetRound() {
        rebuildRoundManagers();

        frozen = false;
        goCountdown = 3;
        countdown = 10;
        postRoundCountdown = POST_ROUND_SECONDS;
        state = GameState.COUNTDOWN;

        mapSessionManager.teleportPlayersToTeamSpawns(online(), playerStateManager);
    }

    public void resetMatch() {
        rebuildRoundManagers();

        frozen = false;
        countdown = 10;
        goCountdown = 4;
        postRoundCountdown = POST_ROUND_SECONDS;
        state = GameState.WAITING;

        for (Player player : online()) {
            loadoutManager.prepareLobbyInventory(player, buyMenuManager);
            mapSessionManager.respawnInLobby(player);
        }
    }

    public void forceNextRound() {
        resetRound();
    }

    private void startRoundSetup() {
        mapSessionManager.teleportPlayersToTeamSpawns(online(), playerStateManager);
        frozen = true;
        goCountdown = 3;
        loadoutManager.giveRoundLoadout(online(), playerStateManager, buyMenuManager, gunManager);
    }

    private void sendGoSequence() {
        if (goCountdown <= 0 && state == GameState.STARTING_ROUND) {
            frozen = false;

            for (Player player : online()) {
                player.showTitle(Title.title(
                        Component.text("GO", NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(200))
                ));

                player.playSound(Sound.sound(
                        SoundEvent.BLOCK_NOTE_BLOCK_HARP,
                        Sound.Source.MASTER,
                        1f,
                        0.8f
                ));
            }

            state = GameState.RUNNING;
            return;
        }

        for (Player player : online()) {
            player.showTitle(Title.title(
                    Component.text(String.valueOf(goCountdown)),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(100))
            ));

            player.playSound(Sound.sound(
                    SoundEvent.BLOCK_NOTE_BLOCK_HAT,
                    Sound.Source.MASTER,
                    1f,
                    1.8f
            ));
        }

        goCountdown--;
    }

    public void assignTeam(Player player) {
        playerStateManager.assignTeam(player);
    }

    public void setTeam(Player player, TeamSide side) {
        playerStateManager.setTeam(player, side);
    }

    public TeamSide getTeam(Player player) {
        return playerStateManager.getTeam(player);
    }

    public PlayerSkin getAssignedSkin(Player player) {
        return playerStateManager.getAssignedSkin(player);
    }

    public int getMoney(Player player) {
        return playerStateManager.getMoney(player);
    }

    public void addMoney(Player player, int amount) {
        playerStateManager.addMoney(player, amount);
    }

    public void setMoney(Player player, int amount) {
        playerStateManager.setMoney(player, amount);
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void refreshPlayerFormatting(Player player) {
        playerStateManager.refreshFormatting(player);
    }

    public void handleJoin(Player player) {
        assignTeam(player);
        loadoutManager.prepareLobbyInventory(player, buyMenuManager);
        Main.moneyBelowNameService.syncAll(online(), this::getMoney);
    }

    public void handleLeave(Player player) {
        playerStateManager.remove(player);
        Main.moneyBelowNameService.removeViewer(player);
    }

    public String getCurrentMapName() {
        return mapSessionManager.currentMapName();
    }

    public String getBombSiteAt(Player player) {
        return mapSessionManager.getBombSiteAt(player);
    }

    public boolean inSite(net.minestom.server.coordinate.Pos pos, com.ncc.map.GameMapConfig.Site site) {
        return mapSessionManager.inSite(pos, site);
    }

    public void switchMap(InstanceContainer instance, com.ncc.map.GameMapConfig cfg, String mapName) {
        mapSessionManager.update(instance, cfg, mapName);
        rebuildRoundManagers();

        Main.INSTANCE = instance;
        Main.cfg = cfg;

        playerStateManager.clear();

        for (Player player : online()) {
            assignTeam(player);
            loadoutManager.prepareLobbyInventory(player, buyMenuManager);
            mapSessionManager.moveToLobbyInstance(player, instance);
        }

        Main.moneyBelowNameService.syncAll(online(), this::getMoney);

        state = GameState.WAITING;
        countdown = 10;
        postRoundCountdown = POST_ROUND_SECONDS;
        frozen = false;
    }

    private void rebuildRoundManagers() {
        bombManager = new BombManager(mapSessionManager.config(), mapSessionManager.currentInstance());
        gunManager = new GunManager();
    }

    private List<Player> online() {
        return new ArrayList<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
    }

    private void sendActionBar(String text) {
        for (Player player : online()) {
            player.sendActionBar(Component.text(text, NamedTextColor.GRAY));
        }
    }
}
