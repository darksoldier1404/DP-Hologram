package com.darksoldier1404.dphologram.render;

import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PerPlayerTask extends BukkitRunnable {

    private final DPHologram plugin;
    private final double viewDistance;

    private PerPlayerTask(DPHologram plugin) {
        this.plugin = plugin;
        this.viewDistance = plugin.config.getDouble("per-player-view-distance", 64.0);
    }

    public static BukkitTask start(DPHologram plugin) {
        long interval = plugin.config.getInt("per-player-check-interval", 20);
        if (interval < 1) interval = 1;
        return new PerPlayerTask(plugin).runTaskTimer(plugin, interval, interval);
    }

    @Override
    public void run() {
        if (plugin.renderer == null || plugin.data == null) return;
        double maxSq = viewDistance * viewDistance;

        for (Hologram h : plugin.data.values()) {
            if (h == null || !h.isPerPlayer()) continue;

            Location base = h.getLocation();
            if (base == null) {
                clearAll(h.getName());
                continue;
            }
            World w = base.getWorld();
            if (w == null) {
                clearAll(h.getName());
                continue;
            }
            int cx = ((int) Math.floor(base.getX())) >> 4;
            int cz = ((int) Math.floor(base.getZ())) >> 4;
            if (!w.isChunkLoaded(cx, cz)) {
                clearAll(h.getName());
                continue;
            }

            Set<UUID> desired = new HashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld() != w) continue;
                if (p.getLocation().distanceSquared(base) > maxSq) continue;
                if (!VisibilityTask.canSee(plugin, p, h)) continue;
                desired.add(p.getUniqueId());
            }

            Set<UUID> current = plugin.renderer.getPerPlayerViewers(h.getName());

            for (UUID u : current) {
                if (!desired.contains(u)) plugin.renderer.despawnFor(h.getName(), u);
            }

            for (UUID u : desired) {
                if (current.contains(u)) continue;
                Player p = Bukkit.getPlayer(u);
                if (p != null) plugin.renderer.spawnFor(h, p);
            }
        }
    }

    private void clearAll(String name) {
        for (UUID u : plugin.renderer.getPerPlayerViewers(name)) {
            plugin.renderer.despawnFor(name, u);
        }
    }
}
