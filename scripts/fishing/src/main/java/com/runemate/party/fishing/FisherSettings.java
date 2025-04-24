package com.runemate.party.fishing;

import com.runemate.ui.setting.annotation.open.Range;
import com.runemate.ui.setting.annotation.open.Setting;
import com.runemate.ui.setting.annotation.open.SettingsGroup;
import com.runemate.ui.setting.open.Settings;


@SettingsGroup(group = "SimpleFisher")
public interface FisherSettings extends Settings {
    @Setting(key = "method", title = "Fishing Method", order = 1)
    default FishingMethod getMethod() {
        return FishingMethod.SMALL_NET; // options: NET, BAIT, HARPOON
    }

    @Setting(key = "spot", title = "Fishing Spot", order = 2)
    default FishingSpot getSpot() {
        return FishingSpot.LUMBRIDGE_SWAMP; // options: DRAYNOR, CATHERBY
    }

    @Setting(key = "breakMinLength", title = "Break Length Min (sec)", order = 3)
    @Range(min = 5, max = 10000)
    default int getBreakMinLength() { return 6000; }

    @Setting(key = "breakMaxLength", title = "Break Length Max (sec)", order = 4)
    @Range(min = 5, max = 10000)
    default int getBreakMaxLength() { return 8000; }

    @Setting(key = "breakMin", title = "Break Interval Min (sec)", order = 5)
    @Range(min = 5, max = 10000)
    default int getBreakMin() { return 6000; }

    @Setting(key = "breakMax", title = "Break Interval Max (sec)", order = 6)
    @Range(min = 5, max = 10000)
    default int getBreakMax() { return 8000; }

    @Setting(key = "useP2P", title = "Use P2P Worlds", order = 7)
    default boolean getUseP2P() { return false; }
}