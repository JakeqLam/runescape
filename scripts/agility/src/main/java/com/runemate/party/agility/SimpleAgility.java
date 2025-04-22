package com.runemate.party.agility;

import com.runemate.game.api.script.framework.LoopingBot;
import com.runemate.game.api.script.framework.listeners.SettingsListener;
import com.runemate.game.api.script.framework.listeners.events.SettingChangedEvent;

public class SimpleAgility extends LoopingBot implements SettingsListener {

    @Override
    public void onStart(String... args) {
        getEventDispatcher().addListener(this);
    }

    @Override
    public void onLoop() {

    }

    @Override
    public void onStop() {

    }

    // Fired when any single setting changes
    @Override
    public void onSettingChanged(SettingChangedEvent event) {
        applySettings();
    }

    @Override
    public void onSettingsConfirmed() {

    }

    // Helper to read & cache settings
    private void applySettings() {

    }
}
