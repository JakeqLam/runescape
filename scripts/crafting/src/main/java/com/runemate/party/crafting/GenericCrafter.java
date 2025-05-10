package com.runemate.party.crafting;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.Item;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.interfaces.*;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.basic.BresenhamPath;
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
import com.runemate.game.api.hybrid.input.Mouse;

import java.awt.event.KeyEvent;

public class GenericCrafter extends LoopingBot implements SettingsListener {

    @SettingsProvider(updatable = true)
    private CraftingSettings settings;

    private Pathfinder pathfinder;
    private AntiBan antiBan;
    private boolean settingsConfirmed;

    private static final String REQUIRED_GOLD_NAME = "Coins";
    private static final int REQUIRED_GOLD_AMOUNT = 20;
    private GPTNavigation navigation;

    // Gaussian random number within bounds (min, max) with mean and std deviation
    private double getGaussian(double min, double max, double mean, double stdDev) {
        double value;
        do {
            value = Random.nextGaussian(min, max, mean, stdDev);
        } while (value < min || value > max);
        return value;
    }

    private boolean hasMaterialsForCrafting() {
        return Inventory.contains("Needle") &&
                Inventory.contains("Thread") &&
                Inventory.contains(settings.getLeatherProduct().getHideType());
    }

    private boolean hasMaterialsForTanning() {
        return Inventory.contains(settings.getHideType().getRawName()) &&
                Inventory.getQuantity(REQUIRED_GOLD_NAME) >= REQUIRED_GOLD_AMOUNT;
    }

    private Area getTanningArea() {
        return settings.getLocation().getArea();
    }

    private static final Area BANK_AREA = new Area.Rectangular(new Coordinate(3270, 3166, 0), new Coordinate(3269, 3161, 0));

    @Override
    public void onStart(String... args) {
        pathfinder = Pathfinder.create(this);
        antiBan = new AntiBan();
        navigation = new GPTNavigation();
        getEventDispatcher().addListener(this);
        System.out.println("GenericCrafter has started.");
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) {
            return;
        }

        System.out.println("Loop start.");

        if (!Players.getLocal().isVisible()) {
            System.out.println("Player not visible. Waiting...");
            Execution.delay((int)getGaussian(300, 600, 450, 100));
            return;
        }

        if (settings.isCraftingEnabled()) {
            handleLeatherCrafting();
        } else {
            handleTanning();
        }

        antiBan.maybePerformAntiBan();
        antiBan.performBreakLogic(settings.getBreakMin(), settings.getBreakMax());

