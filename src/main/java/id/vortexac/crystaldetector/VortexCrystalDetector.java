package id.vortexac.crystaldetector;

import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VortexCrystalDetector extends JavaPlugin
        implements Listener {

    private final Map<UUID, PlayerStats> players =
            new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("VortexAC") == null
                || !getServer().getPluginManager()
                .isPluginEnabled("VortexAC")) {
            getLogger().severe("VortexAC tidak ditemukan!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("VortexCrystalDetector diagnostic enabled.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!debugEnabled()) return;

        Material main = player.getInventory()
                .getItemInMainHand().getType();

        Material off = player.getInventory()
                .getItemInOffHand().getType();

        if (main != Material.END_CRYSTAL
                && off != Material.END_CRYSTAL
                && event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        PlayerStats stats = getStats(player);
        stats.interactions++;

        log(player, "INTERACT"
                + " | Action=" + event.getAction()
                + " | Main=" + main
                + " | Off=" + off
                + " | Cancelled=" + event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnimation(PlayerAnimationEvent event) {
        if (!debugEnabled()) return;

        Player player = event.getPlayer();
        PlayerStats stats = getStats(player);

        stats.swings++;

        if (holdingCrystal(player)) {
            log(player, "SWING_WITH_CRYSTAL");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlotChange(PlayerItemHeldEvent event) {
        if (!debugEnabled()) return;

        Player player = event.getPlayer();
        PlayerStats stats = getStats(player);

        stats.slotChanges++;

        log(player, "SLOT_CHANGE"
                + " | From=" + event.getPreviousSlot()
                + " | To=" + event.getNewSlot()
                + " | NewItem=" + player.getInventory()
                    .getItem(event.getNewSlot()).getType());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!debugEnabled()) return;

        if (event.getBlockPlaced().getType()
                != Material.END_CRYSTAL) {
            return;
        }

        Player player = event.getPlayer();
        PlayerStats stats = getStats(player);

        stats.crystalPlacements++;

        log(player, "CRYSTAL_BLOCK_PLACE");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCrystalSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) return;
        if (!debugEnabled()) return;

        getLogger().info(
                "[CrystalDebug] CRYSTAL_SPAWN"
                + " | World=" + event.getLocation().getWorld().getName()
                + " | X=" + event.getLocation().getBlockX()
                + " | Y=" + event.getLocation().getBlockY()
                + " | Z=" + event.getLocation().getBlockZ()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCrystalAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof EnderCrystal)) return;
        if (!debugEnabled()) return;

        PlayerStats stats = getStats(player);
        stats.crystalAttacks++;

        log(player, "CRYSTAL_ATTACK"
                + " | Damage=" + event.getDamage()
                + " | Cancelled=" + event.isCancelled());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    private PlayerStats getStats(Player player) {
        return players.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new PlayerStats()
        );
    }

    private boolean holdingCrystal(Player player) {
        return player.getInventory().getItemInMainHand().getType()
                == Material.END_CRYSTAL
                || player.getInventory().getItemInOffHand().getType()
                == Material.END_CRYSTAL;
    }

    private boolean debugEnabled() {
        return getConfig().getBoolean("settings.debug", true);
    }

    private void log(Player player, String message) {
        getLogger().info(
                "[CrystalDebug] " + player.getName()
                + " | " + message
        );
    }

    private static final class PlayerStats {
        private int interactions;
        private int swings;
        private int slotChanges;
        private int crystalPlacements;
        private int crystalAttacks;
    }
        }
