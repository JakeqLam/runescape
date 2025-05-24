package com.runemate.party.fishing;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.InterfaceWindows;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.basic.BresenhamPath;
import com.runemate.game.api.hybrid.location.navigation.cognizant.ScenePath;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.LoopingBot;
import com.runemate.game.api.script.framework.listeners.SettingsListener;
import com.runemate.game.api.script.framework.listeners.events.SettingChangedEvent;
import com.runemate.party.common.AntiBan;
import com.runemate.party.common.GPTNavigation;
import com.runemate.pathfinder.Pathfinder;
import com.runemate.ui.setting.annotation.open.SettingsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SimpleFisher extends LoopingBot implements SettingsListener {

    private static final List<Area> BANK_LOCATIONS = Arrays.asList(
            new Area.Rectangular(new Coordinate(3207, 3218, 2), new Coordinate(3211, 3222, 2)), // Lumbridge Castle 2nd floor bank
            new Area.Rectangular(new Coordinate(3181, 3436, 0), new Coordinate(3186, 3444, 0)), // Varrock West
            new Area.Rectangular(new Coordinate(3251, 3420, 0), new Coordinate(3257, 3426, 0)), // Varrock East
            new Area.Rectangular(new Coordinate(3161, 3476, 0), new Coordinate(3169, 3486, 0)), // Grand Exchange
            new Area.Rectangular(new Coordinate(3092, 3489, 0), new Coordinate(3099, 3499, 0)), // Edgeville
            new Area.Rectangular(new Coordinate(3010, 3352, 0), new Coordinate(3016, 3359, 0)), // Falador East
            new Area.Rectangular(new Coordinate(2942, 3367, 0), new Coordinate(2946, 3375, 0)), // Falador West
            new Area.Rectangular(new Coordinate(3092, 3240, 0), new Coordinate(3099, 3245, 0)), // Draynor
            new Area.Rectangular(new Coordinate(3268, 3161, 0), new Coordinate(3273, 3170, 0)), // Al Kharid
            new Area.Rectangular(new Coordinate(2805, 3439, 0), new Coordinate(2810, 3444, 0)), // Catherby
            new Area.Rectangular(new Coordinate(2721, 3491, 0), new Coordinate(2726, 3494, 0)), // Camelot
            new Area.Rectangular(new Coordinate(2723, 3492, 0), new Coordinate(2730, 3499, 0)), // Seers
            new Area.Rectangular(new Coordinate(2611, 3330, 0), new Coordinate(2617, 3337, 0)), // Ardougne North
            new Area.Rectangular(new Coordinate(2652, 3283, 0), new Coordinate(2658, 3289, 0)), // Ardougne South
            new Area.Rectangular(new Coordinate(2610, 3090, 0), new Coordinate(2615, 3095, 0)), // Yanille
            new Area.Rectangular(new Coordinate(1634, 3762, 0), new Coordinate(1640, 3767, 0))  // Zeah
    );

    @SettingsProvider(updatable = true)
    private FisherSettings settings;

    private Pathfinder pathfinder;
    private AntiBan antiBan;
    private GPTNavigation navigation;
    private boolean settingsConfirmed;

    boolean isFishingInLumbridge;
    private long earlyDepositCooldown = 0; // in milliseconds
    private long lastEarlyDepositCheck = 0; // in milliseconds

    private long lastEarlyBankCheck = 0;
    private long earlyBankCooldown = (long)getGaussian(3, 5, 4, 0.7);

    // Gaussian random number within bounds (min, max) with mean and std deviation
    private double getGaussian(double min, double max, double mean, double stdDev) {
        double value;
        do {
            value = Random.nextGaussian(min, max, mean, stdDev);
        } while (value < min || value > max);
        return value;
    }

    @Override
    public void onStart(String... args) {
        antiBan = new AntiBan();
        navigation = new GPTNavigation();
        getEventDispatcher().addListener(this);
        pathfinder = Pathfinder.create(this);
        System.out.println("[SimpleFisher] Started");
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) return;

        isFishingInLumbridge = settings.getSpot().equals(FishingSpot.LUMBRIDGE_SWAMP);

        if (!InterfaceWindows.getInventory().isOpen()) {
            System.out.println("📂 Inventory tab is NOT open. Opening now...");
            Keyboard.pressKey(KeyEvent.VK_F3); // F3 is usually the Inventory hotkey
            Execution.delay(300, 600);
        }

        Player player = Players.getLocal();
        if (player == null) return;

        // Break logic
        antiBan.performBreakLogic(settings.getBreakMin(), settings.getBreakMax());

        // Bank if inventory full
        int inventoryCount = Inventory.getItems().size();
        long currentTime = System.currentTimeMillis();

        boolean shouldCheckEarlyDeposit = (currentTime - lastEarlyDepositCheck) > earlyDepositCooldown;
        boolean earlyDepositChance = false;

        if (shouldCheckEarlyDeposit) {
            earlyDepositChance = inventoryCount >= 24 && Random.nextInt(100) < 5;

            // Always reset cooldown to avoid rapid re-checks
            earlyBankCooldown = (long) getGaussian(2500, 4000, 3200, 500);
            lastEarlyDepositCheck = currentTime;
        }

        if (Inventory.isFull() || earlyDepositChance) {
            System.out.println("Depositing (reason: " +
                    (Inventory.isFull() ? "inventory full" : "early deposit at " + inventoryCount + " items") + ")");

            walkToAndDeposit();
            return;
        }

        // Anti-ban randomly
        antiBan.maybePerformAntiBan();

        // Fishing
        FishingMethod method = settings.getMethod();
        FishingSpot spotEnum = settings.getSpot();
        Area spotArea = spotEnum.getArea();

        Npc spot = Npcs.newQuery()
                .names("Fishing spot")
                .actions(method.getActionName())
                .within(spotArea)
                .results().nearest();

        if (spot != null && spot.isVisible()) {
            if (player.getAnimationId() == -1 && !player.isMoving()) {
                if (Random.nextInt(0, 100) < 8) {
                    System.out.println("[SimpleFisher] Simulating misclick near spot");
                    Mouse.move(spot.getPosition().randomize(3, 6));
                    Execution.delay((int)getGaussian(200, 500, 350, 100));
                    Mouse.click(Mouse.Button.LEFT);
                    Execution.delay((int)getGaussian(800, 1200, 1000, 150));
                }
                System.out.println("[SimpleFisher] Interacting with spot: " + spotEnum + " via " + method);
                if (spot.interact(method.getActionName())) {
                    Execution.delayUntil(
                            () -> player.getAnimationId() != -1,
                            (int)getGaussian(500, 1000, 750, 150),
                            (int)getGaussian(3000, 5000, 4000, 700)
                    );
                }
            }
        } else {
            if (spot != null) {
                System.out.println("[SimpleFisher] Turning camera to spot");
                Camera.turnTo(spot);
                Execution.delay((int)getGaussian(200, 400, 300, 70));
            } else {
                navigation.walkToArea(spotArea, pathfinder);
            }
        }
    }

    private void walkToAndDeposit() {
        Area closestBank;
        if (isFishingInLumbridge) {
            // Explicitly set Lumbridge Bank if fishing in Lumbridge
            closestBank = BANK_LOCATIONS.get(0); // Lumbridge Bank is at index 0
            System.out.println("[SimpleFisher] Fishing in Lumbridge, choosing Lumbridge Bank.");
        } else {
            // Choose nearest bank area for other fishing spots
            closestBank = BANK_LOCATIONS.stream()
                    .min(Comparator.comparingInt(b -> (int) b.getCenter().distanceTo(Players.getLocal().getPosition())))
                    .orElse(BANK_LOCATIONS.get(1)); // Fallback to the second closest bank if necessary
            System.out.println("[SimpleFisher] Walking to bank area: " + closestBank);
        }

        // Walk to bank area with fallback
        while (!closestBank.contains(Players.getLocal()))
            navigation.walkToArea(closestBank, pathfinder);

        // Interact with bank booth/chest
        GameObject bank = GameObjects.newQuery()
                .actions("Bank")
                .results()
                .nearest();

        while (bank != null && !Bank.isOpen() && closestBank.contains(Players.getLocal())) {
            Bank.open();
            Execution.delayUntil(Bank::isOpen,
                    (int) getGaussian(6000, 8000, 7000, 700));
            if (Bank.isOpen()) {

            System.out.println("[SimpleFisher] Bank opened, depositing items");
            // Deposit all except tools and bait
            FishingMethod method = settings.getMethod();
            String[] requiredItems = method.getRequiredItems();

            if (requiredItems.length > 0) {
                Bank.depositAllExcept(requiredItems);
                System.out.println("[SimpleFisher] Kept: " + Arrays.toString(requiredItems));
            } else {
                Bank.depositInventory();
                System.out.println("[SimpleFisher] Deposited all items");
            }

            Execution.delay((int) getGaussian(1000, 2000, 1500, 100));

            } else {
                System.out.println("[SimpleFisher] Failed to open bank, fallback clicking bank area");
                // fallback: click center of bank area
                Coordinate clickPoint = closestBank.getCenter().randomize(1,1);
                Mouse.move(clickPoint);
                Execution.delay((int) getGaussian(200, 400, 300, 70));
                Mouse.click(Mouse.Button.LEFT);

                Execution.delayUntil(Bank::isOpen,
                        (int) getGaussian(4000, 6000, 5000, 700),
                        (int) getGaussian(6000, 8000, 7000, 700));
            }
        }

        // Always close the bank after depositing
        Bank.close();
        // Add short delay to account for bank closure animation
        Execution.delay((int) getGaussian(300, 600, 450, 100));

        Execution.delayUntil(() -> !Bank.isOpen(),
                (int) getGaussian(1000, 3000, 2000, 700));
        System.out.println("[SimpleFisher] Deposit complete, closing bank");
    }

    @Override public void onStop() { System.out.println("[SimpleFisher] Stopped."); }
    @Override public void onSettingChanged(SettingChangedEvent e) {
        isFishingInLumbridge = settings.getSpot().equals(FishingSpot.LUMBRIDGE_SWAMP);
    }
    @Override public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }
}