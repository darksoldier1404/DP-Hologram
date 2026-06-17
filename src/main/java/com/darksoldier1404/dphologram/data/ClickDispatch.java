package com.darksoldier1404.dphologram.data;

import java.util.ArrayList;
import java.util.List;

public final class ClickDispatch {

    private ClickDispatch() {
    }

    public static List<String> match(HologramLine line, ClickType incoming) {
        List<String> out = new ArrayList<>();
        for (ClickAction ca : line.getClickActions()) {
            if (ca.matches(incoming)) out.add(ca.getActionRef());
        }
        return out;
    }
}
