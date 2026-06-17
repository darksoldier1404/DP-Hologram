package com.darksoldier1404.dphologram.papi;

import com.darksoldier1404.dppc.api.placeholder.PlaceholderUtils;
import org.bukkit.entity.Player;

public final class PapiConditionEvaluator {
    private PapiConditionEvaluator() {
    }

    public static String evaluate(Player p, String condition) {
        return PlaceholderUtils.applyPlaceholder(p, condition).trim();
    }
}
