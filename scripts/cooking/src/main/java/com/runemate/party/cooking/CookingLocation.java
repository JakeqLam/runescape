package com.runemate.party.cooking;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;

public enum CookingLocation {

    // F2P Cooking Spots
    AL_KHARID("Al Kharid Range", new Area.Rectangular(new Coordinate(3273, 3180, 0), new Coordinate(3275, 3178, 0))),
//    VARROCK_EAST("Varrock East Bank Fire", new Area.Rectangular(new Coordinate(3264, 3422, 0), new Coordinate(3266, 3420, 0))),
//    LUMBRIDGE_CASTLE("Lumbridge Castle Range", new Area.Rectangular(new Coordinate(3209, 3217, 1), new Coordinate(3211, 3215, 1))),
//
//    this one create fire / use someone elses fire? ->
//    EDGEVILLE_FIRE("Edgeville Fire", new Area.Rectangular(new Coordinate(3074, 3498, 0), new Coordinate(3076, 3496, 0))),

    // P2P Cooking Spots
    ROGUES_DEN("Rogues' Den", new Area.Rectangular(new Coordinate(3043, 4975, 1), new Coordinate(3046, 4973, 1))),
    HOSIDIUS_KITCHEN("Hosidius Kitchen", new Area.Rectangular(new Coordinate(1665, 3617, 0), new Coordinate(1668, 3615, 0))),
    CATHERBY_RANGE("Catherby Range", new Area.Rectangular(new Coordinate(2816, 3431, 0), new Coordinate(2818, 3429, 0))),
    COOKING_GUILD("Cooking Guild", new Area.Rectangular(new Coordinate(3140, 3442, 0), new Coordinate(3143, 3440, 0)));

    private final String name;
    private final Area.Rectangular area;

    CookingLocation(String name, Area.Rectangular area) {
        this.name = name;
        this.area = area;
    }

    @Override
    public String toString() {
        return name;
    }

    public Area.Rectangular getArea() {
        return area;
    }
}
