package com.darksoldier1404.dphologram.render;

import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.papi.PapiConditionEvaluator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VisibilityTask extends BukkitRunnable {

    private final DPHologram plugin;

    private static final Set<String> WARNED =
            Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private VisibilityTask(DPHologram plugin) {
        this.plugin = plugin;
    }

    public static void clearWarned() {
        WARNED.clear();
    }

    public static BukkitTask start(DPHologram plugin) {
        long interval = plugin.config.getInt("visibility-check-interval", 40);
        return new VisibilityTask(plugin).runTaskTimer(plugin, interval, interval);
    }

    @Override
    public void run() {
        if (plugin.renderer == null || plugin.data == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyFor(plugin, player);
        }
    }

    public static void applyFor(DPHologram plugin, Player player) {
        if (plugin.renderer == null || plugin.data == null) return;

        for (String name : new HashSet<>(plugin.renderer.getSpawned().keySet())) {
            Hologram h = plugin.data.get(name);
            if (h == null) continue;

            if (h.getViewPermission() == null && h.getViewCondition() == null) continue;

            List<UUID> ids = plugin.renderer.getSpawned().get(name);
            if (ids == null) continue;

            boolean visible = canSee(plugin, player, h);
            for (UUID id : ids) {
                Entity e = Bukkit.getEntity(id);
                if (e == null) continue;
                if (visible) {
                    player.showEntity(plugin, e);
                } else {
                    player.hideEntity(plugin, e);
                }
            }
        }
    }

    public static boolean canSee(DPHologram plugin, Player p, Hologram h) {
        if (h.getViewPermission() != null && !p.hasPermission(h.getViewPermission())) {
            return false;
        }
        if (h.getViewCondition() != null) {

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                try {
                    String parsed = PapiConditionEvaluator.evaluate(p, h.getViewCondition());
                    return parsed.equalsIgnoreCase("true")
                            || parsed.equalsIgnoreCase("yes")
                            || parsed.equals("1");
                } catch (Exception ex) {

                    if (WARNED.add(h.getName())) {
                        plugin.getLogger().warning("Failed to evaluate view condition for hologram '"
                                + h.getName() + "' (further warnings suppressed): " + ex.getMessage());
                    }
                    return true;
                }
            }

        }
        return true;
    }
}
