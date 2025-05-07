package com.runemate.party.crafting;

public enum LeatherProduct {

    GLOVES("Gloves", 1059, "Leather"),
    BOOTS("Boots", 1061, "Leather"),
    COWL("Cowl", 1167, "Leather"),
    COIF("Coif", 1169, "Leather"),
    VAMBRACES("Vambraces", 1063, "Leather"),
    CHAPS("Chaps", 1095, "Leather"),
    BODY("Body", 1129, "Leather"),
    SHIELD("Shield", 22271, "Hard Leather"); // Hard leather body/shield

    private final String name;
    private final int itemId;
    private final String hideType;  // Now a String instead of HideType enum

    LeatherProduct(String name, int itemId, String hideType) {
        this.name = name;
        this.itemId = itemId;
        this.hideType = hideType;
    }

    public String getName() {
        return name;
    }

    public int getItemId() {
        return itemId;
    }

    public String getHideType() {
        return hideType;  // Returns the string representation of hideType
    }

    @Override
    public String toString() {
        return name;
    }
}
