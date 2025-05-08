package com.runemate.party.mining;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;

public enum BankLocation {
    NEAREST(null),  // Special entry for auto-detect
    VARROCK_WEST(new Area.Rectangular(new Coordinate(3181, 3436, 0), new Coordinate(3186, 3444, 0))),
    VARROCK_EAST(new Area.Rectangular(new Coordinate(3251, 3420, 0), new Coordinate(3257, 3426, 0))),
    GRAND_EXCHANGE(new Area.Rectangular(new Coordinate(3161, 3476, 0), new Coordinate(3169, 3486, 0))),
    EDGEVILLE(new Area.Rectangular(new Coordinate(3092, 3489, 0), new Coordinate(3099, 3499, 0))),
    FALADOR_EAST(new Area.Rectangular(new Coordinate(3010, 3352, 0), new Coordinate(3016, 3359, 0))),
    FALADOR_WEST(new Area.Rectangular(new Coordinate(2942, 3367, 0), new Coordinate(2946, 3375, 0))),
    DRAYNOR(new Area.Rectangular(new Coordinate(3092, 3240, 0), new Coordinate(3099, 3245, 0))),
    AL_KHARID(new Area.Rectangular(new Coordinate(3268, 3161, 0), new Coordinate(3273, 3170, 0))),
    CAMELOT(new Area.Rectangular(new Coordinate(2721, 3491, 0), new Coordinate(2726, 3494, 0))),
    SEERS_VILLAGE(new Area.Rectangular(new Coordinate(2723, 3492, 0), new Coordinate(2730, 3499, 0))),
    ARDOUGNE_NORTH(new Area.Rectangular(new Coordinate(2611, 3330, 0), new Coordinate(2617, 3337, 0))),
    ARDOUGNE_SOUTH(new Area.Rectangular(new Coordinate(2652, 3283, 0), new Coordinate(2658, 3289, 0))),
    CATHERBY(new Area.Rectangular(new Coordinate(2805, 3439, 0), new Coordinate(2810, 3444, 0))),
    YANILLE(new Area.Rectangular(new Coordinate(2610, 3090, 0), new Coordinate(2615, 3095, 0))),
    ZEAH(new Area.Rectangular(new Coordinate(1634, 3762, 0), new Coordinate(1640, 3767, 0)));

    private final Area area;

    BankLocation(Area area) {
        this.area = area;
    }

    public Area getArea() {
        return area;
    }

    public boolean isNearest() {
        return this == NEAREST;
    }
}