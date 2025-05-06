package com.runemate.party.crafting;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;

public enum TanningLocation {

    AL_KHARID(new Coordinate(3270, 3192, 0), new Coordinate(3272, 3190, 0)),
    TAVERLEY(new Coordinate(2886, 3440, 0), new Coordinate(2887, 3442, 0)),
    CATHERBY(new Coordinate(2800, 3410, 0), new Coordinate(2801, 3411, 0));

    private final Coordinate coordinate1;
    private final Coordinate coordinate2;

    TanningLocation(Coordinate coordinate1, Coordinate coordinate2) {
        this.coordinate1 = coordinate1;
        this.coordinate2 = coordinate2;
    }

    public Coordinate getCoordinate1() {
        return coordinate1;
    }

    public Coordinate getCoordinate2() {
        return coordinate2;
    }

    public Area getArea() {
        return new Area.Rectangular(coordinate1, coordinate2);
    }
}
