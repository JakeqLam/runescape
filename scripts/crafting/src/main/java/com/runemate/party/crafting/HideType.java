package com.runemate.party.crafting;

public enum HideType {
    COWHIDE("Cowhide", "Soft leather", 1),
    HARD_LEATHER("Cowhide", "Hard leather", 3),
    GREEN_DRAGONHIDE("Green dragonhide", "Green d'hide", 20),
    BLUE_DRAGONHIDE("Blue dragonhide", "Blue d'hide", 20),
    RED_DRAGONHIDE("Red dragonhide", "Red d'hide", 20),
    BLACK_DRAGONHIDE("Black dragonhide", "Black d'hide", 20);

    private final String rawName;
    private final String tannedName;
    private final int cost;

    HideType(String rawName, String tannedName, int cost) {
        this.rawName = rawName;
        this.tannedName = tannedName;
        this.cost = cost;
    }

    public String getRawName() {
        return rawName;
    }

    public String getTannedName() {
        return tannedName;
    }

    public int getCost() {
        return cost;
    }
}
