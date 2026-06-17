package com.darksoldier1404.dphologram.data;

import com.darksoldier1404.dppc.data.DataCargo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Hologram implements DataCargo {
    private String name;
    private HologramType type = HologramType.COMPLEX;
    private boolean perPlayer = false;
    private String world;
    private double x, y, z;
    private float yaw, pitch;
    private double lineSpacing = 0.25;
    private String viewPermission;
    private String viewCondition;
    private final List<HologramLine> lines = new ArrayList<>();

    private final Vector globalOffset = new Vector(0, 0, 0);
    private float globalYaw, globalPitch, globalRoll;
    private float globalScale = 0f;
    private Billboard globalBillboard;
    private boolean globalBgSet;
    private String globalBackground;
    private int globalBlockLight = -1, globalSkyLight = -1;

    public Hologram() {
    }

    public Hologram(String name) {
        this.name = name;
    }

    @Override
    public YamlConfiguration serialize() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name", name);
        cfg.set("type", type.name());
        cfg.set("perPlayer", perPlayer);
        cfg.set("loc.world", world);
        cfg.set("loc.x", x);
        cfg.set("loc.y", y);
        cfg.set("loc.z", z);
        cfg.set("loc.yaw", yaw);
        cfg.set("loc.pitch", pitch);
        cfg.set("lineSpacing", lineSpacing);
        cfg.set("viewPermission", viewPermission);
        cfg.set("viewCondition", viewCondition);
        cfg.set("global.offset", globalOffset.getX() + "," + globalOffset.getY() + "," + globalOffset.getZ());
        cfg.set("global.rotation", globalYaw + "," + globalPitch + "," + globalRoll);
        cfg.set("global.scale", globalScale);
        cfg.set("global.billboard", globalBillboard == null ? null : globalBillboard.name());
        if (globalBgSet) cfg.set("global.background", globalBackground == null ? "none" : globalBackground);
        if (hasGlobalBrightness()) {
            cfg.set("global.brightness.block", globalBlockLight);
            cfg.set("global.brightness.sky", globalSkyLight);
        }
        for (int i = 0; i < lines.size(); i++) {
            HologramLine l = lines.get(i);
            String p = "lines." + i + ".";
            cfg.set(p + "type", l.getType().name());
            cfg.set(p + "content", l.getContent());
            cfg.set(p + "offset", l.getOffset().getX() + "," + l.getOffset().getY() + "," + l.getOffset().getZ());
            if (l.hasRotation()) cfg.set(p + "rotation", l.getYaw() + "," + l.getPitch() + "," + l.getRoll());
            cfg.set(p + "scale", l.getScale());
            if (l.hasSpacing()) cfg.set(p + "spacing", l.getSpacing());
            cfg.set(p + "billboard", l.getBillboard().name());
            cfg.set(p + "background", l.getBackground());
            if (l.hasBrightness()) {
                cfg.set(p + "brightness.block", l.getBlockLight());
                cfg.set(p + "brightness.sky", l.getSkyLight());
            }
            for (int c = 0; c < l.getClickActions().size(); c++) {
                ClickAction ca = l.getClickActions().get(c);
                cfg.set(p + "click." + c + ".type", ca.getClickType().name());
                cfg.set(p + "click." + c + ".action", ca.getActionRef());
            }
        }
        return cfg;
    }

    @Override
    public Hologram deserialize(YamlConfiguration data) {
        this.name = data.getString("name");
        this.type = parseType(data.getString("type"));
        this.perPlayer = data.getBoolean("perPlayer", false);
        this.world = data.getString("loc.world");
        this.x = data.getDouble("loc.x");
        this.y = data.getDouble("loc.y");
        this.z = data.getDouble("loc.z");
        this.yaw = (float) data.getDouble("loc.yaw");
        this.pitch = (float) data.getDouble("loc.pitch");
        this.lineSpacing = data.getDouble("lineSpacing", 0.25);
        this.viewPermission = data.getString("viewPermission");
        this.viewCondition = data.getString("viewCondition");
        Vector gOff = parseVector(data.getString("global.offset", "0,0,0"));
        this.globalOffset.setX(gOff.getX());
        this.globalOffset.setY(gOff.getY());
        this.globalOffset.setZ(gOff.getZ());
        Vector gRot = parseVector(data.getString("global.rotation", "0,0,0"));
        this.globalYaw = (float) gRot.getX();
        this.globalPitch = (float) gRot.getY();
        this.globalRoll = (float) gRot.getZ();
        this.globalScale = (float) data.getDouble("global.scale", 0.0);
        this.globalBillboard = data.isSet("global.billboard") ? parseBillboard(data.getString("global.billboard")) : null;
        if (data.isSet("global.background")) {
            this.globalBgSet = true;
            String gbg = data.getString("global.background");
            this.globalBackground = "none".equalsIgnoreCase(gbg) ? null : gbg;
        } else {
            this.globalBgSet = false;
            this.globalBackground = null;
        }
        if (data.isSet("global.brightness.block") && data.isSet("global.brightness.sky")) {
            this.globalBlockLight = clampLight(data.getInt("global.brightness.block", -1));
            this.globalSkyLight = clampLight(data.getInt("global.brightness.sky", -1));
        } else {
            this.globalBlockLight = -1;
            this.globalSkyLight = -1;
        }
        lines.clear();
        ConfigurationSection ls = data.getConfigurationSection("lines");
        if (ls != null) {
            List<String> keys = new ArrayList<>(ls.getKeys(false));
            keys.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
            for (String k : keys) {
                String p = "lines." + k + ".";
                HologramLine l = new HologramLine(parseLineType(data.getString(p + "type")), data.getString(p + "content"));
                l.setOffset(parseVector(data.getString(p + "offset", "0,0,0")));
                Vector rot = parseVector(data.getString(p + "rotation", "0,0,0"));
                l.setYaw((float) rot.getX());
                l.setPitch((float) rot.getY());
                l.setRoll((float) rot.getZ());
                l.setScale((float) data.getDouble(p + "scale", 1.0));
                if (data.isSet(p + "spacing")) l.setSpacing(data.getDouble(p + "spacing"));
                l.setBillboard(parseBillboard(data.getString(p + "billboard", "CENTER")));
                l.setBackground(data.getString(p + "background"));
                if (data.isSet(p + "brightness.block") && data.isSet(p + "brightness.sky")) {
                    l.setBlockLight(clampLight(data.getInt(p + "brightness.block", -1)));
                    l.setSkyLight(clampLight(data.getInt(p + "brightness.sky", -1)));
                }
                ConfigurationSection cs = data.getConfigurationSection(p + "click");
                if (cs != null) {
                    List<String> ck = new ArrayList<>(cs.getKeys(false));
                    ck.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
                    for (String c : ck) {
                        ClickType ct = parseClickType(data.getString(p + "click." + c + ".type"));
                        if (ct == null) continue;
                        l.getClickActions().add(new ClickAction(ct, data.getString(p + "click." + c + ".action")));
                    }
                }
                lines.add(l);
            }
        }
        return this;
    }

    private static Vector parseVector(String s) {
        if (s == null) return new Vector(0, 0, 0);
        String[] parts = s.split(",");
        if (parts.length < 3) return new Vector(0, 0, 0);
        try {
            return new Vector(
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()));
        } catch (NumberFormatException e) {
            return new Vector(0, 0, 0);
        }
    }

    private static HologramType parseType(String s) {
        if (s == null) return HologramType.COMPLEX;
        try {
            return HologramType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return HologramType.COMPLEX;
        }
    }

    private static LineType parseLineType(String s) {
        if (s == null) return LineType.TEXT;
        try {
            return LineType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return LineType.TEXT;
        }
    }

    private static Billboard parseBillboard(String s) {
        if (s == null) return Billboard.CENTER;
        try {
            return Billboard.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Billboard.CENTER;
        }
    }

    private static int clampLight(int v) {
        return (v < 0 || v > 15) ? -1 : v;
    }

    public static float wrapDegrees(float deg) {
        float a = deg % 360f;
        if (a >= 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }

    private static ClickType parseClickType(String s) {
        if (s == null) return null;
        try {
            return ClickType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setRawLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Location getLocation() {
        if (world == null || Bukkit.getWorld(world) == null) return null;
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public void setLocation(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HologramType getType() {
        return type;
    }

    public void setType(HologramType type) {
        this.type = type;
    }

    public boolean isTextOnly() {
        return type == HologramType.TEXT_ONLY;
    }

    public boolean isPerPlayer() {
        return perPlayer;
    }

    public void setPerPlayer(boolean perPlayer) {
        this.perPlayer = perPlayer;
    }

    public double getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(double s) {
        this.lineSpacing = s;
    }

    public String getViewPermission() {
        return viewPermission;
    }

    public void setViewPermission(String p) {
        this.viewPermission = p;
    }

    public String getViewCondition() {
        return viewCondition;
    }

    public void setViewCondition(String c) {
        this.viewCondition = c;
    }

    public List<HologramLine> getLines() {
        return lines;
    }

    public Vector getGlobalOffset() {
        return globalOffset;
    }

    public void setGlobalOffset(Vector v) {
        this.globalOffset.setX(v.getX());
        this.globalOffset.setY(v.getY());
        this.globalOffset.setZ(v.getZ());
    }

    public float getGlobalYaw() {
        return globalYaw;
    }

    public void setGlobalYaw(float v) {
        this.globalYaw = v;
    }

    public float getGlobalPitch() {
        return globalPitch;
    }

    public void setGlobalPitch(float v) {
        this.globalPitch = v;
    }

    public float getGlobalRoll() {
        return globalRoll;
    }

    public void setGlobalRoll(float v) {
        this.globalRoll = v;
    }

    public float getGlobalScale() {
        return globalScale;
    }

    public void setGlobalScale(float v) {
        this.globalScale = v;
    }

    public Billboard getGlobalBillboard() {
        return globalBillboard;
    }

    public void setGlobalBillboard(Billboard b) {
        this.globalBillboard = b;
    }

    public boolean isGlobalBgSet() {
        return globalBgSet;
    }

    public String getGlobalBackground() {
        return globalBackground;
    }

    public void setGlobalBackground(String bg) {
        this.globalBgSet = true;
        this.globalBackground = bg;
    }

    public void clearGlobalBackground() {
        this.globalBgSet = false;
        this.globalBackground = null;
    }

    public int getGlobalBlockLight() {
        return globalBlockLight;
    }

    public int getGlobalSkyLight() {
        return globalSkyLight;
    }

    public void setGlobalBrightness(int block, int sky) {
        this.globalBlockLight = block;
        this.globalSkyLight = sky;
    }

    public void clearGlobalBrightness() {
        this.globalBlockLight = -1;
        this.globalSkyLight = -1;
    }

    public boolean hasGlobalBrightness() {
        return globalBlockLight >= 0 && globalSkyLight >= 0;
    }

    public float effectiveYaw(HologramLine l) {
        return wrapDegrees(l.getYaw() + globalYaw);
    }

    public float effectivePitch(HologramLine l) {
        return wrapDegrees(l.getPitch() + globalPitch);
    }

    public float effectiveRoll(HologramLine l) {
        return wrapDegrees(l.getRoll() + globalRoll);
    }

    public float effectiveScale(HologramLine l) {
        float s = l.getScale() + globalScale;
        if (s < 0.05f) s = 0.05f;
        if (s > 10f) s = 10f;
        return s;
    }

    public double effectiveSpacing(HologramLine l) {
        return l.hasSpacing() ? l.getSpacing() : lineSpacing;
    }

    public Billboard effectiveBillboard(HologramLine l) {
        return globalBillboard != null ? globalBillboard : l.getBillboard();
    }

    public String effectiveBackground(HologramLine l) {
        return globalBgSet ? globalBackground : l.getBackground();
    }

    public String getWorldName() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
