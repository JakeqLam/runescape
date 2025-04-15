package com.runemate.party.miner;

import com.runemate.game.api.hybrid.location.Area;
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

    @Setting(key = "location", title = "Mining Location", order = 7)
    default MiningLocation getLocation() {
        return MiningLocation.VARROCK_EAST;
    }

    @Setting(key = "breakMin", title = "Break Min (sec)", order = 4)
    @Range(min = 5, max = 6000)
    default int getBreakMin() {
        return 150;
    }

    @Setting(key = "breakMax", title = "Break Max (sec)", order = 5)
    @Range(min = 5, max = 6000)
    default int getBreakMax() {
        return 300;
    }

    @Setting(key = "useP2P", title = "Use P2P worlds", order = 6)
    default boolean getUseP2P() {
        return false;
    }
}