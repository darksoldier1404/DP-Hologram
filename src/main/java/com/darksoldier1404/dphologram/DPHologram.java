package com.darksoldier1404.dphologram;

import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.data.DataContainer;
import com.darksoldier1404.dppc.data.DataType;
import com.darksoldier1404.dphologram.command.HologramCommand;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.listener.ChunkListener;
import com.darksoldier1404.dphologram.listener.GuiListener;
import com.darksoldier1404.dphologram.listener.InteractionListener;
import com.darksoldier1404.dphologram.listener.PlayerListener;
import com.darksoldier1404.dphologram.papi.PapiRefreshTask;
import com.darksoldier1404.dphologram.render.HologramRenderer;
import com.darksoldier1404.dphologram.render.PerPlayerTask;
import com.darksoldier1404.dphologram.render.VisibilityTask;
import com.darksoldier1404.dphologram.util.EntityTags;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class DPHologram extends DPlugin {
    public static DPHologram plugin;

    public DataContainer<String, Hologram> data;
    public HologramRenderer renderer;
    private BukkitTask papiTask;
    private BukkitTask visibilityTask;
    private BukkitTask perPlayerTask;

    public DPHologram() {
        super(false);
        plugin = this;
        init();
        EntityTags.init(this);
    }

    @Override
    public void onLoad() {
        data = loadDataContainer(new DataContainer<>(this, DataType.CUSTOM, "holograms"), Hologram.class);
        PluginUtil.addPlugin(plugin, 32047);
    }

    @Override
    public void onEnable() {
        renderer = new HologramRenderer(this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this, renderer), this);
        HologramCommand.register(this);

        for (Hologram h : data.values()) {
            if (h.getWorldName() == null) continue;
            org.bukkit.World w = Bukkit.getWorld(h.getWorldName());
            if (w == null) continue;
            int cx = ((int) Math.floor(h.getX())) >> 4;
            int cz = ((int) Math.floor(h.getZ())) >> 4;
            if (w.isChunkLoaded(cx, cz)) {
                renderer.spawn(h);
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papiTask = PapiRefreshTask.start(this);
        }

        VisibilityTask.clearWarned();
        visibilityTask = VisibilityTask.start(this);

        perPlayerTask = PerPlayerTask.start(this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

    }

    @Override
    public void onDisable() {
        if (papiTask != null) papiTask.cancel();
        if (visibilityTask != null) visibilityTask.cancel();
        if (perPlayerTask != null) perPlayerTask.cancel();
        if (renderer != null) renderer.despawnAll();
        saveAllData();
    }
}
