package com.runemate.party.combat;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.ui.setting.annotation.open.Range;
import com.runemate.ui.setting.annotation.open.Setting;
import com.runemate.ui.setting.annotation.open.SettingsGroup;
import com.runemate.ui.setting.open.Settings;


@SettingsGroup(group = "GPTCombat")
public interface CombatSettings extends Settings {

    @Setting(key = "lootValueThreshold", title = "Minimum Item Value to Loot (GP)", order = 10)
    @Range(min = 1, max = 500000)
    default int getLootValueThreshold() {
        return 1000; // default to 1k GP
    }

    @Setting(key = "foodWithdrawAmount", title = "Amount of Food to Withdraw", order = 1)
    @Range(min = 1, max = 28)
    default int getFoodWithdrawAmount() {
        return 8; // Default withdraw amount
    }

    @Setting(key = "foodType", title = "Food type", order = 1)
    default FoodType getFoodType() {
        return FoodType.SHRIMP;
    }

    @Setting(key = "monsterName", title = "Monster Name", order = 2)
    default String getMonsterName() {
        return "Chicken";
    }

    @Setting(key = "playerLocation", title = "Player Start Location", order = 3, disabled = true)
    default String getPlayerLocation() {
        return new Coordinate(3222, 3218, 0).toString(); // Default to Lumbridge
    }

    @Setting(key = "combatRadius", title = "Combat Radius", order = 4)
    @Range(min = 1, max = 20)
    default int getCombatRadius() {
        return 5;
    }

    @Setting(key = "playerLocation", title = "")
    void setPlayerLocation(Coordinate playerLocation);

    @Setting(key = "setCurrentAsStart", title = "Set Current Tile as Start (toggle ON)", order = 6)
    default boolean shouldUpdateLocation() {
        return false;
    }

    @Setting(key = "setCurrentAsStart", title = "")
    void setShouldUpdateLocation(boolean value); // Required for toggle reset

    @Setting(key = "eatBelowPercent", title = "Eat below % HP", order = 5)
    @Range(min = 10, max = 90)
    default int getEatBelowPercent() {
        return 50;
    }

    @Setting(key = "breakMinLength", title = "How long to take break min (sec)", order = 5)
    @Range(min = 5, max = 10000)
    default int getBreakMinLength() {
        return 6000;
    }

    @Setting(key = "breakMaxLength", title = "How long to take break max (sec)", order = 6)
    @Range(min = 5, max = 10000)
    default int getBreakMaxLength() {
        return 8000;
    }

    @Setting(key = "breakMin", title = "How often to take break Min (sec)", order = 7)
    @Range(min = 5, max = 10000)
    default int getBreakMin() {
        return 6000;
    }

    @Setting(key = "breakMax", title = "How often to take break Max (sec)", order = 8)
    @Range(min = 5, max = 10000)
    default int getBreakMax() {
        return 8000;
    }

    @Setting(key = "useP2P", title = "Use P2P worlds", order = 9)
    default boolean getUseP2P() {
        return false;
    }
}