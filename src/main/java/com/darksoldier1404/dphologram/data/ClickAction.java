package com.darksoldier1404.dphologram.data;

public class ClickAction {
    private final ClickType clickType;
    private final String actionRef;

    public ClickAction(ClickType clickType, String actionRef) {
        this.clickType = clickType;
        this.actionRef = actionRef;
    }

    public ClickType getClickType() {
        return clickType;
    }

    public String getActionRef() {
        return actionRef;
    }

    public boolean matches(ClickType incoming) {
        return clickType == ClickType.ANY || clickType == incoming;
    }
}
