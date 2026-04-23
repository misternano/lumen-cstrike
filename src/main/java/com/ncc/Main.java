package com.ncc;

import com.ncc.commands.fun.*;
import com.ncc.commands.items.*;
import com.ncc.commands.match.*;
import com.ncc.commands.permissions.*;
import com.ncc.commands.utility.*;
import com.ncc.config.ServerConfig;
import com.ncc.game.GameManager;
import com.ncc.game.PlayerAbilityManager;
import com.ncc.game.items.ItemRegistry;
import com.ncc.listener.AbilityListeners;
import com.ncc.listener.BombListeners;
import com.ncc.listener.BuyMenuListeners;
import com.ncc.listener.ConnectionListeners;
import com.ncc.listener.FallDamageListeners;
import com.ncc.listener.GunListeners;
import com.ncc.listener.ServerListListeners;
import com.ncc.map.*;
import com.ncc.permissions.ChatFormatService;
import com.ncc.permissions.MoneyBelowNameService;
import com.ncc.permissions.NametagService;
import com.ncc.permissions.OperatorService;
import com.ncc.permissions.PermissionService;
import net.hollowcube.polar.PolarLoader;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.instance.InstanceContainer;

import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static ServerConfig config;
    public static InstanceContainer INSTANCE;
    public static GameMapConfig cfg;
    public static GameManager gameManager;
    public static PermissionService permissionService;
    public static ChatFormatService chatFormatService;
    public static NametagService nametagService;
    public static MoneyBelowNameService moneyBelowNameService;
    public static PlayerAbilityManager abilityManager;
    public static OperatorService operatorService;

    public static void main(String[] args) {

        // --- Server Initialization
        System.out.println("Starting CSTRIKE instance...");
        config = ServerConfig.load(Path.of("config", "server.json"));

        MinecraftServer server = MinecraftServer.init(
                new Auth.Online()
//                new Auth.Velocity("ubBS6I42IRUm0pQyDYVRaR3JIOZUySZKBGhHiI/OKRc=")
        );

        ItemRegistry.init();
        permissionService = new PermissionService(Path.of("config"));
        operatorService = new OperatorService(Path.of("config"));
        chatFormatService = new ChatFormatService(permissionService);
        nametagService = new NametagService(chatFormatService);
        moneyBelowNameService = new MoneyBelowNameService();
        abilityManager = new PlayerAbilityManager();

        // --- Map Loading
        MapLoader mapLoader = new MapLoader(config.maps.directoryPath().toFile());
        File map = mapLoader.getStartupMap(config.maps.selection, config.maps.startupMap);

        String mapName = map.getName().replace(".polar", "");

        System.out.println("Loading map: " + map.getName());

        INSTANCE = MinecraftServer.getInstanceManager().createInstanceContainer();

        try {
            INSTANCE.setChunkLoader(new PolarLoader(map.toPath()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load map: " + map.getName(), e);
        }

        File json = new File(map.getParent(), map.getName().replace(".polar", ".json"));
        cfg = MapConfigLoader.load(json);

        if (cfg == null) {
            throw new RuntimeException("Map config failed to load: " + json.getAbsolutePath());
        }

        MinecraftServer.getInstanceManager().registerInstance(INSTANCE);

        // --- Game Manager
        gameManager = new GameManager(cfg, INSTANCE, config.match, config.bomb);
        gameManager.switchMap(INSTANCE, cfg, mapName);

        registerCommands();

        // --- Event Handling
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        ConnectionListeners.register(events);
        BombListeners.register(events);
        GunListeners.register(events);
        BuyMenuListeners.register(events);
        ServerListListeners.register(events);
        AbilityListeners.register(events);
        FallDamageListeners.register(events);

        // --- Initialize instance
        server.start("0.0.0.0", 25566);
        startConsoleCommandLoop();
    }

    private static void registerCommands() {
        registerMatchCommands();
        registerItemCommands();
        registerPermissionCommands();
        registerUtilityCommands();
        registerFunCommands();
    }

    private static void registerMatchCommands() {
        MinecraftServer.getCommandManager().register(new MapCommand());
        MinecraftServer.getCommandManager().register(new MapsCommand());
        MinecraftServer.getCommandManager().register(new MatchCommand());
        MinecraftServer.getCommandManager().register(new TeamCommand());
    }

    private static void registerItemCommands() {
        MinecraftServer.getCommandManager().register(new MoneyCommand());
        MinecraftServer.getCommandManager().register(new GiveItemCommand());
        MinecraftServer.getCommandManager().register(new GiveWeaponCommand());
    }

    private static void registerPermissionCommands() {
        MinecraftServer.getCommandManager().register(new RankCommand());
        MinecraftServer.getCommandManager().register(new SetRankCommand());
        MinecraftServer.getCommandManager().register(new OpCommand());
        MinecraftServer.getCommandManager().register(new DeopCommand());
    }

    private static void registerUtilityCommands() {
        MinecraftServer.getCommandManager().register(new GamemodeCommand());
        MinecraftServer.getCommandManager().register(new TeleportCommand());
        MinecraftServer.getCommandManager().register(new TeleportSilentCommand());
    }

    private static void registerFunCommands() {
        MinecraftServer.getCommandManager().register(new FlyCommand());
        MinecraftServer.getCommandManager().register(new GodCommand());
        MinecraftServer.getCommandManager().register(new ForcefieldCommand());
        MinecraftServer.getCommandManager().register(new VanishCommand());
    }

    private static void startConsoleCommandLoop() {
        Thread consoleThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    MinecraftServer.getCommandManager().executeServerCommand(line);
                }
            } catch (Exception exception) {
                System.err.println("Console command loop stopped: " + exception.getMessage());
            }
        }, "console-command-thread");

        consoleThread.setDaemon(true);
        consoleThread.start();
    }
}
