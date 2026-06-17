package com.darksoldier1404.dphologram.gui;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.builder.itemstack.ItemStackBuilder;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ItemStackSerializer;
import com.darksoldier1404.dppc.utils.NBT;
import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.data.HologramLine;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class EditGui {

    public static final int CHANNEL_EDIT = 1;

    public static final int CHANNEL_LINE = 2;

    public static final int CHANNEL_BULK = 3;

    public static final int CHANNEL_ADDTYPE = 4;

    public static final String NBT_LINE = "dphol_line";

    private static final int LINES_PER_PAGE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_ADDLINE = 46;
    private static final int SLOT_BULK = 47;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_HELP = 51;
    private static final int SLOT_NEXT = 53;

    private static final int CONTENT_PREVIEW = 32;

    private EditGui() {
    }

    public static void open(DPHologram plugin, Player player, String holoName) {
        open(plugin, player, holoName, 0);
    }

    public static void open(DPHologram plugin, Player player, String holoName, int page) {
        Hologram h = plugin.data.get(holoName);
        if (h == null) {
            player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &cNo hologram named '" + holoName + "'."));
            return;
        }

        List<HologramLine> lines = h.getLines();
        int maxPage = lines.isEmpty() ? 0 : (lines.size() - 1) / LINES_PER_PAGE;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        String typeTag = h.isTextOnly() ? " &9[text]" : "";
        DInventory inv = new DInventory(
                ColorUtils.applyColor("&8Edit: " + holoName + typeTag + " &7(" + (page + 1) + "/" + (maxPage + 1) + ")"),
                54, plugin);
        inv.setChannel(CHANNEL_EDIT);
        inv.setObj(holoName + "#" + page);

        int start = page * LINES_PER_PAGE;
        int end = Math.min(lines.size(), start + LINES_PER_PAGE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inv.setItem(slot++, buildLineIcon(i, lines.get(i)));
        }

        if (page > 0) inv.setItem(SLOT_PREV, navButton("prev", Material.ARROW, "&a« Previous page"));
        if (page < maxPage) inv.setItem(SLOT_NEXT, navButton("next", Material.ARROW, "&aNext page »"));
        inv.setItem(SLOT_ADDLINE, navButton("addline", Material.NAME_TAG, "&aAdd a line",
                "&7Click, then type the line text in chat.",
                "&7Color codes supported; type &fcancel &7to abort."));
        inv.setItem(SLOT_PAGE, pageIndicator(page, maxPage, lines.size()));
        inv.setItem(SLOT_BULK, navButton("bulk", Material.ANVIL, "&6Edit ALL lines at once",
                "&7Apply offset/rotation/scale changes",
                "&7as a delta to every line, or set a",
                "&7uniform billboard/background."));
        inv.setItem(SLOT_HELP, buildFooter());
        inv.openInventory(player);
    }

    public static void openBulk(DPHologram plugin, Player player, String holoName) {
        Hologram h = plugin.data.get(holoName);
        if (h == null) {
            player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &cNo hologram named '" + holoName + "'."));
            return;
        }

        DInventory inv = new DInventory(
                ColorUtils.applyColor("&8Global settings: " + holoName), 54, plugin);
        inv.setChannel(CHANNEL_BULK);
        inv.setObj(holoName);

        buildGlobalIcons(inv, h);
        inv.setItem(4, bulkBanner(h.getLines().size()));
        inv.openInventory(player);
    }

    public static void openAddType(DPHologram plugin, Player player, String holoName, int page) {
        DInventory inv = new DInventory(
                ColorUtils.applyColor("&8Add line: choose a type"), 27, plugin);
        inv.setChannel(CHANNEL_ADDTYPE);
        inv.setObj(holoName + "#" + page);

        inv.setItem(11, navButton("type_text", Material.PAPER, "&fText line",
                "&7Type the text in chat."));
        inv.setItem(13, navButton("type_item", Material.ITEM_FRAME, "&bItem display",
                "&7Uses the item in your main hand."));
        inv.setItem(15, navButton("type_block", Material.GRASS_BLOCK, "&aBlock display",
                "&7Type a block material in chat."));
        inv.setItem(22, navButton("cancel", Material.BARRIER, "&cCancel"));
        inv.openInventory(player);
    }

    private static void buildGlobalIcons(DInventory inv, Hologram h) {
        Vector off = h.getGlobalOffset();
        inv.setItem(10, ctrl("offx", Material.RED_STAINED_GLASS,
                "&cGlobal Offset X", String.format("%.2f", off.getX()), true));
        inv.setItem(11, ctrl("offy", Material.LIME_STAINED_GLASS,
                "&aGlobal Offset Y", String.format("%.2f", off.getY()), true));
        inv.setItem(12, ctrl("offz", Material.LIGHT_BLUE_STAINED_GLASS,
                "&bGlobal Offset Z", String.format("%.2f", off.getZ()), true));

        inv.setItem(19, rotCtrl("yaw", Material.YELLOW_STAINED_GLASS,
                "&eGlobal Yaw (Y)", h.getGlobalYaw()));
        inv.setItem(20, rotCtrl("pitch", Material.PURPLE_STAINED_GLASS,
                "&5Global Pitch (X)", h.getGlobalPitch()));
        inv.setItem(21, rotCtrl("roll", Material.CYAN_STAINED_GLASS,
                "&3Global Roll (Z)", h.getGlobalRoll()));

        inv.setItem(13, spacingCtrl(String.format("%.2f", h.getLineSpacing()), false));

        inv.setItem(14, ctrl("scale", Material.NETHER_STAR,
                "&eGlobal Scale", String.format("%+.2f", h.getGlobalScale()), true));

        inv.setItem(15, ctrl("billboard", Material.COMPASS,
                "&dGlobal Billboard",
                h.getGlobalBillboard() == null ? "none (per-line)" : h.getGlobalBillboard().name(), false));

        inv.setItem(16, ctrl("bg", Material.BLACK_STAINED_GLASS_PANE,
                "&fGlobal Background",
                !h.isGlobalBgSet() ? "unset (per-line)" : (h.getGlobalBackground() == null ? "none" : h.getGlobalBackground()),
                false));

        inv.setItem(23, navButton("perplayer",
                h.isPerPlayer() ? Material.LIME_DYE : Material.GRAY_DYE,
                h.isPerPlayer() ? "&aPer-player: ON" : "&7Per-player: OFF",
                "&7Click to toggle.",
                "&7ON: a personal copy spawns per nearby",
                "&7player, so placeholders resolve per-viewer.",
                "&7OFF: one shared copy for everyone."));

        inv.setItem(48, simpleCtrl("back", Material.ARROW, "&7« Back to line list"));
        inv.setItem(50, simpleCtrl("close", Material.BARRIER, "&cClose"));
    }

    public static final String NBT_CTRL = "dphol_ctrl";

    public static final float SCALE_MIN = 0.1f;
    public static final float SCALE_MAX = 10.0f;

    public static final float ROT_STEP = 15.0f;
    public static final float ROT_STEP_SHIFT = 90.0f;

    public static final double SPACING_STEP = 0.05;
    public static final double SPACING_STEP_SHIFT = 0.25;

    public static final String[] BG_PRESETS = {null, "#80000000", "#80FFFFFF"};

    public static void openLineDetail(DPHologram plugin, Player player, String holoName, int lineIndex) {
        Hologram h = plugin.data.get(holoName);
        if (h == null) {
            player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &cNo hologram named '" + holoName + "'."));
            return;
        }
        List<HologramLine> lines = h.getLines();
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            player.sendMessage(ColorUtils.applyColor(plugin.prefix + " &cLine " + lineIndex + " no longer exists."));
            return;
        }

        DInventory inv = new DInventory(
                ColorUtils.applyColor("&8Line " + lineIndex + " of " + holoName), 54, plugin);
        inv.setChannel(CHANNEL_LINE);
        inv.setObj(holoName + "#" + lineIndex);

        buildDetailIcons(inv, lines.get(lineIndex));
        inv.openInventory(player);
    }

    private static void buildDetailIcons(DInventory inv, HologramLine line) {
        Vector off = line.getOffset();

        inv.setItem(10, ctrl("offx", Material.RED_STAINED_GLASS,
                "&cOffset X", String.format("%.2f", off.getX()), true));
        inv.setItem(11, ctrl("offy", Material.LIME_STAINED_GLASS,
                "&aOffset Y", String.format("%.2f", off.getY()), true));
        inv.setItem(12, ctrl("offz", Material.LIGHT_BLUE_STAINED_GLASS,
                "&bOffset Z", String.format("%.2f", off.getZ()), true));

        inv.setItem(19, rotCtrl("yaw", Material.YELLOW_STAINED_GLASS,
                "&eYaw (Y)", line.getYaw()));
        inv.setItem(20, rotCtrl("pitch", Material.PURPLE_STAINED_GLASS,
                "&5Pitch (X)", line.getPitch()));
        inv.setItem(21, rotCtrl("roll", Material.CYAN_STAINED_GLASS,
                "&3Roll (Z)", line.getRoll()));

        inv.setItem(13, spacingCtrl(line.hasSpacing()
                ? String.format("%.2f", line.getSpacing()) : "inherit (global)", true));

        inv.setItem(14, ctrl("scale", Material.NETHER_STAR,
                "&eScale", String.format("%.2f", line.getScale()), true));

        inv.setItem(15, ctrl("billboard", Material.COMPASS,
                "&dBillboard", String.valueOf(line.getBillboard()), false));

        String bg = line.getBackground();
        inv.setItem(16, ctrl("bg", Material.BLACK_STAINED_GLASS_PANE,
                "&fBackground", bg == null ? "none" : bg, false));

        inv.setItem(48, simpleCtrl("back", Material.ARROW, "&7« Back to line list"));
        inv.setItem(50, simpleCtrl("close", Material.BARRIER, "&cClose"));
    }

    private static ItemStack ctrl(String ctrl, Material mat, String name, String current, boolean stepLore) {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.applyColor("&7Current: &f" + current));
        lore.add("");
        if (stepLore) {
            lore.add(ColorUtils.applyColor("&eLeft &7- increase"));
            lore.add(ColorUtils.applyColor("&eRight &7- decrease"));
            lore.add(ColorUtils.applyColor("&eShift &7- x10 step"));
        } else {
            lore.add(ColorUtils.applyColor("&eClick &7- cycle"));
        }
        ItemStack icon = ItemStackBuilder.of(mat)
                .name(ColorUtils.applyColor(name))
                .lore(lore)
                .build();
        return NBT.setStringTag(icon, NBT_CTRL, ctrl);
    }

    private static ItemStack rotCtrl(String ctrl, Material mat, String name, float current) {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.applyColor("&7Current: &f" + String.format("%.0f", current) + "°"));
        lore.add("");
        lore.add(ColorUtils.applyColor("&eLeft &7- +" + (int) ROT_STEP + "°"));
        lore.add(ColorUtils.applyColor("&eRight &7- -" + (int) ROT_STEP + "°"));
        lore.add(ColorUtils.applyColor("&eShift &7- " + (int) ROT_STEP_SHIFT + "° step"));
        ItemStack icon = ItemStackBuilder.of(mat)
                .name(ColorUtils.applyColor(name))
                .lore(lore)
                .build();
        return NBT.setStringTag(icon, NBT_CTRL, ctrl);
    }

    private static ItemStack spacingCtrl(String current, boolean showReset) {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.applyColor("&7Current: &f" + current));
        lore.add("");
        lore.add(ColorUtils.applyColor("&eLeft &7- +" + SPACING_STEP));
        lore.add(ColorUtils.applyColor("&eRight &7- -" + SPACING_STEP));
        lore.add(ColorUtils.applyColor("&eShift &7- " + SPACING_STEP_SHIFT + " step"));
        if (showReset) lore.add(ColorUtils.applyColor("&eMiddle &7- reset to global"));
        ItemStack icon = ItemStackBuilder.of(Material.LADDER)
                .name(ColorUtils.applyColor("&6Spacing"))
                .lore(lore)
                .build();
        return NBT.setStringTag(icon, NBT_CTRL, "spacing");
    }

    private static ItemStack simpleCtrl(String ctrl, Material mat, String name) {
        ItemStack icon = ItemStackBuilder.of(mat)
                .name(ColorUtils.applyColor(name))
                .build();
        return NBT.setStringTag(icon, NBT_CTRL, ctrl);
    }

    public static ItemStack buildLineIcon(int index, HologramLine line) {
        Material mat = iconMaterial(line);

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.applyColor("&7Content: &f" + preview(line.getContent())));
        Vector off = line.getOffset();
        lore.add(ColorUtils.applyColor(String.format("&7Offset: &f%.2f, %.2f, %.2f",
                off.getX(), off.getY(), off.getZ())));
        lore.add(ColorUtils.applyColor(String.format("&7Scale: &f%.2f", line.getScale())));
        lore.add(ColorUtils.applyColor("&7Spacing: &f" + (line.hasSpacing() ? String.format("%.2f", line.getSpacing()) : "global")));
        int clicks = line.getClickActions() == null ? 0 : line.getClickActions().size();
        lore.add(ColorUtils.applyColor("&7Click actions: &f" + clicks));
        lore.add("");
        lore.add(ColorUtils.applyColor("&eLeft &7- edit line"));
        lore.add(ColorUtils.applyColor("&eRight &7- remove line"));
        lore.add(ColorUtils.applyColor("&eShift+Left &7- move up"));
        lore.add(ColorUtils.applyColor("&eShift+Right &7- move down"));

        ItemStack icon = ItemStackBuilder.of(mat)
                .name(ColorUtils.applyColor("&fLine " + index + " &7(" + line.getType() + ")"))
                .lore(lore)
                .build();

        return NBT.setIntTag(icon, NBT_LINE, index);
    }

    private static ItemStack buildFooter() {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.applyColor("&7Left-click a line to edit it."));
        lore.add(ColorUtils.applyColor("&7Right-click to remove a line."));
        lore.add(ColorUtils.applyColor("&7Shift-click to reorder (up/down)."));
        lore.add(ColorUtils.applyColor("&7Use the arrows to change page."));
        return ItemStackBuilder.of(Material.BOOK)
                .name(ColorUtils.applyColor("&6Controls"))
                .lore(lore)
                .build();
    }

    private static ItemStack navButton(String ctrl, Material mat, String name, String... lore) {
        ItemStackBuilder b = ItemStackBuilder.of(mat).name(ColorUtils.applyColor(name));
        if (lore.length > 0) {
            List<String> l = new ArrayList<>();
            for (String s : lore) l.add(ColorUtils.applyColor(s));
            b.lore(l);
        }
        return NBT.setStringTag(b.build(), NBT_CTRL, ctrl);
    }

    private static ItemStack pageIndicator(int page, int maxPage, int lineCount) {
        return ItemStackBuilder.of(Material.PAPER)
                .name(ColorUtils.applyColor("&ePage " + (page + 1) + " / " + (maxPage + 1)))
                .lore(ColorUtils.applyColor("&7" + lineCount + " line(s) total"))
                .build();
    }

    private static ItemStack bulkBanner(int lineCount) {
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.applyColor("&7Hologram-wide settings, applied on top of"));
        lore.add(ColorUtils.applyColor("&7all &f" + lineCount + "&7 lines &7and any added later."));
        lore.add(ColorUtils.applyColor("&7Offset / rotation / scale: &fadded as a delta"));
        lore.add(ColorUtils.applyColor("&7Billboard / background: &foverride (or unset)"));
        return ItemStackBuilder.of(Material.BEACON)
                .name(ColorUtils.applyColor("&6Global settings"))
                .lore(lore)
                .build();
    }

    private static Material iconMaterial(HologramLine line) {
        if (line.getType() == null) return Material.PAPER;
        switch (line.getType()) {
            case TEXT:
                return Material.PAPER;
            case ITEM:
                return resolveItemMaterial(line.getContent());
            case BLOCK:
                return resolveBlockMaterial(line.getContent());
            default:
                return Material.PAPER;
        }
    }

    private static Material resolveItemMaterial(String content) {
        if (content == null) return Material.PAPER;
        try {
            Material m = Material.valueOf(content.toUpperCase());
            if (m != Material.AIR && m.isItem()) return m;
        } catch (IllegalArgumentException notMaterial) {
            try {
                ItemStack is = ItemStackSerializer.deserialize(content);
                if (is != null && is.getType() != Material.AIR) return is.getType();
            } catch (Exception ignored) {

            }
        }
        return Material.PAPER;
    }

    private static Material resolveBlockMaterial(String content) {
        if (content == null) return Material.STONE;
        try {
            Material m = Material.valueOf(content.toUpperCase());
            return m.isBlock() ? m : Material.STONE;
        } catch (IllegalArgumentException invalid) {
            return Material.STONE;
        }
    }

    private static String preview(String content) {
        if (content == null) return "(empty)";
        if (content.length() <= CONTENT_PREVIEW) return content;
        return content.substring(0, CONTENT_PREVIEW) + "...";
    }
}
