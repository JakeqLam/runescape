package com.runemate.party.fishing;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;

public enum FishingSpot {
    DRAYNOR("Draynor Fishing Spot", new Area.Polygonal(
            new Coordinate[] {
                    // Tight area around your fishing spot
                    new Coordinate(3082, 3224, 0),  // Southwest corner
                    new Coordinate(3092, 3224, 0),  // Southeast corner
                    new Coordinate(3092, 3234, 0),  // Northeast corner
                    new Coordinate(3082, 3234, 0)   // Northwest corner
            }
    )),
    CATHERBY("Catherby Fishing Spot", new Area.Rectangular(
            new Coordinate(2809, 3435, 0),
            new Coordinate(2815, 3441, 0)
    )),
    LUMBRIDGE_SWAMP("Lumbridge Swamp Fishing", new Area.Rectangular(
            new Coordinate(3235, 3145, 0), // Extend northwest
            new Coordinate(3255, 3165, 0)  // Extend northeast/southeast
    )),
//    AL_KHARID_RIVER("Al Kharid River Bank", new Area.Rectangular(
//            new Coordinate(3266, 3139, 0),  // Southwest corner
//            new Coordinate(3276, 3149, 0)   // Northeast corner
//    )),
    BARBARIAN_VILLAGE("Barbarian Village Fishing", new Area.Rectangular(
            new Coordinate(3100, 3425, 0),
            new Coordinate(3112, 3435, 0)
    ));

    private final String name;
    private final Area area;

    FishingSpot(String name, Area area) {
        this.name = name;
        this.area = area;
    }

    @Override
    public String toString() {
        return name;
    }

    public Area getArea() {
        return area;
    }
}
