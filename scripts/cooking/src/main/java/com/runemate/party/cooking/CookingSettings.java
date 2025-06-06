package com.runemate.party.cooking;

import com.runemate.ui.setting.annotation.open.Range;
import com.runemate.ui.setting.annotation.open.Setting;
import com.runemate.ui.setting.annotation.open.SettingsGroup;
import com.runemate.ui.setting.open.Settings;


@SettingsGroup(group = "GPTCooker")
public interface CookingSettings extends Settings {
    @Setting(key = "foodType", title = "Food type", order = 1)
    default FoodType getFoodType() {
        return FoodType.SHRIMP;
    }

    @Setting(key = "location", title = "Cooking Location", order = 2)
    default CookingLocation getLocation() {
        return CookingLocation.AL_KHARID;
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