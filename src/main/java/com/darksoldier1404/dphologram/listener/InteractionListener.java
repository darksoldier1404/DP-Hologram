package com.darksoldier1404.dphologram.listener;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.ClickDispatch;
import com.darksoldier1404.dphologram.data.ClickType;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.data.HologramLine;
import com.darksoldier1404.dphologram.util.EntityTags;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionListener implements Listener {

    private final DPHologram plugin;

    private final Map<String, Long> lastClick = new ConcurrentHashMap<>();

    private final Set<String> warnedMissing = ConcurrentHashMap.newKeySet();

    private final long cooldownMs;

    public InteractionListener(DPHologram plugin) {
        this.plugin = plugin;
        this.cooldownMs = plugin.config.getInt("default-click-cooldown", 0) * 50L;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {

        if (e.getHand() != EquipmentSlot.HAND) return;
        Entity clicked = e.getRightClicked();
        if (!(clicked instanceof Interaction)) return;
        if (!EntityTags.isOurs(clicked)) return;
        e.setCancelled(true);
        handle(e.getPlayer(), clicked, ClickType.RIGHT);
    }

    @EventHandler
    public void onLeftClick(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Entity victim = e.getEntity();
        if (!(victim instanceof Interaction)) return;
        if (!EntityTags.isOurs(victim)) return;
        e.setCancelled(true);
        handle((Player) e.getDamager(), victim, ClickType.LEFT);
    }

    private void handle(Player p, Entity it, ClickType click) {
        String holo = EntityTags.holoOf(it);
        Integer line = EntityTags.lineOf(it);
        if (holo == null) return;

        Hologram h = plugin.data.get(holo);
        if (h == null) return;
        if (line == null || line < 0 || line >= h.getLines().size()) return;

        if (!com.darksoldier1404.dphologram.render.VisibilityTask.canSee(plugin, p, h)) return;

        HologramLine hl = h.getLines().get(line);

        List<String> refs = ClickDispatch.match(hl, click);
        if (refs.isEmpty()) return;

        String key = p.getUniqueId() + "/" + holo + "/" + line;
        long now = System.currentTimeMillis();
        if (cooldownMs > 0) {
            Long last = lastClick.get(key);
            if (last != null && now - last < cooldownMs) return;
        }

        boolean dispatchedAny = false;
        Map<String, ActionBuilder> actions = DPPCore.getActions();
        for (String ref : refs) {
            ActionBuilder ab = actions == null ? null : actions.get(ref);
            if (ab == null) {
                if (warnedMissing.add(ref)) {
                    plugin.getLogger().warning("Click action '" + ref + "' (hologram '" + holo
                            + "', line " + line + ") is not a registered ActionBuilder action; skipping.");
                }
                continue;
            }
            ab.execute(p);
            dispatchedAny = true;
        }

        if (dispatchedAny && cooldownMs > 0) {
            lastClick.put(key, now);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {

        String prefix = e.getPlayer().getUniqueId().toString();
        lastClick.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
