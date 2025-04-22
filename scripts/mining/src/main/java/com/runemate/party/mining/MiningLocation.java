package com.runemate.party.mining;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;

public enum MiningLocation {
    //fix this
//    VARROCK_EAST("Varrock East Mine", new Area.Rectangular(
//            new Coordinate(3283, 3359, 0), new Coordinate(3291, 3366, 0))), // Tin, Copper, Iron

    VARROCK_WEST("Varrock West Mine", new Area.Rectangular(
            new Coordinate(3173, 3361, 0), new Coordinate(3178, 3370, 0))), // Tin, Copper, Iron (extended north)

    LUMBRIDGE_SWAMP("Lumbridge Swamp Mine", new Area.Rectangular(
            new Coordinate(3222, 3144, 0), new Coordinate(3229, 3150, 0))), // Tin, Copper

    AL_KHARID("Al Kharid Mine", new Area.Rectangular(
            new Coordinate(3296, 3284, 0), new Coordinate(3306, 3297, 0))), // Iron, Clay

    RIMMINGTON("Rimmington Mine", new Area.Rectangular(
            new Coordinate(2972, 3232, 0), new Coordinate(2979, 3240, 0))), // Clay, Silver

//    FALADOR_SOUTH("Falador South Mine", new Area.Rectangular(
//            new Coordinate(3052, 3307, 0), new Coordinate(3060, 3315, 0))), // Iron

    DWARVEN_MINE("Dwarven Mine", new Area.Rectangular(
            new Coordinate(2975, 9803, 1), new Coordinate(3023, 9820, 1))), // Underground (Z=1) - Iron, Mithril, Coal

    MINING_GUILD("Mining Guild", new Area.Rectangular(
            new Coordinate(3017, 9738, 0), new Coordinate(3055, 9755, 0))), // Underground (Z=0) - Coal, Mithril (Requires 60 Mining)

    ARDOUGNE_SOUTH("Ardougne Gem Mine", new Area.Rectangular(
            new Coordinate(2628, 3298, 0), new Coordinate(2637, 3306, 0))), // Gem rocks

    YANILLE("Yanille Mine", new Area.Rectangular(
            new Coordinate(2612, 3147, 0), new Coordinate(2618, 3153, 0))); // Coal, Silver

    private final String name;
    private final Area.Rectangular area;

    MiningLocation(String name, Area.Rectangular area) {
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
