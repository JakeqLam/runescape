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

    private boolean hasMaterialsForCrafting() {
        return Inventory.contains("Needle") &&
                Inventory.contains("Thread") &&
                Inventory.contains(settings.getLeatherProduct().getHideType());
    }

    private boolean hasMaterialsForTanning() {
        return Inventory.contains(settings.getHideType().getRawName()) &&
                Inventory.getQuantity(REQUIRED_GOLD_NAME) >= REQUIRED_GOLD_AMOUNT;
    }


    // Get tanning area using the TanningLocation enum
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
            Execution.delay(300, 600);
            return;
        }

        if (settings.isCraftingEnabled()) {
            handleLeatherCrafting();
        } else {
            handleTanning();
        }

        // Anti-ban randomly
        antiBan.maybePerformAntiBan();

        // Break logic
        antiBan.performBreakLogic(settings.getBreakMin(),settings.getBreakMax());

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
                    Execution.delayUntil(Bank::isOpen, 2000);
                    System.out.println("Bank opened.");
                    Execution.delay(Random.nextInt(500, 800), Random.nextInt(800, 1200));  // Added random delay
                } else {
                    System.out.println("Failed to open bank.");
                    return;
                }
            }

            // Deposit all to make inventory clean
            if (Inventory.containsAnyExcept(settings.getHideType().getRawName(), REQUIRED_GOLD_NAME)) {
                System.out.println("Depositing all except hides and coins...");
                Bank.depositInventory();
                Execution.delay(500, 800);
            }

            // Withdraw coins if needed
            if (!hasCoins) {
                int requiredGoldAmount = settings.getRequiredGoldAmount(); // Get the required gold amount from settings

                // Randomize the required gold amount with a small variance
                int randomizedAmount = requiredGoldAmount * 10 + Random.nextInt(-5, 5); // Adding a random variation of -5 to +5 coins

                Item coinsInBank = Bank.newQuery().names(REQUIRED_GOLD_NAME).results().first();
                if (coinsInBank != null && coinsInBank.getQuantity() >= randomizedAmount) {
                    Bank.withdraw(REQUIRED_GOLD_NAME, 1000);
                    Execution.delayUntil(() -> Inventory.contains(REQUIRED_GOLD_NAME), 2000);
                    System.out.println("Withdrew coins from bank.");
                    Execution.delay(Random.nextInt(500, 800), Random.nextInt(800, 1200));  // Added random delay
                } else {
                    System.out.println("Not enough coins in bank.");
                }
            }

            // Withdraw hides
            if (!hasHide) {
                Item hide = Bank.newQuery().names(settings.getHideType().getRawName()).results().first();
                if (hide != null) {
                    // Withdraw 27 hides, leaving 1 slot for coins
                    Bank.withdraw(settings.getHideType().getRawName(), 27);
                    Execution.delayUntil(() -> Inventory.contains(settings.getHideType().getRawName()), 2000);
                    System.out.println("Withdrew hides from bank.");
                    Execution.delay(Random.nextInt(500, 800), Random.nextInt(800, 1200));  // Added random delay
                } else {
                    System.out.println("No hides in bank.");
                }
            }

            Bank.close();
            Execution.delayUntil(() -> !Bank.isOpen(), 2000);
            return;
        }

        // Walk to tanner
        Area tanningArea = getTanningArea();
        if (!tanningArea.contains(Players.getLocal())) {
            navigation.walkToArea(tanningArea, pathfinder);
            return;
        }