        System.out.println("Loop end.");
    }

    private void handleTanning() {
        if (!hasMaterialsForTanning()) {
            System.out.println("[GenericCrafter] No more hides or coins available. Stopping bot.");
            stop("No more materials to tan.");
        }

        boolean hasHide = Inventory.contains(settings.getHideType().getRawName());
        boolean hasCoins = Inventory.getQuantity(REQUIRED_GOLD_NAME) >= REQUIRED_GOLD_AMOUNT;

        if (!hasHide || !hasCoins) {
            System.out.println("Missing required materials. Opening bank...");
            if (!Bank.isOpen()) {
                if (Bank.open()) {
                    Execution.delayUntil(Bank::isOpen, (int)getGaussian(2000, 3000, 2500, 300));
                    System.out.println("Bank opened.");
                    Execution.delay((int)getGaussian(500, 800, 650, 100));
                } else {
                    System.out.println("Failed to open bank.");
                    return;
                }
            }

            if (Inventory.containsAnyExcept(settings.getHideType().getRawName(), REQUIRED_GOLD_NAME)) {
                System.out.println("Depositing all except hides and coins...");
                Bank.depositInventory();
                Execution.delay((int)getGaussian(500, 800, 650, 100));
            }

            if (!hasCoins) {
                int requiredGoldAmount = settings.getRequiredGoldAmount();
                int randomizedAmount = requiredGoldAmount * 10 + (int)getGaussian(-5, 5, 0, 3);

                Item coinsInBank = Bank.newQuery().names(REQUIRED_GOLD_NAME).results().first();
                if (coinsInBank != null && coinsInBank.getQuantity() >= randomizedAmount) {
                    Bank.withdraw(REQUIRED_GOLD_NAME, 1000);
                    Execution.delayUntil(() -> Inventory.contains(REQUIRED_GOLD_NAME),
                            (int)getGaussian(2000, 3000, 2500, 300));
                    System.out.println("Withdrew coins from bank.");
                    Execution.delay((int)getGaussian(500, 800, 650, 100));
                } else {
                    System.out.println("Not enough coins in bank.");
                }
            }

            if (!hasHide) {
                Item hide = Bank.newQuery().names(settings.getHideType().getRawName()).results().first();
                if (hide != null) {
                    Bank.withdraw(settings.getHideType().getRawName(), 0);
                    Execution.delayUntil(() -> Inventory.contains(settings.getHideType().getRawName()),
                            (int)getGaussian(2000, 3000, 2500, 300));
                    System.out.println("Withdrew hides from bank.");
                    Execution.delay((int)getGaussian(500, 800, 650, 100));
                } else {
                    System.out.println("No hides in bank.");
                }
            }

            Execution.delay((int)getGaussian(1000, 2000, 1500, 100));

            Bank.close();
            Execution.delayUntil(() -> !Bank.isOpen(), (int)getGaussian(2000, 3000, 2500, 300));
            return;
        }

        Area tanningArea = getTanningArea();
        if (!tanningArea.contains(Players.getLocal())) {
            navigation.walkToArea(tanningArea, pathfinder);
            return;
        }

        if (Inventory.contains(settings.getHideType().getRawName()) &&
                Inventory.getQuantity(REQUIRED_GOLD_NAME) >= REQUIRED_GOLD_AMOUNT) {

            System.out.println("Trying to tan hides...");

            Npc tanner = null;
            for (int i = 0; i < 3 && tanner == null; i++) {
                tanner = Npcs.newQuery()
                        .names("Ellis", "Sbott")
                        .results()
                        .nearest();

                if (tanner == null) {
                    System.out.println("Attempt " + (i + 1) + ": Tanner not found, retrying...");
                    Execution.delay((int)getGaussian(2000, 3500, 2750, 500));
                }
            }

            if (tanner != null) {
                System.out.println("Interacted with tanner NPC, waiting for interface...");
                Execution.delay((int)getGaussian(500, 800, 650, 100));

                if (!tanner.isVisible()) {
                    Camera.turnTo(tanner);
                    Execution.delay((int)getGaussian(500, 800, 650, 100));
                }

                if (Players.getLocal().distanceTo(tanner) > 5) {
                    tanner.getPosition().interact("Walk here");
                    Npc finalTanner = tanner;
                    Execution.delayUntil(() -> Players.getLocal().distanceTo(finalTanner.getPosition()) <= 4,
                            (int)getGaussian(3000, 5000, 4000, 700));
                }

                boolean interacted = false;
                for (int i = 0; i < 3 && !interacted; i++) {
                    interacted = tanner.interact("Trade");
                    if (!interacted) {
                        System.out.println("Failed to interact on attempt " + (i + 1));
                        Execution.delay((int)getGaussian(500, 1000, 750, 150));
                    }
                }

                if (!interacted) {
                    System.out.println("❌ Failed to interact with tanner after retries.");
                    return;
                }

                Execution.delay((int)getGaussian(2000, 3500, 2750, 500));

                InterfaceComponent hideComponent = Interfaces.getAt(324, settings.getHideType().getComponentId());
                Execution.delay((int)getGaussian(400, 700, 550, 100));

                if (hideComponent != null && hideComponent.isVisible() && hideComponent.interact("Tan All")) {
                    System.out.println("Clicked 'Tan All' on " + settings.getHideType().getTannedName());
                    Execution.delayUntil(() ->
                                    !Inventory.contains(settings.getHideType().getRawName()),
                            (int)getGaussian(5000, 7000, 6000, 700));

                    System.out.println("Tanning completed.");
                    Execution.delay((int)getGaussian(500, 800, 650, 100));

                    if (shouldMisclick()) {
                        System.out.println("[GenericCrafter] Misclicking near tanner...");
                        misclickNear(tanner.getPosition());
                    }
                } else {
                    System.out.println("Failed to interact with 'Tan All' option.");
                }
            } else {
                System.out.println("Tanner NPC not found or interaction failed.");
            }
        }

        System.out.println("Tanning complete, walking back to the bank...");
        while (!BANK_AREA.contains(Players.getLocal()))
            navigation.walkToArea(BANK_AREA, pathfinder);
    }

    private void handleLeatherCrafting() {
        Execution.delay((int)getGaussian(2000, 4000, 3000, 700));

        LeatherProduct type = settings.getLeatherProduct();
        String leatherName = type.getHideType();

        if (!Inventory.contains("Needle") || !Inventory.contains("Thread") || !Inventory.contains(leatherName)) {
            System.out.println("Missing materials. Banking...");
            bankForLeatherCrafting(leatherName);
            return;
        }

        SpriteItem needle = Inventory.getItems("Needle").first();
        SpriteItem leather = Inventory.getItems(i -> i.getDefinition().getName().equals(leatherName)).first();

        if (needle != null && leather != null) {
            if (needle.interact("Use")) {
                Execution.delayUntil(() -> Inventory.isItemSelected(),
                        (int)getGaussian(500, 1500, 1000, 300));

                if (leather.interact("Use")) {
                    Execution.delayUntil(() -> {
                        InterfaceComponent chatbox = Interfaces.newQuery().textContains("chaps", "vambraces", "cowl").results().first();
                        return chatbox != null && chatbox.isVisible();
                    }, (int)getGaussian(2500, 3800, 3150, 400));

                    switch (type) {
                        case GLOVES: Keyboard.pressKey(KeyEvent.VK_1); break;
                        case BOOTS: Keyboard.pressKey(KeyEvent.VK_2); break;
                        case COWL: Keyboard.pressKey(KeyEvent.VK_3); break;
                        case COIF: Keyboard.pressKey(KeyEvent.VK_4); break;
                        case VAMBRACES: Keyboard.pressKey(KeyEvent.VK_5); break;
                        case CHAPS: Keyboard.pressKey(KeyEvent.VK_6); break;
                        case BODY: Keyboard.pressKey(KeyEvent.VK_7); break;
                        case SHIELD: Keyboard.pressKey(KeyEvent.VK_8); break;
                    }

                    long startTime = System.currentTimeMillis();
                    long duration = (long)getGaussian(15000, 18000, 16500, 1000);
                    while (System.currentTimeMillis() - startTime < duration) {
                        Execution.delay((int)getGaussian(600, 1200, 900, 200));
                        antiBan.maybePerformAntiBan();
                    }
                }
            }
        }
    }

    private void bankForLeatherCrafting(String leatherName) {
        if (!hasMaterialsForCrafting()) {
            System.out.println("[GenericCrafter] No more materials to craft leather. Stopping bot.");
            stop("Out of crafting materials.");
        }

        if (!Bank.isOpen()) {
            if (!Bank.open()) return;
            Execution.delayUntil(Bank::isOpen, (int)getGaussian(2000, 3000, 2500, 300));
        }

        Bank.depositInventory();

        if (!Inventory.contains("Needle")) {
            Bank.withdraw("Needle", 1);
            Execution.delayUntil(() -> Inventory.contains("Needle"),
                    (int)getGaussian(2000, 3000, 2500, 300));
        }

// Withdraw Thread with randomized amount
        if (!Inventory.contains("Thread")) {
            int threadAmount = (int) getGaussian(24, 28, 27, 1); // Withdraw 27 ± 1
            Bank.withdraw("Thread", threadAmount);
            Execution.delayUntil(() -> Inventory.contains("Thread"),
                    (int) getGaussian(2000, 3000, 2500, 300));
        }

        Bank.withdraw(leatherName, 0);
        Execution.delayUntil(() -> Inventory.contains(leatherName),
                (int) getGaussian(2000, 3000, 2500, 300));

        Execution.delay((int)getGaussian(1000, 2000, 1500, 100));

        Bank.close();
        Execution.delayUntil(() -> !Bank.isOpen(),
                (int)getGaussian(2000, 3000, 2500, 300));
    }

    private boolean shouldMisclick() {
        return Random.nextInt(100) < 5; // 5% chance to misclick
    }

    private void misclickNear(Coordinate base) {
        Coordinate offset = new Coordinate(
                base.getX() + (int)getGaussian(-2, 3, 0, 1.5),
                base.getY() + (int)getGaussian(-2, 3, 0, 1.5),
                base.getPlane()
        );
        Mouse.move(offset);
        Execution.delay((int)getGaussian(100, 300, 200, 70));
        Mouse.click(Mouse.Button.LEFT);
        Execution.delay((int)getGaussian(300, 700, 500, 150));
    }

    @Override
    public void onStop() {
        System.out.println("GenericCrafter has stopped.");
    }

    @Override
    public void onSettingChanged(SettingChangedEvent settingChangedEvent) {}

    @Override
    public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }
}