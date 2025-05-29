package org.nojorundev.funnytitles;

public class Title {
    private String displayName;
    private int cost;
    private String suffix;
    private final boolean show;

    public Title(String displayName, int cost, String suffix, boolean show) {
        this.displayName = displayName;
        this.cost = cost;
        this.suffix = suffix;
        this.show = show;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }

    public String getSuffix() {
        return suffix;
    }

    public boolean isShow() {
        return show;
    }
}
