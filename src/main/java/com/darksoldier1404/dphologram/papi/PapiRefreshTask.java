package com.darksoldier1404.dphologram.papi;

import com.darksoldier1404.dppc.api.placeholder.PlaceholderUtils;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.data.HologramLine;
import com.darksoldier1404.dphologram.data.LineType;
import com.darksoldier1404.dphologram.util.EntityTags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PapiRefreshTask extends BukkitRunnable {

    private final DPHologram plugin;

    private final Set<String> warned = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private PapiRefreshTask(DPHologram plugin) {
        this.plugin = plugin;
    }

    public static BukkitTask start(DPHologram plugin) {
        long interval = plugin.config.getInt("refresh-interval", 20);
        return new PapiRefreshTask(plugin).runTaskTimer(plugin, interval, interval);
    }

    @Override
    public void run() {
        if (plugin.renderer == null || plugin.data == null) return;

        for (String name : new HashSet<>(plugin.renderer.getSpawned().keySet())) {
            Hologram h = plugin.data.get(name);
            if (h == null) continue;
            List<UUID> ids = plugin.renderer.getSpawned().get(name);
            if (ids == null) continue;
            refreshHologram(name, h, ids, null);
        }

        for (String name : new HashSet<>(plugin.renderer.getPerPlayer().keySet())) {
            Hologram h = plugin.data.get(name);
            if (h == null) continue;
            Map<UUID, List<UUID>> byViewer = plugin.renderer.getPerPlayer().get(name);
            if (byViewer == null) continue;
            for (UUID viewerId : new HashSet<>(byViewer.keySet())) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer == null) continue;
                List<UUID> ids = byViewer.get(viewerId);
                if (ids == null) continue;
                refreshHologram(name, h, ids, viewer);
            }
        }
    }

    private void refreshHologram(String name, Hologram h, List<UUID> ids, Player viewer) {
        if (h.isTextOnly()) {
            refreshTextOnly(name, h, ids, viewer);
            return;
        }
        List<HologramLine> lines = h.getLines();
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            if (line.getType() != LineType.TEXT) continue;
            String content = line.getContent();
            if (content == null) continue;
            refreshLine(name, i, content, ids, viewer);
        }
    }

    private void refreshTextOnly(String name, Hologram h, List<UUID> ids, Player viewer) {
        List<HologramLine> lines = h.getLines();
        try {
            TextDisplay target = null;
            for (UUID id : ids) {
                Entity e = Bukkit.getEntity(id);
                if (e instanceof TextDisplay) {
                    target = (TextDisplay) e;
                    break;
                }
            }
            if (target == null) return;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) sb.append('\n');
                String c = lines.get(i).getContent();

                String resolved = (c == null) ? "" : PlaceholderUtils.applyPlaceholder(viewer, c);
                sb.append(ColorUtils.applyColor(resolved));
            }
            target.setText(sb.toString());
        } catch (Exception ex) {
            String key = name + "/*";
            if (warned.add(key)) {
                plugin.getLogger().warning("Failed to refresh placeholders for text-only hologram '"
                        + name + "' (further warnings suppressed): " + ex.getMessage());
            }
        }
    }

    private void refreshLine(String name, int index, String content, List<UUID> ids, Player viewer) {
        try {

            TextDisplay target = null;
            for (UUID id : ids) {
                Entity e = Bukkit.getEntity(id);
                if (!(e instanceof TextDisplay)) continue;
                Integer ln = EntityTags.lineOf(e);
                if (ln != null && ln == index) {
                    target = (TextDisplay) e;
                    break;
                }
            }
            if (target == null) return;

            String resolved = PlaceholderUtils.applyPlaceholder(viewer, content);
            target.setText(ColorUtils.applyColor(resolved));
        } catch (Exception ex) {

            String key = name + "/" + index;
            if (warned.add(key)) {
                plugin.getLogger().warning("Failed to refresh placeholders for hologram '"
                        + name + "' line " + index + " (further warnings suppressed): " + ex.getMessage());
            }
        }
    }
}
