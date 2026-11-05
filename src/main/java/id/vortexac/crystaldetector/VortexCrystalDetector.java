package id.vortexac.crystaldetector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VortexCrystalDetector extends JavaPlugin
        implements Listener {

    private final Map<UUID, PlayerData> players =
            new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("VortexAC") == null
                || !Bukkit.getPluginManager()
                .isPluginEnabled("VortexAC")) {

            getLogger().severe(
                    "VortexAC tidak ditemukan! Plugin dinonaktifkan."
            );

            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info(
                "VortexCrystalDetector berhasil diaktifkan."
        );

        getLogger().info(
                "Mode: Observation"
        );
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onCrystalUse(PlayerInteractEvent event) {

        if (!getConfig().getBoolean("settings.enabled", true)) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();

        if (isBypassed(player)) {
            return;
        }

        Material mainHand =
                player.getInventory().getItemInMainHand().getType();

        Material offHand =
                player.getInventory().getItemInOffHand().getType();

        if (mainHand != Material.END_CRYSTAL
                && offHand != Material.END_CRYSTAL) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block != null) {
            Material type = block.getType();

            if (type != Material.OBSIDIAN
                    && type != Material.BEDROCK) {
                return;
            }
        }

        PlayerData data = players.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new PlayerData()
        );

        data.lastCrystalUse = System.nanoTime();
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onCrystalAttack(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof EnderCrystal)) {
            return;
        }

        if (isBypassed(player)) {
            return;
        }

        PlayerData data = players.get(player.getUniqueId());

        if (data == null || data.lastCrystalUse == 0L) {
            return;
        }

        long now = System.nanoTime();

        long elapsed =
                (now - data.lastCrystalUse) / 1_000_000L;

        data.lastCrystalUse = 0L;

        long maxCycle = Math.max(
                1L,
                getConfig().getLong(
                        "detection.max-cycle-ms", 220L
                )
        );

        long window = Math.max(
                1000L,
                getConfig().getLong(
                        "detection.window-ms", 10000L
                )
        );

        data.cycles.addLast(now);

        prune(data.cycles, now, window);

        if (elapsed <= maxCycle) {
            data.fastCycles.addLast(now);
        }

        prune(data.fastCycles, now, window);

        int minimum = Math.max(
                1,
                getConfig().getInt(
                        "detection.minimum-cycles", 12
                )
        );

        int required = Math.max(
                1,
                getConfig().getInt(
                        "detection.required-fast-cycles", 10
                )
        );

        if (data.cycles.size() < minimum
                || data.fastCycles.size() < required) {
            return;
        }

        String details =
                "Crystal cycles: " + data.cycles.size()
                + " | Fast cycles: " + data.fastCycles.size()
                + " | Interval: " + elapsed + "ms";

        getLogger().warning(
                "[CrystalDetection] "
                + player.getName()
                + " | " + details
        );

        if (getConfig().getBoolean(
                "settings.auto-kick", false
        )) {

            String message = getConfig().getString(
                    "punishment.message",
                    "&cVortexAC &8» &fSuspicious crystal activity."
            );

            if (message == null || message.isBlank()) {
                message = "&cSuspicious crystal activity.";
            }

            player.kickPlayer(
                    ChatColor.translateAlternateColorCodes(
                            '&', message
                    )
            );
        }

        data.cycles.clear();
        data.fastCycles.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        players.remove(event.getPlayer().getUniqueId());
    }

    private boolean isBypassed(Player player) {

        String permission = getConfig().getString(
                "settings.bypass-permission",
                "vortexcrystal.bypass"
        );

        return permission != null
                && !permission.isBlank()
                && player.hasPermission(permission);
    }

    private void prune(
            Deque<Long> timestamps,
            long now,
            long windowMs
    ) {

        long windowNanos = windowMs * 1_000_000L;

        while (!timestamps.isEmpty()
                && now - timestamps.peekFirst() > windowNanos) {

            timestamps.removeFirst();
        }
    }

    private static final class PlayerData {

        private long lastCrystalUse;

        private final Deque<Long> cycles =
                new ArrayDeque<>();

        private final Deque<Long> fastCycles =
                new ArrayDeque<>();
    }
}
