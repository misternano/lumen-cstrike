package com.ncc.game;

import com.ncc.map.GameMapConfig;
import com.ncc.map.MapUtil;
import com.ncc.skin.Skins;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.*;

import static com.ncc.Main.MAX_PLAYERS;

public class GameManager {

    private GameMapConfig CFG;
    public static GameManager gameManager;

    private final Map<UUID, TeamSide> teams = new HashMap<>();
    private final List<Player> ct = new ArrayList<>();
    private final List<Player> t = new ArrayList<>();

    private final int MINIMUM_PLAYERS = 1;

    private boolean countdownRunning = false;
    private int countdown = 30;

    private final Random RANDOM = new Random();

    private Task roundCountdownTask;
    private Task pregameTask;
    private Task waitingTask;

    public GameManager(GameMapConfig CFG) {
        this.CFG = CFG;
    }

    public void handleJoin(Player player) {

        TeamSide team = assignTeam();
        teams.put(player.getUuid(), team);

        if (team == TeamSide.CT) {
            ct.add(player);
            player.setSkin(Skins.CT);
        } else {
            t.add(player);
            player.setSkin(Skins.T);
        }

        checkStart();
    }

    private TeamSide assignTeam() {

        if (ct.size() <= t.size()) {
            if (ct.size() < 5) return TeamSide.CT;
            return TeamSide.T;
        } else {
            if (t.size() < 5) return TeamSide.T;
            return TeamSide.CT;
        }
    }

    private void checkStart() {

        int total = ct.size() + t.size();

        if (total < MINIMUM_PLAYERS) {

            countdownRunning = false;
            countdown = 30;

            if (pregameTask != null) pregameTask.cancel();
            if (roundCountdownTask != null) roundCountdownTask.cancel();

            startWaitingCount();
            return;
        }

        startPregameCountdown();
    }

    private void startWaitingCount() {

        if (waitingTask != null) return;

        waitingTask = MinecraftServer.getSchedulerManager().buildTask(() -> {

            int totalPlayers = ct.size() + t.size();

            for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                p.sendActionBar(Component.text("Waiting for players... ", NamedTextColor.GRAY)
                            .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(totalPlayers + "/" + MINIMUM_PLAYERS, NamedTextColor.WHITE)
                        ));
            }

            if (totalPlayers >= MINIMUM_PLAYERS) {
                waitingTask.cancel();
                waitingTask = null;
                checkStart();
            }

        }).repeat(TaskSchedule.seconds(1)).schedule();
    }

    private void startPregameCountdown() {

        if (countdownRunning) return;

        countdownRunning = true;

        if (waitingTask != null) {
            waitingTask.cancel();
            waitingTask = null;
        }

        pregameTask = MinecraftServer.getSchedulerManager().buildTask(() -> {

            int totalPlayers = ct.size() + t.size();

            if (totalPlayers < MINIMUM_PLAYERS) {
                pregameTask.cancel();
                pregameTask = null;
                countdownRunning = false;
                countdown = 30;
                startWaitingCount();
                return;
            }

            for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                p.sendActionBar(Component.text(totalPlayers + "/" + MAX_PLAYERS + " ready", NamedTextColor.GREEN)
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(countdown + "s", NamedTextColor.WHITE)
                    ));
            }

            if (countdown <= 0) {
                pregameTask.cancel();
                pregameTask = null;

                startRound();
                return;
            }

            countdown--;

        }).repeat(TaskSchedule.seconds(1)).schedule();
    }

    private void startRound() {

        countdownRunning = false;
        countdown = 30;

        for (int i = 0; i < ct.size(); i++) {
            var spawn = CFG.ctSpawns.get(i % CFG.ctSpawns.size());
            ct.get(i).teleport(MapUtil.toPos(spawn));
        }

        for (int i = 0; i < t.size(); i++) {
            var spawn = CFG.tSpawns.get(i % CFG.tSpawns.size());
            t.get(i).teleport(MapUtil.toPos(spawn));
        }

        startRoundCountdown();
    }

    private void startRoundCountdown() {

        final int[] time = {3};

        roundCountdownTask = MinecraftServer.getSchedulerManager().buildTask(() -> {

            if (time[0] <= 0) {

                for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {

                    p.showTitle(Title.title(
                            Component.text("GO"),
                            Component.empty(),
                            Title.Times.times(
                                    Duration.ZERO,
                                    Duration.ofMillis(900),
                                    Duration.ofMillis(200)
                            )
                    ));

                    p.playSound(Sound.sound(
                            SoundEvent.ENTITY_PLAYER_LEVELUP,
                            Sound.Source.MASTER,
                            1f,
                            1.2f
                    ));
                }

                roundCountdownTask.cancel();
                roundCountdownTask = null;
                return;
            }

            for (Player p : MinecraftServer.getConnectionManager().getOnlinePlayers()) {

                p.showTitle(Title.title(
                        Component.text(String.valueOf(time[0])),
                        Component.empty(),
                        Title.Times.times(
                                Duration.ZERO,
                                Duration.ofMillis(700),
                                Duration.ofMillis(100)
                        )
                ));

                p.playSound(Sound.sound(
                        SoundEvent.BLOCK_NOTE_BLOCK_HAT,
                        Sound.Source.MASTER,
                        1f,
                        1.8f
                ));
            }

            time[0]--;

        }).repeat(TaskSchedule.seconds(1)).schedule();
    }

    public void switchMap(InstanceContainer newInstance, GameMapConfig cfg) {

        cancelTask(pregameTask);
        cancelTask(waitingTask);
        cancelTask(roundCountdownTask);

        pregameTask = null;
        waitingTask = null;
        roundCountdownTask = null;

        countdownRunning = false;
        countdown = 30;

        ct.clear();
        t.clear();
        teams.clear();

        CFG = cfg;

        List<Player> players = new ArrayList<>(MinecraftServer.getConnectionManager().getOnlinePlayers());

        int ctCount = 0;
        int tCount = 0;

        for (Player p : players) {

            TeamSide team;

            if (ctCount <= tCount) {
                team = ctCount < 5 ? TeamSide.CT : TeamSide.T;
            } else {
                team = tCount < 5 ? TeamSide.T : TeamSide.CT;
            }

            teams.put(p.getUuid(), team);

            if (team == TeamSide.CT) {
                ct.add(p);
                p.setSkin(Skins.CT);
                ctCount++;
            } else {
                t.add(p);
                p.setSkin(Skins.T);
                tCount++;
            }

            GameMapConfig.Spawn spawn = cfg.lobbySpawns.get(RANDOM.nextInt(cfg.lobbySpawns.size()));

            Pos pos = MapUtil.toPos(spawn);

            p.setInstance(newInstance, pos);
            p.setRespawnPoint(pos);
        }

        countdownRunning = false;
        countdown = 30;
        checkStart();
    }

    private void cancelTask(Task task) {
        if (task != null) task.cancel();
    }
}