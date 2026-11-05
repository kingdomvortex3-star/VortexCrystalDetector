package id.vortexac.crystaldetector;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VortexCrystalDetector extends JavaPlugin
        implements Listener {

    private final Map<UUID, Long> lastEvents =
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

        getLogger().info("Crystal diagnostic mode enabled.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!isDebugEnabled()) return;

        Action action = event.getAction();
        Material main = player.getInventory()
                .getItemInMainHand().getType();
        Material off = player.getInventory()
                .getItemInOffHand().getType();

        Block block = event.getClickedBlock();

        getLogger().info(
                "[CrystalDebug] INTERACT"
                + " | Player=" + player.getName()
                + " | Action=" + action
                + " | MainHand=" + main
                + " | OffHand=" + off
                + " | Block=" + (block == null
                    ? "none" : block.getType())
                + " | Cancelled=" + event.isCancelled()
        );

        if (main == Material.END_CRYSTAL
                || off == Material.END_CRYSTAL
                || action == Action.LEFT_CLICK_BLOCK
                || action == Action.LEFT_CLICK_AIR) {
            lastEvents.put(player.getUniqueId(), System.nanoTime());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof EnderCrystal)) return;

        long previous = lastEvents.getOrDefault(
                player.getUniqueId(), 0L
        );

        long elapsed = previous == 0L
                ? -1L
                : (System.nanoTime() - previous) / 1_000_000L;

        getLogger().info(
                "[CrystalDebug] CRYSTAL_ATTACK"
                + " | Player=" + player.getName()
                + " | PreviousInteraction=" + elapsed + "ms"
                + " | Cancelled=" + event.isCancelled()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent event) {
        if (!isDebugEnabled()) return;

        getLogger().info(
                "[CrystalDebug] SWING"
                + " | Player=" + event.getPlayer().getName()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastEvents.remove(event.getPlayer().getUniqueId());
    }

    private boolean isDebugEnabled() {
        return getConfig().getBoolean("settings.debug", true);
    }
                }
