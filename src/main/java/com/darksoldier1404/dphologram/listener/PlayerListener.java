package com.darksoldier1404.dphologram.listener;

import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.render.VisibilityTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {

    private final DPHologram plugin;

    public PlayerListener(DPHologram plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        org.bukkit.entity.Player player = e.getPlayer();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisibilityTask.applyFor(plugin, player);

            if (plugin.renderer != null) plugin.renderer.hideForeignPerPlayer(player);
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {

        org.bukkit.entity.Player player = e.getPlayer();

        if (plugin.renderer != null) plugin.renderer.despawnAllForViewer(player.getUniqueId());
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            VisibilityTask.applyFor(plugin, player);
            if (plugin.renderer != null) plugin.renderer.hideForeignPerPlayer(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {

        if (plugin.renderer != null) plugin.renderer.despawnAllForViewer(e.getPlayer().getUniqueId());
    }
}
