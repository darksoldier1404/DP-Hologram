package com.darksoldier1404.dphologram.listener;

import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.render.HologramRenderer;
import com.darksoldier1404.dphologram.util.EntityTags;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkListener implements Listener {

    private final DPHologram plugin;
    private final HologramRenderer renderer;

    public ChunkListener(DPHologram plugin, HologramRenderer renderer) {
        this.plugin = plugin;
        this.renderer = renderer;
    }

    boolean inChunk(Hologram h, Chunk c) {
        if (h.getWorldName() == null || !h.getWorldName().equals(c.getWorld().getName())) return false;
        int cx = ((int) Math.floor(h.getX())) >> 4;
        int cz = ((int) Math.floor(h.getZ())) >> 4;
        return cx == c.getX() && cz == c.getZ();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();

        for (Hologram h : plugin.data.values()) {
            if (inChunk(h, chunk)) {
                renderer.spawn(h);
            }
        }

        for (Entity ent : chunk.getEntities()) {
            if (EntityTags.isOurs(ent) && !renderer.isTracked(ent.getUniqueId())) {
                ent.remove();
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        for (Hologram h : plugin.data.values()) {
            if (inChunk(h, chunk)) {
                renderer.despawn(h.getName());
            }
        }
    }
}
