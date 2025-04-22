package com.runemate.party.mining;

public enum OreType {
    COPPER("Copper rocks"),
    TIN("Tin rocks"),
    IRON("Iron rocks"),
    COAL("Coal rocks"),
    MITHRIL("Mithril rocks"),
    ADAMANTITE("Adamantite rocks"),
    RUNITE("Runite rocks");

    private final String name;
    OreType(String name) { this.name = name; }
    @Override public String toString() { return name; }
}