// Tan hides
        if (Inventory.contains(settings.getHideType().getRawName()) &&
                Inventory.getQuantity(REQUIRED_GOLD_NAME) >= REQUIRED_GOLD_AMOUNT) {

            System.out.println("Trying to tan hides...");

            // Try up to 3 times to find the tanner
            Npc tanner = null;
            for (int i = 0; i < 3 && tanner == null; i++) {
                tanner = Npcs.newQuery()
                        .names("Ellis", "Sbott")
                        .results()
                        .nearest();

                if (tanner == null) {
                    System.out.println("Attempt " + (i + 1) + ": Tanner not found, retrying...");
                    Execution.delay(2000, 3500);
                }
            }

            if (tanner != null) {
                System.out.println("Interacted with tanner NPC, waiting for interface...");

                // Extra wait to ensure full load
                Execution.delay(500, 800);

                if (!tanner.isVisible()) {
                    Camera.turnTo(tanner);
                    Execution.delay(500, 800);
                }

                if (Players.getLocal().distanceTo(tanner) > 5) {
                    tanner.getPosition().interact("Walk here");
                    Npc finalTanner = tanner;
                    Execution.delayUntil(() -> Players.getLocal().distanceTo(finalTanner.getPosition()) <= 4, 3000);
                }

                boolean interacted = false;
                for (int i = 0; i < 3 && !interacted; i++) {
                    interacted = tanner.interact("Trade");
                    if (!interacted) {
                        System.out.println("Failed to interact on attempt " + (i + 1));
                        Execution.delay(500, 1000);
                    }
                }

                if (!interacted) {
                    System.out.println("❌ Failed to interact with tanner after retries.");
                    return;
                }

                Execution.delay(2000, 3500);

                InterfaceComponent hideComponent = Interfaces.getAt(324, settings.getHideType().getComponentId());
                Execution.delay(400, 700);

                if (hideComponent != null && hideComponent.isVisible() && hideComponent.interact("Tan All")) {
                    System.out.println("Clicked 'Tan All' on " + settings.getHideType().getTannedName());
                    Execution.delayUntil(() ->
                            !Inventory.contains(settings.getHideType().getRawName()), 5000);

                    System.out.println("Tanning completed.");
                    Execution.delay(Random.nextInt(500, 800), Random.nextInt(800, 1200));

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

        // After tanning, navigate back to the bank
        System.out.println("Tanning complete, walking back to the bank...");
        while (!BANK_AREA.contains(Players.getLocal()))
            navigation.walkToArea(BANK_AREA, pathfinder);
    }

    private void handleLeatherCrafting() {

        Execution.delay(2000, 4000);

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
            // Use needle
            if (needle.interact("Use")) {
                Execution.delayUntil(() -> Inventory.isItemSelected(), 500, 1500);

                // Use it on the leather
                if (leather.interact("Use")) {
                    Execution.delayUntil(() -> {
                        InterfaceComponent chatbox = Interfaces.newQuery().textContains("chaps", "vambraces", "cowl").results().first();
                        return chatbox != null && chatbox.isVisible();
                    }, 2500, 3800);

                    // Simulate selecting the correct product based on the LeatherProduct
                    switch (type) {
                        case GLOVES:
                            Keyboard.pressKey(KeyEvent.VK_1); // Select Gloves
                            break;
                        case BOOTS:
                            Keyboard.pressKey(KeyEvent.VK_2); // Select Boots
                            break;
                        case COWL:
                            Keyboard.pressKey(KeyEvent.VK_3); // Select Cowl
                            break;
                        case COIF:
                            Keyboard.pressKey(KeyEvent.VK_4); // Select Coif
                            break;
                        case VAMBRACES:
                            Keyboard.pressKey(KeyEvent.VK_5); // Select Vambraces
                            break;
                        case CHAPS:
                            Keyboard.pressKey(KeyEvent.VK_6); // Select Chaps
                            break;
                        case BODY:
                            Keyboard.pressKey(KeyEvent.VK_7); // Select Body
                            break;
                        case SHIELD:
                            Keyboard.pressKey(KeyEvent.VK_8); // Select Shield
                            break;
                    }

                    // Wait while crafting, periodically do antiban
                    long startTime = System.currentTimeMillis();
                    long duration = Random.nextInt(15000, 18000);
                    while (System.currentTimeMillis() - startTime < duration) {
                        Execution.delay(600, 1200);
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
            Execution.delayUntil(Bank::isOpen, 2000);
        }

        Bank.depositInventory();

        if (!Inventory.contains("Needle")) {
            Bank.withdraw("Needle", 1);
            Execution.delayUntil(() -> Inventory.contains("Needle"), 2000);
        }

        if (!Inventory.contains("Thread")) {
            Bank.withdraw("Thread", 27);
            Execution.delayUntil(() -> Inventory.contains("Thread"), 2000);
        }

        Bank.withdraw(leatherName, 26);
        Execution.delayUntil(() -> Inventory.contains(leatherName), 2000);

        Bank.close();
        Execution.delayUntil(() -> !Bank.isOpen(), 2000);
    }

    private boolean shouldMisclick() {
        return Random.nextInt(100) < 5; // 5% chance to misclick
    }

    private void misclickNear(Coordinate base) {
        Coordinate offset = new Coordinate(
                base.getX() + Random.nextInt(-2, 3),
                base.getY() + Random.nextInt(-2, 3),
                base.getPlane()
        );
        Mouse.move(offset);
        Execution.delay(100, 300);
        Mouse.click(Mouse.Button.LEFT);
        Execution.delay(300, 700);
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
