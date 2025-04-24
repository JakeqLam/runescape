package com.runemate.party.fishing;

public enum FishingMethod {
    SMALL_NET("Net", "Small fishing net"),
    BAIT("Bait", "Feather", "Fishing rod"),
    LURE("Lure", "Feather", "Fly fishing rod"),
    HARPOON("Harpoon", "Harpoon"),
    CAGE("Cage", "Lobster pot");

    private final String actionName;
    private final String[] requiredItems;

    FishingMethod(String actionName, String... requiredItems) {
        this.actionName = actionName;
        this.requiredItems = requiredItems;
    }

    public String getActionName() {
        return actionName;
    }

    public String[] getRequiredItems() {
        return requiredItems;
    }
}