package com.runemate.party.miner;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;

public enum MiningLocation {
    VARROCK_EAST("Varrock East", new Area.Rectangular(new Coordinate(3282, 3363, 0), new Coordinate(3288, 3370, 0))),
    VARROCK_WEST("Varrock West", new Area.Rectangular(new Coordinate(3173, 3361, 0), new Coordinate(3178, 3367, 0))),
    LUMBRIDGE_SWAMP("Lumbridge Swamp", new Area.Rectangular(new Coordinate(3222, 3144, 0), new Coordinate(3229, 3150, 0))),
    AL_KHARID("Al Kharid", new Area.Rectangular(new Coordinate(3297, 3277, 0), new Coordinate(3305, 3283, 0))),
    RIMMINGTON("Rimmington", new Area.Rectangular(new Coordinate(2972, 3232, 0), new Coordinate(2979, 3240, 0))),
    FALADOR_SOUTH("Falador South", new Area.Rectangular(new Coordinate(3047, 3296, 0), new Coordinate(3055, 3303, 0))),
    DWARVEN_MINE("Dwarven Mine", new Area.Rectangular(new Coordinate(3012, 3448, 0), new Coordinate(3018, 3455, 0))),
    MINING_GUILD("Mining Guild", new Area.Rectangular(new Coordinate(3010, 3330, 0), new Coordinate(3017, 3337, 0))),
    ARDOUGNE_SOUTH("Ardougne South", new Area.Rectangular(new Coordinate(2608, 3307, 0), new Coordinate(2613, 3313, 0))),
    YANILLE("Yanille", new Area.Rectangular(new Coordinate(2612, 3147, 0), new Coordinate(2618, 3153, 0)));

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
