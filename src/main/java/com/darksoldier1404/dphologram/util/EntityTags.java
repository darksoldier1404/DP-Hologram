package com.darksoldier1404.dphologram.util;

import com.darksoldier1404.dphologram.DPHologram;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

public final class EntityTags {

    public static NamespacedKey HOLO;

    public static NamespacedKey LINE;

    private EntityTags() {
    }

    public static void init(DPHologram plugin) {
        HOLO = new NamespacedKey(plugin, "holo_name");
        LINE = new NamespacedKey(plugin, "line_index");
    }

    public static void tag(Entity e, String holo, int line) {
        e.getPersistentDataContainer().set(HOLO, PersistentDataType.STRING, holo);
        e.getPersistentDataContainer().set(LINE, PersistentDataType.INTEGER, line);
    }

    public static boolean isOurs(Entity e) {
        return e.getPersistentDataContainer().has(HOLO, PersistentDataType.STRING);
    }

    public static String holoOf(Entity e) {
        return e.getPersistentDataContainer().get(HOLO, PersistentDataType.STRING);
    }

    public static Integer lineOf(Entity e) {
        return e.getPersistentDataContainer().get(LINE, PersistentDataType.INTEGER);
    }
}
