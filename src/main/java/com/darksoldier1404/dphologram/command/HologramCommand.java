package com.darksoldier1404.dphologram.command;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.builder.command.ArgumentIndex;
import com.darksoldier1404.dppc.builder.command.ArgumentType;
import com.darksoldier1404.dppc.builder.command.CommandBuilder;
import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ItemStackSerializer;
import com.darksoldier1404.dphologram.DPHologram;
import com.darksoldier1404.dphologram.data.ClickAction;
import com.darksoldier1404.dphologram.data.ClickType;
import com.darksoldier1404.dphologram.data.Hologram;
import com.darksoldier1404.dphologram.data.HologramLine;
import com.darksoldier1404.dphologram.data.HologramType;
import com.darksoldier1404.dphologram.data.LineType;
import com.darksoldier1404.dphologram.gui.EditGui;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class HologramCommand {

    private static final String PERM = "dphologram.admin";

    private HologramCommand() {
    }

    public static void register(DPHologram plugin) {
        CommandBuilder b = new CommandBuilder(plugin);

        b.beginSubCommand("create", "/hologram create <name> [complex | textonly]")
                .withPermission(PERM)
                .playerOnly()
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING)
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.STRING, Arrays.asList("complex", "textonly"))
                .executesPlayer((player, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    if (name == null)
                        return msg(player, plugin, "&cUsage: /hologram create <name> [complex | textonly]");
                    if (plugin.data.containsKey(name))
                        return msg(player, plugin, "&cHologram '" + name + "' already exists.");
                    HologramType type = parseHoloType(args.getString(ArgumentIndex.ARG_1));
                    if (type == null) return msg(player, plugin, "&cType must be 'complex' or 'textonly'.");
                    Hologram h = new Hologram(name);
                    h.setType(type);
                    h.setLocation(player.getLocation());
                    h.setLineSpacing(plugin.config.getDouble("default-line-spacing", 0.25));
                    plugin.data.put(name, h);
                    plugin.data.save(name);
                    return msg(player, plugin, "&aCreated " + type.name().toLowerCase(Locale.ROOT) + " hologram '" + name + "'. Add lines with /hologram addline.");
                });

        b.beginSubCommand("settype", "/hologram settype <name> <complex | textonly>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.STRING, Arrays.asList("complex", "textonly"))
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    String ts = args.getString(ArgumentIndex.ARG_1);
                    if (ts == null)
                        return msg(sender, plugin, "&cUsage: /hologram settype <name> <complex | textonly>");
                    HologramType type = parseHoloType(ts);
                    if (type == null) return msg(sender, plugin, "&cType must be 'complex' or 'textonly'.");
                    h.setType(type);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    String note = type == HologramType.TEXT_ONLY ? " &7(lines merged into one multi-line text display)" : "";
                    return msg(sender, plugin, "&aSet type of '" + name + "' to " + type.name().toLowerCase(Locale.ROOT) + "." + note);
                });

        b.beginSubCommand("setperplayer", "/hologram setperplayer <name> <true | false>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.STRING, Arrays.asList("true", "false"))
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    String bs = args.getString(ArgumentIndex.ARG_1);
                    if (bs == null || !(bs.equalsIgnoreCase("true") || bs.equalsIgnoreCase("false"))) {
                        return msg(sender, plugin, "&cUsage: /hologram setperplayer <name> <true | false>");
                    }
                    boolean on = bs.equalsIgnoreCase("true");
                    h.setPerPlayer(on);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    String note = on
                            ? " &7(a personal copy spawns per nearby player; placeholders resolve per-viewer)"
                            : " &7(single shared copy for everyone)";
                    return msg(sender, plugin, "&aSet per-player of '" + name + "' to " + on + "." + note);
                });

        b.beginSubCommand("delete", "/hologram delete <name>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    plugin.renderer.despawn(name);
                    plugin.data.delete(name);
                    plugin.data.remove(name);
                    return msg(sender, plugin, "&aDeleted hologram '" + name + "'.");
                });

        b.beginSubCommand("list", "/hologram list")
                .withPermission(PERM)
                .executes((sender, args) -> {
                    if (plugin.data.isEmpty())
                        return msg(sender, plugin, "&7No holograms defined.");
                    msg(sender, plugin, "&eHolograms (" + plugin.data.size() + "): &f" + String.join(", ", plugin.data.keySet()));
                    return true;
                });

        b.beginSubCommand("tp", "/hologram tp <name>")
                .withPermission(PERM)
                .playerOnly()
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .executesPlayer((player, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, player, name);
                    if (h == null) return true;
                    Location loc = h.getLocation();
                    if (loc == null) return msg(player, plugin, "&cWorld '" + h.getWorldName() + "' is not loaded.");
                    player.teleport(loc);
                    return msg(player, plugin, "&aTeleported to hologram '" + name + "'.");
                });

        b.beginSubCommand("move", "/hologram move <name>")
                .withPermission(PERM)
                .playerOnly()
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .executesPlayer((player, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, player, name);
                    if (h == null) return true;
                    h.setLocation(player.getLocation());
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(player, plugin, "&aMoved hologram '" + name + "' to your location.");
                });

        b.beginSubCommand("addline", "/hologram addline <name> <text...>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.STRING_ARRAY)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    String[] words = args.getStringArray(ArgumentIndex.ARG_1);
                    if (words == null || words.length == 0)
                        return msg(sender, plugin, "&cUsage: /hologram addline <name> <text...>");
                    String text = String.join(" ", words);
                    h.getLines().add(new HologramLine(LineType.TEXT, text));
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aAdded text line to '" + name + "' (line " + (h.getLines().size() - 1) + ").");
                });

        b.beginSubCommand("setitem", "/hologram setitem <name> <line>")
                .withPermission(PERM)
                .playerOnly()
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .executesPlayer((player, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, player, name);
                    if (h == null) return true;
                    if (h.isTextOnly())
                        return msg(player, plugin, "&cText-only holograms support text lines only. Use /hologram settype " + name + " complex first.");
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, player, h, idx);
                    if (line == null) return true;
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand == null || hand.getType() == Material.AIR)
                        return msg(player, plugin, "&cHold an item in your main hand first.");
                    line.setType(LineType.ITEM);
                    line.setContent(itemToContent(hand));
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(player, plugin, "&aSet line " + idx + " of '" + name + "' to your held item.");
                });

        b.beginSubCommand("setblock", "/hologram setblock <name> <line> <material>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.MATERIAL)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    if (h.isTextOnly())
                        return msg(sender, plugin, "&cText-only holograms support text lines only. Use /hologram settype " + name + " complex first.");
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    Material mat = args.getMaterial(ArgumentIndex.ARG_2);
                    if (mat == null || !mat.isBlock())
                        return msg(sender, plugin, "&cThat material is not a placeable block.");
                    line.setType(LineType.BLOCK);
                    line.setContent(mat.name());
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet line " + idx + " of '" + name + "' to block " + mat.name() + ".");
                });

        b.beginSubCommand("removeline", "/hologram removeline <name> <line>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    if (lineAt(plugin, sender, h, idx) == null) return true;
                    h.getLines().remove((int) idx);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aRemoved line " + idx + " from '" + name + "'.");
                });

        b.beginSubCommand("setoffset", "/hologram setoffset <name> <line> <x> <y> <z>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_3, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_4, ArgumentType.DOUBLE)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    Double dx = args.getDouble(ArgumentIndex.ARG_2);
                    Double dy = args.getDouble(ArgumentIndex.ARG_3);
                    Double dz = args.getDouble(ArgumentIndex.ARG_4);
                    if (dx == null || dy == null || dz == null)
                        return msg(sender, plugin, "&cOffsets must be numbers.");
                    line.setOffset(new Vector(dx, dy, dz));
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet offset of line " + idx + " to (" + dx + ", " + dy + ", " + dz + ").");
                });

        b.beginSubCommand("setrotation", "/hologram setrotation <name> <line> <yaw> <pitch> <roll>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_3, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_4, ArgumentType.DOUBLE)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    Double yaw = args.getDouble(ArgumentIndex.ARG_2);
                    Double pitch = args.getDouble(ArgumentIndex.ARG_3);
                    Double roll = args.getDouble(ArgumentIndex.ARG_4);
                    if (yaw == null || pitch == null || roll == null)
                        return msg(sender, plugin, "&cRotation angles must be numbers (degrees).");
                    line.setYaw((float) (double) yaw);
                    line.setPitch((float) (double) pitch);
                    line.setRoll((float) (double) roll);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet rotation of line " + idx + " to yaw=" + yaw + ", pitch=" + pitch + ", roll=" + roll + " (degrees).");
                });

        b.beginSubCommand("setspacing", "/hologram setspacing <name> <line> <value | default>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.STRING, Arrays.asList("default", "0.25", "0.5"))
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    String val = args.getString(ArgumentIndex.ARG_2);
                    if (val == null)
                        return msg(sender, plugin, "&cUsage: /hologram setspacing <name> <line> <value | default>");
                    if (val.equalsIgnoreCase("default") || val.equalsIgnoreCase("global") || val.equalsIgnoreCase("reset")) {
                        line.clearSpacing();
                        plugin.data.save(name);
                        plugin.renderer.spawn(h);
                        return msg(sender, plugin, "&aLine " + idx + " of '" + name + "' now uses the global spacing.");
                    }
                    double sp;
                    try {
                        sp = Double.parseDouble(val);
                    } catch (NumberFormatException nfe) {
                        return msg(sender, plugin, "&cSpacing must be a number, or 'default' to inherit the global.");
                    }
                    line.setSpacing(sp);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet spacing below line " + idx + " of '" + name + "' to " + sp + ".");
                });

        b.beginSubCommand("setbillboard", "/hologram setbillboard <name> <line> <fixed | vertical | horizontal | center>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.STRING, billboardNames())
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    Billboard bb = parseBillboard(args.getString(ArgumentIndex.ARG_2));
                    if (bb == null)
                        return msg(sender, plugin, "&cBillboard must be one of: " + String.join(", ", billboardNames()) + ".");
                    line.setBillboard(bb);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet billboard of line " + idx + " of '" + name + "' to " + bb.name() + ".");
                });

        b.beginSubCommand("setbrightness", "/hologram setbrightness <name> <line> <block 0-15> <sky 0-15>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_3, ArgumentType.INTEGER)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    Integer block = args.getInteger(ArgumentIndex.ARG_2);
                    Integer sky = args.getInteger(ArgumentIndex.ARG_3);
                    if (block == null || sky == null)
                        return msg(sender, plugin, "&cUsage: /hologram setbrightness <name> <line> <block 0-15> <sky 0-15>");

                    if (block < 0 || sky < 0) {
                        line.clearBrightness();
                        plugin.data.save(name);
                        plugin.renderer.spawn(h);
                        return msg(sender, plugin, "&aCleared brightness override of line " + idx + " of '" + name + "' (follows world light).");
                    }
                    if (block > 15 || sky > 15)
                        return msg(sender, plugin, "&cBrightness values must be in the range 0-15 (or negative to clear).");
                    line.setBlockLight(block);
                    line.setSkyLight(sky);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet brightness of line " + idx + " of '" + name + "' to block=" + block + ", sky=" + sky + ".");
                });

        b.beginSubCommand("setalloffset", "/hologram setalloffset <name> <dx> <dy> <dz>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_3, ArgumentType.DOUBLE)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Double dx = args.getDouble(ArgumentIndex.ARG_1);
                    Double dy = args.getDouble(ArgumentIndex.ARG_2);
                    Double dz = args.getDouble(ArgumentIndex.ARG_3);
                    if (dx == null || dy == null || dz == null)
                        return msg(sender, plugin, "&cOffsets must be numbers.");
                    Vector g = h.getGlobalOffset();
                    h.setGlobalOffset(new Vector(g.getX() + dx, g.getY() + dy, g.getZ() + dz));
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    Vector now = h.getGlobalOffset();
                    return msg(sender, plugin, String.format("&aGlobal offset of '%s' is now (%.2f, %.2f, %.2f).", name, now.getX(), now.getY(), now.getZ()));
                });

        b.beginSubCommand("setallrotation", "/hologram setallrotation <name> <dyaw> <dpitch> <droll>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.DOUBLE)
                .withArgument(ArgumentIndex.ARG_3, ArgumentType.DOUBLE)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Double dyaw = args.getDouble(ArgumentIndex.ARG_1);
                    Double dpitch = args.getDouble(ArgumentIndex.ARG_2);
                    Double droll = args.getDouble(ArgumentIndex.ARG_3);
                    if (dyaw == null || dpitch == null || droll == null)
                        return msg(sender, plugin, "&cRotation angles must be numbers (degrees).");
                    h.setGlobalYaw(Hologram.wrapDegrees(h.getGlobalYaw() + (float) (double) dyaw));
                    h.setGlobalPitch(Hologram.wrapDegrees(h.getGlobalPitch() + (float) (double) dpitch));
                    h.setGlobalRoll(Hologram.wrapDegrees(h.getGlobalRoll() + (float) (double) droll));
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, String.format("&aGlobal rotation of '%s' is now yaw=%.0f, pitch=%.0f, roll=%.0f (degrees).", name, h.getGlobalYaw(), h.getGlobalPitch(), h.getGlobalRoll()));
                });

        b.beginSubCommand("setallscale", "/hologram setallscale <name> <delta>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.DOUBLE)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Double delta = args.getDouble(ArgumentIndex.ARG_1);
                    if (delta == null) return msg(sender, plugin, "&cScale delta must be a number.");
                    float ns = h.getGlobalScale() + (float) (double) delta;
                    if (ns < -10f) ns = -10f;
                    if (ns > 10f) ns = 10f;
                    h.setGlobalScale(ns);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, String.format("&aGlobal scale modifier of '%s' is now %+.2f (added to each line's scale).", name, h.getGlobalScale()));
                });

        b.beginSubCommand("setallspacing", "/hologram setallspacing <name> <value>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.DOUBLE)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Double value = args.getDouble(ArgumentIndex.ARG_1);
                    if (value == null) return msg(sender, plugin, "&cSpacing must be a number.");
                    h.setLineSpacing(value);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet global line spacing of '" + name + "' to " + value + " (used by lines without their own spacing).");
                });

        b.beginSubCommand("setallbillboard", "/hologram setallbillboard <name> <fixed | vertical | horizontal | center | none>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.STRING, billboardNamesWithNone())
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    String typeStr = args.getString(ArgumentIndex.ARG_1);
                    if (typeStr == null)
                        return msg(sender, plugin, "&cUsage: /hologram setallbillboard <name> <type | none>");
                    if (typeStr.equalsIgnoreCase("none")) {
                        h.setGlobalBillboard(null);
                        plugin.data.save(name);
                        plugin.renderer.spawn(h);
                        return msg(sender, plugin, "&aCleared global billboard of '" + name + "' (each line uses its own).");
                    }
                    Billboard bb = parseBillboard(typeStr);
                    if (bb == null)
                        return msg(sender, plugin, "&cBillboard must be one of: " + String.join(", ", billboardNames()) + ", or none.");
                    h.setGlobalBillboard(bb);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet global billboard of '" + name + "' to " + bb.name() + " (applies to every line).");
                });

        b.beginSubCommand("setallbrightness", "/hologram setallbrightness <name> <block 0-15> <sky 0-15>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.INTEGER)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer block = args.getInteger(ArgumentIndex.ARG_1);
                    Integer sky = args.getInteger(ArgumentIndex.ARG_2);
                    if (block == null || sky == null)
                        return msg(sender, plugin, "&cUsage: /hologram setallbrightness <name> <block 0-15> <sky 0-15>");
                    if (block < 0 || sky < 0) {
                        h.clearGlobalBrightness();
                        plugin.data.save(name);
                        plugin.renderer.spawn(h);
                        return msg(sender, plugin, "&aCleared global brightness of '" + name + "' (each line uses its own).");
                    }
                    if (block > 15 || sky > 15)
                        return msg(sender, plugin, "&cBrightness values must be in the range 0-15 (or negative to clear).");
                    h.setGlobalBrightness(block, sky);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aSet global brightness of '" + name + "' to block=" + block + ", sky=" + sky + " (applies to every line).");
                });

        b.beginSubCommand("resetglobal", "/hologram resetglobal <name>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    h.setGlobalOffset(new Vector(0, 0, 0));
                    h.setGlobalYaw(0);
                    h.setGlobalPitch(0);
                    h.setGlobalRoll(0);
                    h.setGlobalScale(0f);
                    h.setGlobalBillboard(null);
                    h.clearGlobalBackground();
                    h.clearGlobalBrightness();
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aReset all global settings of '" + name + "'.");
                });

        b.beginSubCommand("setperm", "/hologram setperm <name> <perm | none>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.STRING)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    String perm = args.getString(ArgumentIndex.ARG_1);
                    if (perm == null) return msg(sender, plugin, "&cUsage: /hologram setperm <name> <perm | none>");
                    if (perm.equalsIgnoreCase("none")) {
                        h.setViewPermission(null);
                        plugin.data.save(name);
                        return msg(sender, plugin, "&aCleared view permission for '" + name + "'.");
                    }
                    h.setViewPermission(perm);
                    plugin.data.save(name);
                    return msg(sender, plugin, "&aSet view permission of '" + name + "' to '" + perm + "'.");
                });

        b.beginSubCommand("setcondition", "/hologram setcondition <name> <papi | none>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.STRING)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    String cond = args.getString(ArgumentIndex.ARG_1);
                    if (cond == null)
                        return msg(sender, plugin, "&cUsage: /hologram setcondition <name> <papi | none>");
                    if (cond.equalsIgnoreCase("none")) {
                        h.setViewCondition(null);
                        plugin.data.save(name);
                        return msg(sender, plugin, "&aCleared view condition for '" + name + "'.");
                    }
                    h.setViewCondition(cond);
                    plugin.data.save(name);
                    return msg(sender, plugin, "&aSet view condition of '" + name + "' to '" + cond + "'.");
                });

        b.beginSubCommand("setclick", "/hologram setclick <name> <line> <left | right | any> <action>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.STRING, Arrays.asList("left", "right", "any"))
                .withArgument(ArgumentIndex.ARG_3, ArgumentType.STRING, DPPCore.actions.keySet())
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    String typeStr = args.getString(ArgumentIndex.ARG_2);
                    ClickType ct = parseClickType(typeStr);
                    if (ct == null) return msg(sender, plugin, "&cClick type must be left, right, or any.");
                    String action = args.getString(ArgumentIndex.ARG_3);
                    if (action == null)
                        return msg(sender, plugin, "&cUsage: /hologram setclick <name> <line> <left | right | any> <action>");
                    line.getClickActions().add(new ClickAction(ct, action));
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aAdded " + ct.name() + " click action '" + action + "' to line " + idx + ".");
                });

        b.beginSubCommand("removeclick", "/hologram removeclick <name> <line> <index>")
                .withPermission(PERM)
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .withArgument(ArgumentIndex.ARG_1, ArgumentType.INTEGER)
                .withArgument(ArgumentIndex.ARG_2, ArgumentType.INTEGER)
                .executes((sender, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    Hologram h = lookup(plugin, sender, name);
                    if (h == null) return true;
                    Integer idx = args.getInteger(ArgumentIndex.ARG_1);
                    HologramLine line = lineAt(plugin, sender, h, idx);
                    if (line == null) return true;
                    Integer ci = args.getInteger(ArgumentIndex.ARG_2);
                    List<ClickAction> actions = line.getClickActions();
                    if (actions.isEmpty())
                        return msg(sender, plugin, "&cThat line has no click actions.");
                    if (ci == null || ci < 0 || ci >= actions.size())
                        return msg(sender, plugin, "&cClick action index out of range (0.." + (actions.size() - 1) + ").");
                    actions.remove((int) ci);
                    plugin.data.save(name);
                    plugin.renderer.spawn(h);
                    return msg(sender, plugin, "&aRemoved click action " + ci + " from line " + idx + ".");
                });

        b.beginSubCommand("reload", "/hologram reload")
                .withPermission(PERM)
                .executes((sender, args) -> {

                    plugin.reload();
                    plugin.renderer.despawnAll();
                    plugin.data.loadAll(Hologram.class);
                    int spawned = 0;
                    for (Hologram h : plugin.data.values()) {
                        if (h.getWorldName() == null) continue;
                        World w = Bukkit.getWorld(h.getWorldName());
                        if (w == null) continue;
                        int cx = ((int) Math.floor(h.getX())) >> 4;
                        int cz = ((int) Math.floor(h.getZ())) >> 4;
                        if (w.isChunkLoaded(cx, cz)) {
                            plugin.renderer.spawn(h);
                            spawned++;
                        }
                    }
                    return msg(sender, plugin, "&aReloaded. " + plugin.data.size() + " holograms loaded, " + spawned + " rendered. &7(interval/cooldown changes need a full restart)");
                });

        b.beginSubCommand("edit", "/hologram edit <name>")
                .withPermission(PERM)
                .playerOnly()
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, plugin.data.keySet())
                .executesPlayer((player, args) -> {
                    String name = args.getString(ArgumentIndex.ARG_0);
                    if (lookup(plugin, player, name) == null) return true;
                    EditGui.open(plugin, player, name);
                    return true;
                });

        b.build("hologram");
    }

    private static Hologram lookup(DPHologram plugin, CommandSender sender, String name) {
        if (name == null) {
            msg(sender, plugin, "&cMissing hologram name.");
            return null;
        }
        Hologram h = plugin.data.get(name);
        if (h == null) msg(sender, plugin, "&cNo hologram named '" + name + "'.");
        return h;
    }

    private static HologramLine lineAt(DPHologram plugin, CommandSender sender, Hologram h, Integer idx) {
        if (h.getLines().isEmpty()) {
            msg(sender, plugin, "&cThat hologram has no lines.");
            return null;
        }
        if (idx == null || idx < 0 || idx >= h.getLines().size()) {
            int max = h.getLines().size() - 1;
            msg(sender, plugin, "&cLine index out of range (0.." + max + ").");
            return null;
        }
        return h.getLines().get(idx);
    }

    public static String itemToContent(ItemStack item) {
        if (!item.hasItemMeta()) return item.getType().name();
        ItemMeta meta = item.getItemMeta();
        boolean plain = meta == null
                || (!meta.hasDisplayName() && !meta.hasLore() && !meta.hasEnchants()
                && !meta.hasCustomModelData() && meta.getItemFlags().isEmpty());
        if (plain && item.getAmount() == 1) return item.getType().name();
        return ItemStackSerializer.serialize(item);
    }

    private static List<String> billboardNames() {
        List<String> names = new ArrayList<>();
        for (Billboard bb : Billboard.values()) names.add(bb.name().toLowerCase(Locale.ROOT));
        return names;
    }

    private static HologramType parseHoloType(String s) {
        if (s == null) return HologramType.COMPLEX;
        switch (s.toLowerCase(Locale.ROOT)) {
            case "complex":
                return HologramType.COMPLEX;
            case "textonly":
            case "text":
                return HologramType.TEXT_ONLY;
            default:
                return null;
        }
    }

    private static List<String> billboardNamesWithNone() {
        List<String> names = billboardNames();
        names.add("none");
        return names;
    }

    private static Billboard parseBillboard(String s) {
        if (s == null) return null;
        try {
            return Billboard.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static ClickType parseClickType(String s) {
        if (s == null) return null;
        switch (s.toLowerCase()) {
            case "left":
                return ClickType.LEFT;
            case "right":
                return ClickType.RIGHT;
            case "any":
                return ClickType.ANY;
            default:
                return null;
        }
    }

    private static boolean msg(CommandSender sender, DPHologram plugin, String text) {
        sender.sendMessage(ColorUtils.applyColor(plugin.prefix + " " + text));
        return true;
    }
}
