package com.runemate.party.cooking;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
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

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SimpleCooker extends LoopingBot implements SettingsListener {

    @SettingsProvider(updatable = true)
    private CookingSettings settings;

    private static final Map<CookingLocation, Area> cookingLocationToBank = new HashMap<>();

    private Pathfinder pathfinder;
    AntiBan antiBan;
    GPTNavigation navigation;
    private boolean settingsConfirmed;

    static {
        cookingLocationToBank.put(CookingLocation.AL_KHARID, new Area.Rectangular(new Coordinate(3269, 3167, 0), new Coordinate(3271, 3165, 0)));
        cookingLocationToBank.put(CookingLocation.ROGUES_DEN, new Area.Rectangular(new Coordinate(3044, 4971, 1), new Coordinate(3046, 4969, 1)));
        cookingLocationToBank.put(CookingLocation.HOSIDIUS_KITCHEN, new Area.Rectangular(new Coordinate(1714, 3616, 0), new Coordinate(1716, 3614, 0)));
        cookingLocationToBank.put(CookingLocation.CATHERBY_RANGE, new Area.Rectangular(new Coordinate(2808, 3441, 0), new Coordinate(2810, 3439, 0)));
        cookingLocationToBank.put(CookingLocation.COOKING_GUILD, new Area.Rectangular(new Coordinate(3209, 3217, 1), new Coordinate(3211, 3215, 1)));
    }

    // Gaussian random number within bounds (min, max) with mean and std deviation
    private double getGaussian(double min, double max, double mean, double stdDev) {
        double value;
        do {
            value = Random.nextGaussian(min, max, mean, stdDev);
        } while (value < min || value > max);
        return value;
    }

    private boolean shouldMisclick() {
        return Random.nextInt(100) < 15; // 15% chance to misclick
    }

    private void misclickNear(Coordinate base) {
        Coordinate offset = new Coordinate(
                base.getX() + (int)getGaussian(-2, 2, 0, 1),
                base.getY() + (int)getGaussian(-2, 2, 0, 1),
                base.getPlane()
        );
        Mouse.move(offset);
        Execution.delay((int)getGaussian(150, 300, 225, 50));
        Mouse.click(Mouse.Button.LEFT);
        Execution.delay((int)getGaussian(800, 1200, 1000, 150));
    }

    @Override
    public void onStart(String... args) {
        antiBan = new AntiBan();
        navigation = new GPTNavigation();
        getEventDispatcher().addListener(this);
        pathfinder = Pathfinder.create(this);
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) return;
        if (Players.getLocal() == null || !RuneScape.isLoggedIn()) return;

        antiBan.performBreakLogic(settings.getBreakMin(), settings.getBreakMax());
        antiBan.maybePerformAntiBan();

        if (Inventory.contains(settings.getFoodType().getRawName())) {
            System.out.println("Raw food found in inventory. Proceeding to cook.");
            cookFood();
        } else {
            System.out.println("No raw food in inventory. Walking to bank to withdraw.");
            if (isAllFoodCooked()) {
                System.out.println("All food cooked. Stopping bot.");
                stop();
                return;
            }
            walkToBankAndWithdraw();
        }
    }

    private boolean isAllFoodCooked() {
        if (!Bank.isOpen()) {
            GameObject bankBooth = GameObjects.newQuery().actions("Bank").results().nearest();
            if (bankBooth != null && bankBooth.interact("Bank")) {
                Execution.delayUntil(Bank::isOpen,
                        (int)getGaussian(2000, 5000, 3500, 1000));
            }
        }

        if (Bank.isOpen()) {
            boolean rawFoodInBank = Bank.contains(settings.getFoodType().getRawName());
            if (!rawFoodInBank) {
                System.out.println("All food is cooked. No raw food left in the bank.");
                return true;
            }
        }
        return false;
    }

    private void cookFood() {
        Area cookingArea = settings.getLocation().getArea();
        Coordinate lower = Objects.requireNonNull(cookingArea.getArea()).getBottomLeft();
        Coordinate upper = cookingArea.getArea().getTopRight();

        Coordinate expandedLower = new Coordinate(lower.getX() - 2, lower.getY() - 2, lower.getPlane());
        Coordinate expandedUpper = new Coordinate(upper.getX() + 2, upper.getY() + 2, upper.getPlane());

        GameObject cookingObject = GameObjects.newQuery()
                .actions("Cook")
                .within(new Area.Rectangular(expandedLower, expandedUpper))
                .results()
                .nearest();

        if (cookingObject != null && cookingObject.isVisible()) {
            System.out.println("🍳 Cooking object found: " + cookingObject + ". Attempting to cook...");

            if (shouldMisclick()) {
                System.out.println("🤖 Simulating misclick...");
                misclickNear(cookingObject.getPosition());
                System.out.println("🔄 Correcting misclick...");
            }

            Execution.delay((int)getGaussian(500, 800, 650, 100));

            if (cookingObject.interact("Cook")) {
                Execution.delay((int)getGaussian(4000, 5100, 4500, 400));

                System.out.println("📋 Cooking interface appeared");
                Execution.delay((int)getGaussian(300, 800, 550, 150));

                if (Random.nextInt(100) < 4) {
                    System.out.println("⌨️ Simulating keypress fumble...");
                    Keyboard.typeKey(KeyEvent.VK_SHIFT);
                    Execution.delay((int)getGaussian(200, 400, 300, 70));
                }

                Keyboard.typeKey(KeyEvent.VK_SPACE);
                System.out.println("✅ Confirmed cooking");

                boolean startedCooking = Execution.delayUntil(
                        () -> Players.getLocal().getAnimationId() != -1,
                        (int)getGaussian(800, 1500, 1150, 250),
                        (int)getGaussian(3000, 5000, 4000, 700)
                );

                if (startedCooking) {
                    System.out.println("🔥 Cooking started");
                    while (Players.getLocal().getAnimationId() != -1) {
                        Execution.delay((int)getGaussian(1000, 2000, 1500, 300));
                        antiBan.maybePerformAntiBan();
                        if (Random.nextInt(100) < 1) {
                            System.out.println("🚪 Simulating early exit");
                            break;
                        }
                    }
                } else {
                    System.out.println("⚠️ Failed to start cooking");
                    Execution.delay((int)getGaussian(2000, 3000, 2500, 350));
                }
            }
        } else {
            System.out.println("🚶 Cooking object not found or not visible. Walking to cooking area.");
            if (!cookingArea.contains(Players.getLocal())) {
                navigation.walkToArea(cookingArea, pathfinder);
            }
            Execution.delay((int)getGaussian(600, 1200, 900, 200));
        }
    }

    private void walkToBankAndWithdraw() {
        Area closestBank = cookingLocationToBank.get(settings.getLocation());
        if (closestBank == null) {
            System.out.println("❌ No associated bank area found for location: " + settings.getLocation());
            return;
        }

        if (Bank.isOpen()) {
            System.out.println("✅ Bank opened. Depositing inventory.");
            Execution.delay((int)getGaussian(300, 800, 550, 150));
            Bank.depositInventory();

            Execution.delay((int)getGaussian(1000, 2400, 1700, 500));
            String rawFoodName = settings.getFoodType().getRawName();
            System.out.println("🎣 Withdrawing raw food: " + rawFoodName);
            Bank.withdraw(rawFoodName, 0);

            Execution.delayUntil(() -> Inventory.contains(rawFoodName),
                    (int)getGaussian(2000, 4000, 3000, 700));

            if (!Inventory.contains(rawFoodName)) {
                System.out.println("❌ Withdrawal failed or no raw food left.");
            } else {
                System.out.println("✅ Withdrawal successful.");
            }

            Execution.delay((int)getGaussian(1000, 2000, 1500, 100));
            System.out.println("🔐 Closing bank.");
            Bank.close();
            Execution.delay((int)getGaussian(400, 1000, 700, 200));
        } else {
            System.out.println("🏦 Bank is closed. Attempting to open bank.");
            Execution.delay((int)getGaussian(400, 800, 600, 150));

            GameObject bank = GameObjects.newQuery().actions("Bank").results().nearest();
            if (bank != null) {
                Execution.delay((int)getGaussian(2500, 3500, 3000, 350));
                if (bank.interact("Bank")) {
                    Execution.delayUntil(Bank::isOpen,
                            (int)getGaussian(2000, 5000, 3500, 1000));
                } else {
                    System.out.println("⚠️ Failed to interact with bank object.");
                }
            } else {
                System.out.println("❌ No bank object found nearby.");
            }
        }

        if (!closestBank.contains(Players.getLocal())) {
            navigation.walkToArea(closestBank, pathfinder);
        }
    }

    @Override
    public void onStop() {
        RuneScape.logout();
        System.out.println("Cooker stopped.");
    }

    @Override
    public void onSettingChanged(SettingChangedEvent settingChangedEvent) {}

    @Override
    public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }
}