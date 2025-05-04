package com.runemate.party.combat;

public enum FoodType {

    SHRIMP("Shrimps"),
    ANCHOVIES("Anchovies"),
    TROUT("Trout"),
    SALMON("Salmon"),
    TUNA("Tuna"),
    LOBSTER("Lobster"),
    SWORDFISH("Swordfish"),
    MONKFISH("Monkfish"),
    SHARK("Shark"),
    KARAMBWAN("Cooked karambwan"),
    MANTA_RAY("Manta ray"),
    SEA_TURTLE("Sea turtle"),
    DARK_CRAB("Dark crab");

    private final String name;

    FoodType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getCookedName() {
        return name;
    }

    public String getRawName() {
        if (name.equals("Cooked karambwan")) {
            return "Raw karambwan";
        }
        return "Raw " + name;
    }
}