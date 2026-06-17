package com.darksoldier1404.dphologram.data;

import org.bukkit.entity.Display.Billboard;
import org.bukkit.util.Vector;

public class HologramLine {
    private LineType type;
    private String content;
    private Vector offset = new Vector(0, 0, 0);
    private float yaw = 0f, pitch = 0f, roll = 0f;
    private float scale = 1.0f;
    private Billboard billboard = Billboard.CENTER;
    private String background;
    private int blockLight = -1;
    private int skyLight = -1;
    private boolean spacingSet;
    private double spacing;
    private final java.util.List<ClickAction> clickActions = new java.util.ArrayList<>();

    public HologramLine() {
    }

    public HologramLine(LineType type, String content) {
        this.type = type;
        this.content = content;
    }

    public Vector computeOffset(double stackY) {
        return new Vector(offset.getX(), offset.getY() + stackY, offset.getZ());
    }

    public boolean hasSpacing() {
        return spacingSet;
    }

    public double getSpacing() {
        return spacing;
    }

    public void setSpacing(double spacing) {
        this.spacingSet = true;
        this.spacing = spacing;
    }

    public void clearSpacing() {
        this.spacingSet = false;
        this.spacing = 0;
    }

    public LineType getType() {
        return type;
    }

    public void setType(LineType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Vector getOffset() {
        return offset;
    }

    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public boolean hasRotation() {
        return yaw != 0f || pitch != 0f || roll != 0f;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Billboard getBillboard() {
        return billboard;
    }

    public void setBillboard(Billboard b) {
        this.billboard = b;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String bg) {
        this.background = bg;
    }

    public int getBlockLight() {
        return blockLight;
    }

    public void setBlockLight(int blockLight) {
        this.blockLight = blockLight;
    }

    public int getSkyLight() {
        return skyLight;
    }

    public void setSkyLight(int skyLight) {
        this.skyLight = skyLight;
    }

    public boolean hasBrightness() {
        return blockLight >= 0 && skyLight >= 0;
    }

    public void clearBrightness() {
        this.blockLight = -1;
        this.skyLight = -1;
    }

    public java.util.List<ClickAction> getClickActions() {
        return clickActions;
    }
}
