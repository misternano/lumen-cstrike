package com.ncc;

import com.ncc.commands.*;
import com.ncc.game.GameManager;
import com.ncc.game.TeamSide;
import com.ncc.game.bomb.BombManager;
import com.ncc.game.gun.GunManager;
import com.ncc.game.items.ItemRegistry;
import com.ncc.map.*;
import com.ncc.skin.Skins;
import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;

import java.io.File;
import java.util.Random;

public class Main {

    public static InstanceContainer INSTANCE;
    public static GameMapConfig cfg;
    public static GameManager gameManager;

    public static Team ctTeam;
    public static Team tTeam;

    public static final int MAX_PLAYERS = 10;
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {

        // --- Server Initialization
        System.out.println("Starting CSTRIKE instance...");

        MinecraftServer server = MinecraftServer.init(
//                new Auth.Velocity("ubBS6I42IRUm0pQyDYVRaR3JIOZUySZKBGhHiI/OKRc=")
        );

        ItemRegistry.init();

        // --- Map Loading
        MapLoader mapLoader = new MapLoader();
        File map = mapLoader.getRandomMap();

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

        // --- Team Creation & Styling
        ctTeam = MinecraftServer.getTeamManager().createTeam("CT");
        tTeam = MinecraftServer.getTeamManager().createTeam("T");

        ctTeam.setNameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS);
        tTeam.setNameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS);

        ctTeam.setPrefix(Component.text("[CT] ", NamedTextColor.BLUE));
        tTeam.setPrefix(Component.text("[T] ", NamedTextColor.RED));

        // --- Game Manager
        gameManager = new GameManager(cfg, INSTANCE);
        gameManager.switchMap(INSTANCE, cfg, mapName);

        // --- Command Registrar
        MinecraftServer.getCommandManager().register(new GamemodeCommand());
        MinecraftServer.getCommandManager().register(new MoneyCommand());
        MinecraftServer.getCommandManager().register(new MapCommand());
        MinecraftServer.getCommandManager().register(new MapsCommand());
        MinecraftServer.getCommandManager().register(new MatchCommand());
        MinecraftServer.getCommandManager().register(new GiveItemCommand());
        MinecraftServer.getCommandManager().register(new GiveWeaponCommand());
        MinecraftServer.getCommandManager().register(new TeamCommand());
        MinecraftServer.getCommandManager().register(new TeleportCommand());

        // --- Event Handling
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {

            Player p = event.getPlayer();

            if (INSTANCE.getPlayers().size() >= MAX_PLAYERS) {
                p.kick("Instance is full");
                return;
            }

            event.setSpawningInstance(INSTANCE);
            p.setGameMode(GameMode.ADVENTURE);
        });

        events.addListener(PlayerSpawnEvent.class, event -> {

            Player p = event.getPlayer();

            if (!event.isFirstSpawn()) return;

            var spawn = cfg.lobbySpawns.get(RANDOM.nextInt(cfg.lobbySpawns.size()));
            var pos = MapUtil.toPos(spawn);

            p.teleport(pos);
            p.setRespawnPoint(pos);

            gameManager.handleJoin(p);
        });

        events.addListener(PlayerSkinInitEvent.class, event -> {

            Player p = event.getPlayer();
            TeamSide team = gameManager.getTeam(p);

            if (team == null) {
                event.setSkin(Skins.FALLBACK);
                return;
            }

            event.setSkin(team == TeamSide.CT ? Skins.CT : Skins.T);
        });

        events.addListener(PlayerMoveEvent.class, event -> {

            Player p = event.getPlayer();
            GameManager gm = Main.gameManager;
            BombManager bm = gameManager.getBombManager();

            if (gm == null) return;

            var from = p.getPosition();
            var to = event.getNewPosition();

            if (gm.isFrozen()) {
                event.setNewPosition(from.withYaw(to.yaw()).withPitch(to.pitch()).withY(to.y()));
            }

            if (bm.isPlanting(p)) {
                if (event.getNewPosition().distance(p.getPosition()) > 0.1) {
                    bm.setPlanting(p, false, null);
                }
            }
        });

        events.addListener(PlayerTickEvent.class, event -> {

            Player p = event.getPlayer();
            BombManager bm = gameManager.getBombManager();
            GunManager gunManager = gameManager.getGunManager();

            if (ItemRegistry.isGun(p.getItemInOffHand())) {
                p.getInventory().setItemStack(PlayerInventoryUtils.OFFHAND_SLOT, ItemStack.AIR);
                p.getInventory().update(p);
            }
            
            // --- PLANTING (T side)
            if (gameManager.getTeam(p) == TeamSide.T) {

                boolean holdingBomb = ItemRegistry.isBomb(p.getItemInMainHand());

                String site = gameManager.getBombSiteAt(p);

                if (holdingBomb && site != null) {
                    bm.setPlanting(p, true, site);
                } else {
                    bm.setPlanting(p, false, null);
                }
            }

            // --- DEFUSING (CT side)
            if (gameManager.getTeam(p) == TeamSide.CT) {
                boolean sneaking = p.isSneaking();

                bm.setDefusing(p, sneaking);
            }

            if (!bm.isPlanting(p) && !bm.isDefusing(p)) {
                gunManager.tickPlayer(p);
            }
        });

        events.addListener(PlayerHandAnimationEvent.class, event -> {
            if (event.getHand() != net.minestom.server.entity.PlayerHand.MAIN) return;
            gameManager.getGunManager().tryShoot(event.getPlayer());
        });

        events.addListener(PlayerUseItemEvent.class, event -> {
            if (event.getHand() != net.minestom.server.entity.PlayerHand.MAIN) return;

            if (ItemRegistry.isBuyMenu(event.getItemStack())) {
                event.setCancelled(true);
                gameManager.openBuyMenu(event.getPlayer());
                return;
            }

            if (!ItemRegistry.isGun(event.getItemStack())) return;

            event.setCancelled(true);
            gameManager.getGunManager().tryShoot(event.getPlayer());
        });

        events.addListener(InventoryPreClickEvent.class, event -> {
            if (!gameManager.isBuyMenu(event.getInventory())) return;

            event.setCancelled(true);
            gameManager.handleBuyMenuClick(event.getPlayer(), event.getSlot());
        });

        events.addListener(PlayerSwapItemEvent.class, event -> {
            if (!ItemRegistry.isGun(event.getMainHandItem()) && !ItemRegistry.isGun(event.getOffHandItem())) return;

            event.setCancelled(true);
            if (ItemRegistry.isGun(event.getOffHandItem()) && !ItemRegistry.isGun(event.getMainHandItem())) {
                event.setMainHandItem(event.getOffHandItem());
            } else {
                event.setMainHandItem(event.getMainHandItem());
            }
            event.setOffHandItem(ItemStack.AIR);
            if (ItemRegistry.isGun(event.getMainHandItem())) {
                gameManager.getGunManager().tryReload(event.getPlayer());
            }
            MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
                event.getPlayer().getInventory().setItemStack(PlayerInventoryUtils.OFFHAND_SLOT, ItemStack.AIR);
                event.getPlayer().getInventory().update(event.getPlayer());
            });
        });

        // --- Initialize instance
        server.start("0.0.0.0", 25566);
    }
}
