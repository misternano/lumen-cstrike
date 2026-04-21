package com.ncc.game;

import com.ncc.Main;
import com.ncc.game.bomb.BombManager;
import com.ncc.game.gun.GunDefinition;
import com.ncc.game.gun.GunManager;
import com.ncc.game.items.ItemRegistry;
import com.ncc.map.GameMapConfig;
import com.ncc.map.MapUtil;
import com.ncc.skin.Skins;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.ncc.Main.ctTeam;
import static com.ncc.Main.tTeam;

public class GameManager {

    private GameMapConfig CFG;
    private InstanceContainer currentInstance;
    public static GameManager gameManager;
    private BombManager bombManager;
    private GunManager gunManager;

    private final Map<UUID, TeamSide> teams = new HashMap<>();
    private final Map<UUID, Integer> money = new HashMap<>();

    private String currentMapName;

    private static final int MINIMUM_PLAYERS = 1;
    private static final int MAX_TEAM_SIZE = 5;
    private static final int MAX_PLAYERS = 10;
    private static final int START_MONEY = 800;
    private static final int POST_ROUND_SECONDS = 15;
    private static final int AK47_PRICE = 2700;
    private static final int M4A1_PRICE = 2900;
    private static final int DEFUSE_KIT_PRICE = 400;
    private static final int BUY_MENU_SLOT = 8;

    private final Random RANDOM = new Random();

    private Task gameLoopTask;
    private Task bombLoopTask;
    private final Inventory buyMenu;

    private GameState state = GameState.WAITING;

    private int countdown = 10;
    private int goCountdown = 4;
    private int postRoundCountdown = POST_ROUND_SECONDS;

    private boolean frozen = false;

    public GameManager(GameMapConfig cfg, Instance instance) {
        this.CFG = cfg;
        this.currentInstance = (InstanceContainer) instance;
        this.bombManager = new BombManager(cfg, instance);
        this.gunManager = new GunManager();
        this.buyMenu = createBuyMenu();
        startGameLoop();
    }

    public BombManager getBombManager() {
        return bombManager;
    }

    public GunManager getGunManager() {
        return gunManager;
    }

    public boolean canOpenBuyMenu() {
        return state == GameState.WAITING || state == GameState.COUNTDOWN || state == GameState.STARTING_ROUND;
    }

    public void openBuyMenu(Player player) {
        if (!canOpenBuyMenu()) {
            player.sendMessage(Component.text("Buy menu is only available in lobby or freeze time.", NamedTextColor.RED));
            return;
        }

        player.openInventory(buyMenu);
    }

    public boolean isBuyMenu(AbstractInventory inventory) {
        return inventory == buyMenu;
    }

    public void handleBuyMenuClick(Player player, int slot) {
        switch (slot) {
            case 2 -> tryBuyAk47(player);
            case 4 -> tryBuyM4A1(player);
            case 6 -> tryBuyDefuseKit(player);
            default -> {}
        }
    }

    // --- State Loop
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

        int playerCount = teams.size();

