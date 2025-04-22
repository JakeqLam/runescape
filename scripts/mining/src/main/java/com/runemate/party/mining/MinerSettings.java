package com.runemate.party.mining;

import com.runemate.ui.setting.annotation.open.Setting;
import com.runemate.ui.setting.annotation.open.SettingsGroup;
import com.runemate.ui.setting.annotation.open.Range;
import com.runemate.ui.setting.open.*;


@SettingsGroup(group = "SimpleMiner")
public interface MinerSettings extends Settings {
    @Setting(key = "oreType", title = "Ore type", order = 1)
    default OreType getOreType() {
        return OreType.TIN;
    }

    @Setting(key = "location", title = "Mining Location", order = 2)
    default MiningLocation getLocation() {
        return MiningLocation.VARROCK_WEST;
    }

    @Setting(key = "breakMinLength", title = "How long to take break min (sec)", order = 3)
    @Range(min = 5, max = 10000)
    default int getBreakMinLength() {
        return 6000;
    }

    @Setting(key = "breakMaxLength", title = "How long to take break max (sec)", order = 4)
    @Range(min = 5, max = 10000)
    default int getBreakMaxLength() {
        return 8000;
    }

    @Setting(key = "breakMin", title = "How often to take break Min (sec)", order = 5)
    @Range(min = 5, max = 10000)
    default int getBreakMin() {
        return 6000;
    }

    @Setting(key = "breakMax", title = "How often to take break Max (sec)", order = 6)
    @Range(min = 5, max = 10000)
    default int getBreakMax() {
        return 8000;
    }

    @Setting(key = "useP2P", title = "Use P2P worlds", order = 7)
    default boolean getUseP2P() {
        return false;
    }
}