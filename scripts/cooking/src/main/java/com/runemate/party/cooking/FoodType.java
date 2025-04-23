package com.runemate.party.cooking;

public enum FoodType {

    SHRIMP("Raw shrimps"),
    ANCHOVIES("Raw anchovies"),
    TROUT("Raw trout"),
    SALMON("Raw salmon"),
    TUNA("Raw tuna"),
    LOBSTER("Raw lobster"),
    SWORDFISH("Raw swordfish"),
    MONKFISH("Raw monkfish"),
    SHARK("Raw shark"),
    KARAMBWAN("Raw karambwan"),
    MANTA_RAY("Raw manta ray"),
    SEA_TURTLE("Raw sea turtle"),
    DARK_CRAB("Raw dark crab");

    private final String name;
    FoodType(String name) { this.name = name; }
    @Override public String toString() { return name; }

    public String getRawName() {
        return name;
    }

    public String getCookedName() {
        return name.replace("Raw ", "");
    }
}