        switch (state) {

            case WAITING -> {
                if (playerCount >= MINIMUM_PLAYERS) {
                    state = GameState.COUNTDOWN;
                    countdown = 10;
                }
                sendActionBar("§cWaiting for players... §8| §f" + playerCount + "/" + MAX_PLAYERS);
            }

            case COUNTDOWN -> {
                if (playerCount < MINIMUM_PLAYERS) {
                    state = GameState.WAITING;
                    return;
                }

                sendActionBar("§aStarting in " + countdown + "s §8| §f" + playerCount + "/" + MAX_PLAYERS);

                if (--countdown <= 0) {
                    startRoundSetup();
                    state = GameState.STARTING_ROUND;
                }
            }

            case STARTING_ROUND -> sendGoSequence();

            case RUNNING -> {}

            case ENDING_ROUND -> {
                sendActionBar("Next round in " + postRoundCountdown + "s");

                if (--postRoundCountdown <= 0) {
                    resetRound();
                }
            }
        }
    }

    // --- Round utilities
    public void startPlant(Player p, String site) {

        if (getTeam(p) != TeamSide.T) return;

        bombManager.setPlanting(p, true, site);
    }

    private void giveRoundLoadout() {

        List<Player> ts = new ArrayList<>();

        for (Player p : online()) {
            TeamSide side = getTeam(p);
            p.getInventory().clear();
            giveBuyMenuItem(p);
            gunManager.giveDefaultLoadout(p, side);

            if (side == TeamSide.T) ts.add(p);
        }

        if (!ts.isEmpty()) {
            Player bombCarrier = ts.get(RANDOM.nextInt(ts.size()));

            bombCarrier.getInventory().setItemStack(7, ItemRegistry.BOMB);
            bombCarrier.sendMessage(Component.text("You have the ", NamedTextColor.GRAY)
                    .append(Component.text("bomb", NamedTextColor.DARK_RED)));
        }
    }

    // --- Round Management
    public void forceStartRound() {
        if (state == GameState.RUNNING) return;

        countdown = 0;
        frozen = true;

        startRoundSetup();
        state = GameState.STARTING_ROUND;
    }

    public void endRound(TeamSide winner) {
        if (state == GameState.ENDING_ROUND) return;

        bombManager.cancelPlant();
        frozen = false;
        postRoundCountdown = POST_ROUND_SECONDS;
        state = GameState.ENDING_ROUND;

        String winText = winner == null ? "§c§lROUND OVER" : "§6§l" + winner.name() + " §aWIN THE ROUND";

        for (Player p : online()) {
            p.sendMessage(Component.text(winText));
            p.showTitle(Title.title(
                    Component.text(winText),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(200))
            ));
        }
    }

    public void resetRound() {

        bombManager = new BombManager(CFG, currentInstance);
        gunManager = new GunManager();

        frozen = false;
        goCountdown = 3;
        countdown = 10;
        postRoundCountdown = POST_ROUND_SECONDS;

        state = GameState.COUNTDOWN;

        teleportPlayers();
    }

    public void resetMatch() {

        bombManager = new BombManager(CFG, currentInstance);
        gunManager = new GunManager();

        frozen = false;
        countdown = 10;
        goCountdown = 4;
        postRoundCountdown = POST_ROUND_SECONDS;

        state = GameState.WAITING;

        for (Player p : online()) {
            p.getInventory().clear();
            giveBuyMenuItem(p);

            var spawn = CFG.lobbySpawns.get(RANDOM.nextInt(CFG.lobbySpawns.size()));
            Pos pos = MapUtil.toPos(spawn);

            p.teleport(pos);
            p.setRespawnPoint(pos);
        }
    }

    public void forceNextRound() {
        resetRound();
    }

    // --- Round Start Setup
    private void startRoundSetup() {
        teleportPlayers();
        frozen = true;
        goCountdown = 3;

        giveRoundLoadout();
    }

    private void sendGoSequence() {

        if (goCountdown <= 0 && state == GameState.STARTING_ROUND) {

            frozen = false;

            for (Player p : online()) {
                p.showTitle(Title.title(
                        Component.text("GO", NamedTextColor.GREEN, TextDecoration.BOLD),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(200))
                ));

                p.playSound(Sound.sound(
                        SoundEvent.BLOCK_NOTE_BLOCK_HARP,
                        Sound.Source.MASTER,
                        1f,
                        0.8f
                ));
            }

            state = GameState.RUNNING;
            return;
        }

        for (Player p : online()) {

            int display = goCountdown;

            p.showTitle(Title.title(
                    Component.text(String.valueOf(display)),
                    Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(100))
            ));

            p.playSound(Sound.sound(
                    SoundEvent.BLOCK_NOTE_BLOCK_HAT,
                    Sound.Source.MASTER,
                    1f,
                    1.8f
            ));
        }

        goCountdown--;
    }

    // --- Team Management
    public void assignTeam(Player p) {
        TeamSide side = balancedTeam();
        setTeam(p, side);
        updateNametag(p);
    }

    private TeamSide balancedTeam() {
        long ctCount = count(TeamSide.CT);
        long tCount = count(TeamSide.T);

        if (ctCount < tCount) return TeamSide.CT;
        if (tCount < ctCount) return TeamSide.T;

        return RANDOM.nextBoolean() ? TeamSide.CT : TeamSide.T;
    }

    public void setTeam(Player p, TeamSide side) {

        teams.put(p.getUuid(), side);
        money.putIfAbsent(p.getUuid(), START_MONEY);

        p.setTeam(side == TeamSide.CT ? ctTeam : tTeam);
        p.setSkin(side == TeamSide.CT ? Skins.CT : Skins.T);
        updateNametag(p);
    }

    public TeamSide getTeam(Player p) {
        return teams.get(p.getUuid());
    }

    public void updateNametag(Player p) {

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {

            TeamSide side = teams.get(p.getUuid());
            if (side == null) return;

            int moneyValue = money.getOrDefault(p.getUuid(), 0);

            Component prefix = side == TeamSide.CT
                    ? Component.text("[CT] ", NamedTextColor.BLUE)
                    : Component.text("[T] ", NamedTextColor.RED);

            Component name = Component.text()
                    .append(prefix)
                    .append(Component.text(p.getUsername(), NamedTextColor.WHITE))
                    .append(Component.text(" $" + moneyValue, NamedTextColor.GREEN))
                    .build();

            p.setDisplayName(name);
            p.setCustomNameVisible(true);
        });
    }

    // --- Cash Management
    public int getMoney(Player p) {
        return money.getOrDefault(p.getUuid(), 0);
    }

    public void addMoney(Player p, int amount) {
        money.put(p.getUuid(), getMoney(p) + amount);
        updateNametag(p);
    }

    public void setMoney(Player p, int amount) {
        money.put(p.getUuid(), Math.max(0, amount));
        updateNametag(p);
    }

    // --- Round Starting Logic
    public boolean isFrozen() {
        return frozen;
    }

    private void teleportPlayers() {

        List<Player> players = online();

        int ctIndex = 0;
        int tIndex = 0;

        for (Player p : players) {

            TeamSide side = teams.get(p.getUuid());
            if (side == null) continue;

            if (side == TeamSide.CT) {
                var spawn = CFG.ctSpawns.get(ctIndex++ % CFG.ctSpawns.size());
                p.teleport(MapUtil.toPos(spawn));
            } else {
                var spawn = CFG.tSpawns.get(tIndex++ % CFG.tSpawns.size());
                p.teleport(MapUtil.toPos(spawn));
            }
        }
    }

    // -- Utilities
    public void handleJoin(Player p) {
        assignTeam(p);
        p.getInventory().clear();
        giveBuyMenuItem(p);
    }

    private List<Player> online() {
        return new ArrayList<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
    }

    public String getCurrentMapName() {
        return currentMapName;
    }

    private void sendActionBar(String text) {
        for (Player p : online()) {
            p.sendActionBar(Component.text(text));
        }
    }

    private long count(TeamSide side) {
        return teams.values().stream().filter(t -> t == side).count();
    }

    public String getBombSiteAt(Player p) {
        var pos = p.getPosition();

        if (inSite(pos, CFG.bombSites.A)) return "A";
        if (inSite(pos, CFG.bombSites.B)) return "B";

        return null;
    }

    public boolean inSite(Pos pos, GameMapConfig.Site site) {
        double dx = pos.x() - site.center.x();
        double dz = pos.z() - site.center.z();

        return dx * dx + dz * dz <= site.radius * site.radius;
    }

    public void switchMap(InstanceContainer instance, GameMapConfig cfg, String mapName) {

        this.CFG = cfg;
        this.currentInstance = instance;
        this.currentMapName = mapName;
        this.bombManager = new BombManager(cfg, instance);
        this.gunManager = new GunManager();

        Main.INSTANCE = instance;
        Main.cfg = cfg;

        teams.clear();
        money.clear();

        for (Player p : online()) {
            assignTeam(p);
            p.getInventory().clear();
            giveBuyMenuItem(p);

            var spawn = cfg.lobbySpawns.get(RANDOM.nextInt(cfg.lobbySpawns.size()));
            Pos pos = MapUtil.toPos(spawn);

            p.setInstance(instance, pos);
            p.setRespawnPoint(pos);
        }

        state = GameState.WAITING;
        countdown = 10;
        postRoundCountdown = POST_ROUND_SECONDS;
        frozen = false;
    }

    private Inventory createBuyMenu() {
        Inventory inventory = new Inventory(InventoryType.CHEST_1_ROW, Component.text("Buy Menu", NamedTextColor.GOLD));

        inventory.setItemStack(2, ItemStack.builder(net.minestom.server.item.Material.DIAMOND_HOE)
                .customName(Component.text("AK-47", NamedTextColor.GOLD))
                .lore(
                        Component.text("$" + AK47_PRICE, NamedTextColor.GREEN),
                        Component.text("T-side rifle", NamedTextColor.GRAY)
                )
                .build());

        inventory.setItemStack(4, ItemStack.builder(net.minestom.server.item.Material.IRON_HOE)
                .customName(Component.text("M4A1-S", NamedTextColor.AQUA))
                .lore(
                        Component.text("$" + M4A1_PRICE, NamedTextColor.GREEN),
                        Component.text("CT-side rifle", NamedTextColor.GRAY)
                )
                .build());

        inventory.setItemStack(6, ItemStack.builder(net.minestom.server.item.Material.LIGHTNING_ROD)
                .customName(Component.text("Defuse Kit", NamedTextColor.AQUA))
                .lore(
                        Component.text("$" + DEFUSE_KIT_PRICE, NamedTextColor.GREEN),
                        Component.text("Cuts defuse time", NamedTextColor.GRAY)
                )
                .build());

        return inventory;
    }

    private void giveBuyMenuItem(Player player) {
        player.getInventory().setItemStack(BUY_MENU_SLOT, ItemRegistry.BUY_MENU);
    }

    private void tryBuyAk47(Player player) {
        if (getTeam(player) != TeamSide.T) {
            player.sendMessage(Component.text("Only T players can buy an AK-47.", NamedTextColor.RED));
            return;
        }

        if (!spendMoney(player, AK47_PRICE)) {
            player.sendMessage(Component.text("Not enough money for an AK-47.", NamedTextColor.RED));
            return;
        }

        gunManager.giveWeapon(player, GunDefinition.AK47);
        player.sendMessage(Component.text("Purchased ", NamedTextColor.GRAY)
                .append(Component.text("AK-47", NamedTextColor.GREEN, TextDecoration.BOLD)));
        player.closeInventory();
    }

    private void tryBuyM4A1(Player player) {
        if (getTeam(player) != TeamSide.CT) {
            player.sendMessage(Component.text("Only CT players can buy an M4A1-S.", NamedTextColor.RED));
            return;
        }

        if (!spendMoney(player, M4A1_PRICE)) {
            player.sendMessage(Component.text("Not enough money for an M4A1-S.", NamedTextColor.RED));
            return;
        }

        gunManager.giveWeapon(player, GunDefinition.M4A1);
        player.sendMessage(Component.text("Purchased ", NamedTextColor.GRAY)
                .append(Component.text("M4A1-S", NamedTextColor.GREEN, TextDecoration.BOLD)));
        player.closeInventory();
    }

    private void tryBuyDefuseKit(Player player) {
        if (hasItem(player, ItemRegistry::isDefuseKit)) {
            player.sendMessage(Component.text("You already have a defuse kit.", NamedTextColor.YELLOW));
            return;
        }

        if (!spendMoney(player, DEFUSE_KIT_PRICE)) {
            player.sendMessage(Component.text("Not enough money for a defuse kit.", NamedTextColor.RED));
            return;
        }

        player.getInventory().setItemStack(7, ItemRegistry.DEFUSE);
        player.sendMessage(Component.text("Purchased ", NamedTextColor.GRAY)
                .append(Component.text("Defuse Kit", NamedTextColor.GREEN, TextDecoration.BOLD)));
        player.closeInventory();
    }

    private boolean spendMoney(Player player, int amount) {
        int current = getMoney(player);
        if (current < amount) return false;

        money.put(player.getUuid(), current - amount);
        updateNametag(player);
        return true;
    }

    private boolean hasItem(Player player, java.util.function.Predicate<net.minestom.server.item.ItemStack> matcher) {
        for (int slot = 0; slot < 46; slot++) {
            if (matcher.test(player.getInventory().getItemStack(slot))) {
                return true;
            }
        }

        return false;
    }
}
