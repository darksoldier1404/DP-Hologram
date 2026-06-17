package com.darksoldier1404.dphologram.listener;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.events.dinventory.DInventoryClickEvent;
import com.darksoldier1404.dppc.events.dinventory.DInventoryCloseEvent;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.command.HologramCommand;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.data.HologramLine;
import com.darksoldier1404.dphologram.data.LineType;
import com.darksoldier1404.dphologram.gui.EditGui;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiListener implements Listener {

    private final DPHologram plugin;

    private final Map<UUID, PendingInput> pendingAddLine = new ConcurrentHashMap<>();

    private static final class PendingInput {
        final String holoName;
        final int page;
        final LineType type;

        PendingInput(String holoName, int page, LineType type) {
            this.holoName = holoName;
            this.page = page;
            this.type = type;
        }
    }

    public GuiListener(DPHologram plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(DInventoryClickEvent e) {
        DInventory inv = e.getDInventory();
        if (!inv.isValidHandler(plugin)) return;

        if (inv.isValidChannel(EditGui.CHANNEL_EDIT)) {
            onEditClick(e, inv);
        } else if (inv.isValidChannel(EditGui.CHANNEL_LINE)) {
            onLineDetailClick(e, inv);
        } else if (inv.isValidChannel(EditGui.CHANNEL_BULK)) {
            onBulkClick(e, inv);
        } else if (inv.isValidChannel(EditGui.CHANNEL_ADDTYPE)) {
            onAddTypeClick(e, inv);
        }
    }

    private void onEditClick(DInventoryClickEvent e, DInventory inv) {
        e.setCancelled(true);

        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getWhoClicked().getInventory())) return;

        final String obj = (String) inv.getObj();
        if (obj == null) return;
        final String holoName = holoOf(obj);
        final int page = pageOf(obj);
        Hologram h = plugin.data.get(holoName);
        if (h == null) return;

        final Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        if (NBT.hasTagKey(clicked, EditGui.NBT_CTRL)) {
            if (!e.getClick().isLeftClick()) return;
            String ctrl = NBT.getStringTag(clicked, EditGui.NBT_CTRL);
            if (ctrl == null) return;
            switch (ctrl) {
                case "prev":
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, page - 1));
                    return;
                case "next":
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, page + 1));
                    return;
                case "bulk":
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.openBulk(plugin, player, holoName));
                    return;
                case "addline":
                    if (h.isTextOnly()) {

                        promptForText(player, holoName, page);
                    } else {

                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.openAddType(plugin, player, holoName, page));
                    }
                    return;
                default:
                    return;
            }
        }

        if (!NBT.hasTagKey(clicked, EditGui.NBT_LINE)) return;
        final int lineIndex = NBT.getIntegerTag(clicked, EditGui.NBT_LINE);

        List<HologramLine> lines = h.getLines();
        if (lineIndex < 0 || lineIndex >= lines.size()) return;

        ClickType click = e.getClick();

        if (click == ClickType.SHIFT_LEFT) {

            if (lineIndex > 0) {
                java.util.Collections.swap(lines, lineIndex, lineIndex - 1);
                saveAndRespawn(h, holoName);
            }
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, page));
        } else if (click == ClickType.SHIFT_RIGHT) {

            if (lineIndex < lines.size() - 1) {
                java.util.Collections.swap(lines, lineIndex, lineIndex + 1);
                saveAndRespawn(h, holoName);
            }
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, page));
        } else if (click == ClickType.RIGHT) {

            lines.remove(lineIndex);
            saveAndRespawn(h, holoName);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, page));
        } else if (click == ClickType.LEFT) {

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.openLineDetail(plugin, player, holoName, lineIndex));
        }
    }

    private void onLineDetailClick(DInventoryClickEvent e, DInventory inv) {
        e.setCancelled(true);

        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getWhoClicked().getInventory())) return;

        String obj = (String) inv.getObj();
        if (obj == null) return;
        int hash = obj.lastIndexOf('#');
        if (hash < 0) return;
        final String holoName = obj.substring(0, hash);
        final int lineIndex;
        try {
            lineIndex = Integer.parseInt(obj.substring(hash + 1));
        } catch (NumberFormatException invalid) {
            return;
        }

        Hologram h = plugin.data.get(holoName);
        if (h == null) return;
        List<HologramLine> lines = h.getLines();
        if (lineIndex < 0 || lineIndex >= lines.size()) return;
        HologramLine line = lines.get(lineIndex);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        if (!NBT.hasTagKey(clicked, EditGui.NBT_CTRL)) return;
        String ctrl = NBT.getStringTag(clicked, EditGui.NBT_CTRL);
        if (ctrl == null || ctrl.isEmpty()) return;

        final Player player = (Player) e.getWhoClicked();
        ClickType click = e.getClick();
        boolean shift = click.isShiftClick();
        boolean left = click.isLeftClick();
        boolean right = click.isRightClick();

        switch (ctrl) {
            case "back":
                if (!click.isLeftClick()) return;
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName));
                return;
            case "close":
                if (!click.isLeftClick()) return;
                player.closeInventory();
                return;
            case "offx":
            case "offy":
            case "offz":
            case "yaw":
            case "pitch":
            case "roll":
            case "scale": {
                if (!applyNumericControl(line, ctrl, shift, left, right)) return;
                break;
            }
            case "spacing": {
                if (click == ClickType.MIDDLE) {
                    line.clearSpacing();
                } else {
                    double step = shift ? EditGui.SPACING_STEP_SHIFT : EditGui.SPACING_STEP;
                    double delta = left ? step : (right ? -step : 0);
                    if (delta == 0) return;
                    double cur = line.hasSpacing() ? line.getSpacing() : h.getLineSpacing();
                    line.setSpacing(round2(cur + delta));
                }
                break;
            }
            case "billboard": {
                Display.Billboard[] vals = Display.Billboard.values();
                Display.Billboard cur = line.getBillboard();
                int next = 0;
                for (int i = 0; i < vals.length; i++) {
                    if (vals[i] == cur) {
                        next = (i + 1) % vals.length;
                        break;
                    }
                }
                line.setBillboard(vals[next]);
                break;
            }
            case "bg": {
                String cur = line.getBackground();
                String[] presets = EditGui.BG_PRESETS;
                int idx = -1;
                for (int i = 0; i < presets.length; i++) {
                    if ((presets[i] == null && cur == null) ||
                            (presets[i] != null && presets[i].equals(cur))) {
                        idx = i;
                        break;
                    }
                }
                line.setBackground(presets[(idx + 1) % presets.length]);
                break;
            }
            default:
                return;
        }

        saveAndRespawn(h, holoName);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.openLineDetail(plugin, player, holoName, lineIndex));
    }

    private void onBulkClick(DInventoryClickEvent e, DInventory inv) {
        e.setCancelled(true);
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getWhoClicked().getInventory())) return;

        final String holoName = (String) inv.getObj();
        if (holoName == null) return;
        Hologram h = plugin.data.get(holoName);
        if (h == null) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        if (!NBT.hasTagKey(clicked, EditGui.NBT_CTRL)) return;
        String ctrl = NBT.getStringTag(clicked, EditGui.NBT_CTRL);
        if (ctrl == null || ctrl.isEmpty()) return;

        final Player player = (Player) e.getWhoClicked();
        ClickType click = e.getClick();
        boolean shift = click.isShiftClick();
        boolean left = click.isLeftClick();
        boolean right = click.isRightClick();

        if (ctrl.equals("back")) {
            if (!left) return;
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, 0));
            return;
        }
        if (ctrl.equals("close")) {
            if (!left) return;
            player.closeInventory();
            return;
        }

        boolean changed;
        switch (ctrl) {
            case "offx":
            case "offy":
            case "offz":
            case "yaw":
            case "pitch":
            case "roll":
            case "scale":

                changed = applyGlobalNumericControl(h, ctrl, shift, left, right);
                break;
            case "spacing": {

                double step = shift ? EditGui.SPACING_STEP_SHIFT : EditGui.SPACING_STEP;
                double delta = left ? step : (right ? -step : 0);
                if (delta == 0) return;
                h.setLineSpacing(round2(h.getLineSpacing() + delta));
                changed = true;
                break;
            }
            case "billboard": {

                Display.Billboard[] vals = Display.Billboard.values();
                Display.Billboard cur = h.getGlobalBillboard();
                if (cur == null) {
                    h.setGlobalBillboard(vals[0]);
                } else {
                    int i = 0;
                    for (int k = 0; k < vals.length; k++)
                        if (vals[k] == cur) {
                            i = k;
                            break;
                        }
                    h.setGlobalBillboard(i == vals.length - 1 ? null : vals[i + 1]);
                }
                changed = true;
                break;
            }
            case "bg": {

                String[] presets = EditGui.BG_PRESETS;
                if (!h.isGlobalBgSet()) {
                    h.setGlobalBackground(presets[0]);
                } else {
                    String cur = h.getGlobalBackground();
                    int idx = -1;
                    for (int i = 0; i < presets.length; i++) {
                        if ((presets[i] == null && cur == null) ||
                                (presets[i] != null && presets[i].equals(cur))) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx == presets.length - 1) h.clearGlobalBackground();
                    else h.setGlobalBackground(presets[idx + 1]);
                }
                changed = true;
                break;
            }
            case "perplayer": {

                if (!left) return;
                h.setPerPlayer(!h.isPerPlayer());
                changed = true;
                break;
            }
            default:
                return;
        }

        if (!changed) return;
        saveAndRespawn(h, holoName);
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.openBulk(plugin, player, holoName));
    }

    private void onAddTypeClick(DInventoryClickEvent e, DInventory inv) {
        e.setCancelled(true);
        if (e.getClickedInventory() != null && e.getClickedInventory().equals(e.getWhoClicked().getInventory())) return;

        final String obj = (String) inv.getObj();
        if (obj == null) return;
        final String holoName = holoOf(obj);
        final int page = pageOf(obj);
        Hologram h = plugin.data.get(holoName);
        if (h == null) return;

        final Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        if (!NBT.hasTagKey(clicked, EditGui.NBT_CTRL)) return;
        if (!e.getClick().isLeftClick()) return;
        String ctrl = NBT.getStringTag(clicked, EditGui.NBT_CTRL);
        if (ctrl == null) return;

        switch (ctrl) {
            case "cancel":
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, page));
                return;
            case "type_text":
                promptForText(player, holoName, page);
                return;
            case "type_block":
                pendingAddLine.put(player.getUniqueId(), new PendingInput(holoName, page, LineType.BLOCK));
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();
                    player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &eType a block material in chat (e.g. &fSTONE&e), or type &fcancel&e."));
                });
                return;
            case "type_item": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) {
                    player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &cHold an item in your main hand first."));
                    return;
                }
                h.getLines().add(new HologramLine(LineType.ITEM, HologramCommand.itemToContent(hand)));
                plugin.data.save(holoName);
                plugin.renderer.spawn(h);
                player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &aAdded item line " + (h.getLines().size() - 1) + " to '" + holoName + "'."));
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> EditGui.open(plugin, player, holoName, page));
                return;
            }
            default:
                return;
        }
    }

    private void promptForText(Player player, String holoName, int page) {
        pendingAddLine.put(player.getUniqueId(), new PendingInput(holoName, page, LineType.TEXT));
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &eType the new line text in chat (color codes supported), or type &fcancel&e."));
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        final Player player = e.getPlayer();
        final PendingInput pend = pendingAddLine.remove(player.getUniqueId());
        if (pend == null) return;
        e.setCancelled(true);
        final String text = e.getMessage();

        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            Hologram h = plugin.data.get(pend.holoName);
            if (h == null) {
                player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &cHologram '" + pend.holoName + "' no longer exists."));
                return;
            }
            if (text.equalsIgnoreCase("cancel")) {
                player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &7Add line cancelled."));
            } else if (pend.type == LineType.BLOCK) {
                Material mat;
                try {
                    mat = Material.valueOf(text.trim().toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    mat = null;
                }
                if (mat == null || !mat.isBlock()) {
                    player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &c'" + text + "' is not a valid block material. Add line cancelled."));
                } else {
                    h.getLines().add(new HologramLine(LineType.BLOCK, mat.name()));
                    plugin.data.save(pend.holoName);
                    plugin.renderer.spawn(h);
                    player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &aAdded block line " + (h.getLines().size() - 1) + " to '" + pend.holoName + "'."));
                }
            } else {
                h.getLines().add(new HologramLine(LineType.TEXT, text));
                plugin.data.save(pend.holoName);
                plugin.renderer.spawn(h);
                player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &aAdded line " + (h.getLines().size() - 1) + " to '" + pend.holoName + "'."));
            }
            EditGui.open(plugin, player, pend.holoName, pend.page);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pendingAddLine.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClose(DInventoryCloseEvent e) {
        DInventory inv = e.getDInventory();
        if (!inv.isValidHandler(plugin)) return;

        if (inv.isValidChannel(EditGui.CHANNEL_EDIT)) {

            String obj = (String) inv.getObj();
            if (obj == null) return;
            String holoName = holoOf(obj);
            if (plugin.data.containsKey(holoName)) {
                plugin.data.save(holoName);
            }
        } else if (inv.isValidChannel(EditGui.CHANNEL_BULK)) {
            String holoName = (String) inv.getObj();
            if (holoName != null && plugin.data.containsKey(holoName)) {
                plugin.data.save(holoName);
            }
        } else if (inv.isValidChannel(EditGui.CHANNEL_LINE)) {

            String obj = (String) inv.getObj();
            if (obj == null) return;
            int hash = obj.lastIndexOf('#');
            if (hash < 0) return;
            String holoName = obj.substring(0, hash);
            if (plugin.data.containsKey(holoName)) {
                plugin.data.save(holoName);
            }
        }
    }

    private void saveAndRespawn(Hologram h, String holoName) {
        plugin.data.save(holoName);
        plugin.renderer.spawn(h);
    }

    private static boolean applyNumericControl(HologramLine line, String ctrl, boolean shift, boolean left, boolean right) {
        switch (ctrl) {
            case "offx":
            case "offy":
            case "offz": {
                double step = shift ? 1.0 : 0.1;
                double delta = left ? step : (right ? -step : 0);
                if (delta == 0) return false;
                Vector off = line.getOffset().clone();
                if (ctrl.equals("offx")) off.setX(round2(off.getX() + delta));
                else if (ctrl.equals("offy")) off.setY(round2(off.getY() + delta));
                else off.setZ(round2(off.getZ() + delta));
                line.setOffset(off);
                return true;
            }
            case "yaw":
            case "pitch":
            case "roll": {
                float step = shift ? EditGui.ROT_STEP_SHIFT : EditGui.ROT_STEP;
                float delta = left ? step : (right ? -step : 0);
                if (delta == 0) return false;
                if (ctrl.equals("yaw")) line.setYaw(normalizeAngle(line.getYaw() + delta));
                else if (ctrl.equals("pitch")) line.setPitch(normalizeAngle(line.getPitch() + delta));
                else line.setRoll(normalizeAngle(line.getRoll() + delta));
                return true;
            }
            case "scale": {
                double step = shift ? 1.0 : 0.1;
                double delta = left ? step : (right ? -step : 0);
                if (delta == 0) return false;
                double val = line.getScale() + delta;
                val = Math.max(EditGui.SCALE_MIN, Math.min(EditGui.SCALE_MAX, val));
                line.setScale((float) round2(val));
                return true;
            }
            default:
                return false;
        }
    }

    private static boolean applyGlobalNumericControl(Hologram h, String ctrl, boolean shift, boolean left, boolean right) {
        switch (ctrl) {
            case "offx":
            case "offy":
            case "offz": {
                double step = shift ? 1.0 : 0.1;
                double delta = left ? step : (right ? -step : 0);
                if (delta == 0) return false;
                Vector g = h.getGlobalOffset().clone();
                if (ctrl.equals("offx")) g.setX(round2(g.getX() + delta));
                else if (ctrl.equals("offy")) g.setY(round2(g.getY() + delta));
                else g.setZ(round2(g.getZ() + delta));
                h.setGlobalOffset(g);
                return true;
            }
            case "yaw":
            case "pitch":
            case "roll": {
                float step = shift ? EditGui.ROT_STEP_SHIFT : EditGui.ROT_STEP;
                float delta = left ? step : (right ? -step : 0);
                if (delta == 0) return false;
                if (ctrl.equals("yaw")) h.setGlobalYaw(Hologram.wrapDegrees(h.getGlobalYaw() + delta));
                else if (ctrl.equals("pitch")) h.setGlobalPitch(Hologram.wrapDegrees(h.getGlobalPitch() + delta));
                else h.setGlobalRoll(Hologram.wrapDegrees(h.getGlobalRoll() + delta));
                return true;
            }
            case "scale": {
                double step = shift ? 1.0 : 0.1;
                double delta = left ? step : (right ? -step : 0);
                if (delta == 0) return false;
                float ns = (float) round2(h.getGlobalScale() + delta);
                if (ns < -10f) ns = -10f;
                if (ns > 10f) ns = 10f;
                h.setGlobalScale(ns);
                return true;
            }
            default:
                return false;
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static float normalizeAngle(float deg) {
        float a = deg % 360f;
        if (a >= 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }

    private static String holoOf(String obj) {
        int i = obj.lastIndexOf('#');
        return i < 0 ? obj : obj.substring(0, i);
    }

    private static int pageOf(String obj) {
        int i = obj.lastIndexOf('#');
        if (i < 0) return 0;
        try {
            return Integer.parseInt(obj.substring(i + 1));
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }
}
