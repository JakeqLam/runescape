package com.runemate.party.crafting;

public enum HideType {
    SOFT_LEATHER("Soft leather", "Cowhide", 124),
    HARD_LEATHER("Hard leather", "Cowhide", 125),
    SNAKESKIN_1("Snakeskin", "Snake hide", 126),   // 20 coins
    SNAKESKIN_2("Snakeskin", "Snake hide", 127),   // 15 coins
    GREEN_DHIDE("Green d'hide", "Green dragonhide", 128),
    BLUE_DHIDE("Blue d'hide", "Blue dragonhide", 129),
    RED_DHIDE("Red d'hide", "Red dragonhide", 130),
    BLACK_DHIDE("Black d'hide", "Black dragonhide", 131);

    private final String tannedName;
    private final String rawName;
    private final int componentId;

    HideType(String tannedName, String rawName, int componentId) {
        this.tannedName = tannedName;
        this.rawName = rawName;
        this.componentId = componentId;
    }

    public String getTannedName() {
        return tannedName;
    }

    public String getRawName() {
        return rawName;
    }

    public int getComponentId() {
        return componentId;
    }

}
