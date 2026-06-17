package com.darksoldier1404.dphologram.render;

import com.darksoldier1404.dppc.api.placeholder.PlaceholderUtils;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ItemStackSerializer;
import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.data.HologramLine;
import com.darksoldier1404.dphologram.util.EntityTags;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HologramRenderer {

    private final DPHologram plugin;
    private final Map<String, List<UUID>> spawned = new ConcurrentHashMap<>();

    private final Map<String, Map<UUID, List<UUID>>> perPlayer = new ConcurrentHashMap<>();
    private final java.util.Set<java.util.UUID> trackedSet = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public HologramRenderer(DPHologram plugin) {
        this.plugin = plugin;
    }

    public void spawn(Hologram h) {
        Location base = h.getLocation();
        if (base == null) return;
        despawn(h.getName());

        if (h.isPerPlayer()) return;

        List<UUID> ids = new ArrayList<>();
        if (h.isTextOnly()) {
            spawnTextOnly(h, base, ids, null);
        } else {
            spawnComplex(h, base, ids, null);
        }
        spawned.put(h.getName(), ids);
    }

    public void spawnFor(Hologram h, Player viewer) {
        Location base = h.getLocation();
        if (base == null) return;

        List<UUID> ids = new ArrayList<>();
        if (h.isTextOnly()) {
            spawnTextOnly(h, base, ids, viewer);
        } else {
            spawnComplex(h, base, ids, viewer);
        }
        if (ids.isEmpty()) return;

        for (UUID id : ids) {
            Entity e = Bukkit.getEntity(id);
            if (e == null) continue;
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(viewer.getUniqueId())) continue;
                other.hideEntity(plugin, e);
            }
        }
        perPlayer.computeIfAbsent(h.getName(), k -> new ConcurrentHashMap<>())
                .put(viewer.getUniqueId(), ids);
    }

    public void despawnFor(String name, UUID viewer) {
        Map<UUID, List<UUID>> byViewer = perPlayer.get(name);
        if (byViewer == null) return;
        List<UUID> ids = byViewer.remove(viewer);
        if (ids != null) {
            for (UUID id : ids) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
                trackedSet.remove(id);
            }
        }
        if (byViewer.isEmpty()) perPlayer.remove(name);
    }

    public void despawnAllForViewer(UUID viewer) {
        for (String name : new HashSet<>(perPlayer.keySet())) {
            despawnFor(name, viewer);
        }
    }

    public void hideForeignPerPlayer(Player viewer) {
        UUID vid = viewer.getUniqueId();
        for (Map<UUID, List<UUID>> byViewer : perPlayer.values()) {
            for (Map.Entry<UUID, List<UUID>> en : byViewer.entrySet()) {
                if (en.getKey().equals(vid)) continue;
                for (UUID id : en.getValue()) {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null) viewer.hideEntity(plugin, e);
                }
            }
        }
    }

    public java.util.Set<UUID> getPerPlayerViewers(String name) {
        Map<UUID, List<UUID>> byViewer = perPlayer.get(name);
        if (byViewer == null) return java.util.Collections.emptySet();
        return new HashSet<>(byViewer.keySet());
    }

    public Map<String, Map<UUID, List<UUID>>> getPerPlayer() {
        return perPlayer;
    }

    String renderContent(String content, Player viewer) {
        if (content == null) return "";
        String c = content;

        if (viewer != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                c = PlaceholderUtils.applyPlaceholder(viewer, content);
            } catch (Exception ignored) {
                c = content;
            }
        }
        return ColorUtils.applyColor(c);
    }

    private void spawnComplex(Hologram h, Location base, List<UUID> ids, Player viewer) {
        double stackY = 0;
        for (int i = 0; i < h.getLines().size(); i++) {
            try {
                HologramLine line = h.getLines().get(i);
                if (i > 0) stackY -= h.effectiveSpacing(h.getLines().get(i - 1));
                Location pos = base.clone()
                        .add(line.computeOffset(stackY))
                        .add(h.getGlobalOffset());

                Display disp = spawnDisplay(pos, line, h, viewer);
                if (disp != null) {
                    disp.setPersistent(false);
                    applyBrightness(disp, h, line);
                    EntityTags.tag(disp, h.getName(), i);
                    ids.add(disp.getUniqueId());
                    trackedSet.add(disp.getUniqueId());
                }

                if (!line.getClickActions().isEmpty() && pos.getWorld() != null) {
                    Interaction it = pos.getWorld().spawn(pos, Interaction.class, (org.bukkit.util.Consumer<Interaction>) in -> {
                        in.setPersistent(false);
                        in.setInteractionWidth(0.5f);
                        in.setInteractionHeight(0.5f);
                    });
                    EntityTags.tag(it, h.getName(), i);
                    ids.add(it.getUniqueId());
                    trackedSet.add(it.getUniqueId());
                }
            } catch (Exception ex) {

                for (UUID id : ids) {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null) e.remove();
                    trackedSet.remove(id);
                }
                ids.clear();
                plugin.getLogger().severe("Failed to spawn hologram '" + h.getName()
                        + "' line " + i + ": " + ex.getMessage());
                return;
            }
        }
    }

    private void spawnTextOnly(Hologram h, Location base, List<UUID> ids, Player viewer) {
        if (h.getLines().isEmpty() || base.getWorld() == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < h.getLines().size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(renderContent(h.getLines().get(i).getContent(), viewer));
        }
        final String text = sb.toString();

        final HologramLine ref = new HologramLine();
        Location pos = base.clone().add(h.getGlobalOffset());

        try {
            TextDisplay d = pos.getWorld().spawn(pos, TextDisplay.class, (org.bukkit.util.Consumer<TextDisplay>) dd -> {
                dd.setText(text);
                dd.setBillboard(h.effectiveBillboard(ref));
                applyTransform(dd, ref, h);
                String bg = h.effectiveBackground(ref);
                if (bg != null) {
                    Color c = parseArgb(bg);
                    if (c != null) dd.setBackgroundColor(c);
                }
            });
            d.setPersistent(false);
            applyBrightness(d, h, ref);
            EntityTags.tag(d, h.getName(), 0);
            ids.add(d.getUniqueId());
            trackedSet.add(d.getUniqueId());
        } catch (Exception ex) {
            for (UUID id : ids) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
                trackedSet.remove(id);
            }
            ids.clear();
            plugin.getLogger().severe("Failed to spawn text-only hologram '" + h.getName() + "': " + ex.getMessage());
        }
    }

    private Display spawnDisplay(Location pos, HologramLine line, Hologram h, Player viewer) {
        if (pos.getWorld() == null) return null;
        switch (line.getType()) {
            case TEXT:
                return pos.getWorld().spawn(pos, TextDisplay.class, (org.bukkit.util.Consumer<TextDisplay>) d -> {
                    d.setText(renderContent(line.getContent(), viewer));
                    d.setBillboard(h.effectiveBillboard(line));
                    applyTransform(d, line, h);
                    String bg = h.effectiveBackground(line);
                    if (bg != null) {
                        Color c = parseArgb(bg);
                        if (c != null) d.setBackgroundColor(c);
                    }
                });
            case ITEM:
                return pos.getWorld().spawn(pos, ItemDisplay.class, (org.bukkit.util.Consumer<ItemDisplay>) d -> {
                    d.setBillboard(h.effectiveBillboard(line));
                    d.setItemStack(resolveItem(line.getContent()));
                    applyTransform(d, line, h);
                });
            case BLOCK:
                return pos.getWorld().spawn(pos, BlockDisplay.class, (org.bukkit.util.Consumer<BlockDisplay>) d -> {
                    d.setBlock(resolveBlock(line.getContent()).createBlockData());
                    applyTransform(d, line, h);
                });
            default:
                return null;
        }
    }

    private void applyBrightness(Display d, Hologram h, HologramLine line) {
        if (h.hasGlobalBrightness()) {
            d.setBrightness(new Display.Brightness(h.getGlobalBlockLight(), h.getGlobalSkyLight()));
        } else if (line.hasBrightness()) {
            d.setBrightness(new Display.Brightness(line.getBlockLight(), line.getSkyLight()));
        } else {
            d.setBrightness(null);
        }
    }

    private void applyTransform(Display d, HologramLine line, Hologram h) {
        Transformation t = d.getTransformation();
        float s = h.effectiveScale(line);
        org.joml.Quaternionf left = new org.joml.Quaternionf().rotationYXZ(
                (float) Math.toRadians(h.effectiveYaw(line)),
                (float) Math.toRadians(h.effectivePitch(line)),
                (float) Math.toRadians(h.effectiveRoll(line)));
        d.setTransformation(new Transformation(
                t.getTranslation(),
                left,
                new org.joml.Vector3f(s, s, s),
                new org.joml.Quaternionf()));
    }

    private ItemStack resolveItem(String content) {
        if (content == null) return new ItemStack(Material.STONE);
        try {
            Material m = Material.valueOf(content.toUpperCase());
            if (m == Material.AIR || !m.isItem()) return new ItemStack(Material.STONE);
            return new ItemStack(m);
        } catch (IllegalArgumentException notMaterial) {
            try {
                ItemStack is = ItemStackSerializer.deserialize(content);
                if (is != null) return is;
            } catch (Exception ignored) {

            }
            return new ItemStack(Material.STONE);
        }
    }

    private Material resolveBlock(String content) {
        if (content == null) return Material.STONE;
        try {
            Material m = Material.valueOf(content.toUpperCase());
            return m.isBlock() ? m : Material.STONE;
        } catch (IllegalArgumentException invalid) {
            return Material.STONE;
        }
    }

    private Color parseArgb(String s) {
        if (s == null) return null;
        String hex = s.startsWith("#") ? s.substring(1) : s;
        try {
            long argb = Long.parseLong(hex, 16);
            return Color.fromARGB((int) argb);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void despawn(String name) {
        List<UUID> ids = spawned.remove(name);
        if (ids != null) {
            for (UUID id : ids) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
                trackedSet.remove(id);
            }
        }

        Map<UUID, List<UUID>> byViewer = perPlayer.remove(name);
        if (byViewer != null) {
            for (List<UUID> vids : byViewer.values()) {
                for (UUID id : vids) {
                    Entity e = Bukkit.getEntity(id);
                    if (e != null) e.remove();
                    trackedSet.remove(id);
                }
            }
        }
    }

    public void despawnAll() {
        for (String name : new HashSet<>(spawned.keySet())) {
            despawn(name);
        }
        for (String name : new HashSet<>(perPlayer.keySet())) {
            despawn(name);
        }
        trackedSet.clear();
    }

    public Map<String, List<UUID>> getSpawned() {
        return spawned;
    }

    public boolean isTracked(UUID id) {
        return trackedSet.contains(id);
    }
}
