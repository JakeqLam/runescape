package com.runemate.party.crafting;

import com.runemate.commons.internal.hud.components.Widget;
import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.Item;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.local.hud.interfaces.InterfaceComponent;
import com.runemate.game.api.hybrid.local.hud.interfaces.Interfaces;
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
import com.runemate.pathfinder.Pathfinder;
import com.runemate.ui.setting.annotation.open.SettingsProvider;
import com.runemate.game.api.hybrid.input.Mouse;

public class GenericCrafter extends LoopingBot implements SettingsListener {

    @SettingsProvider(updatable = true)
    private CraftingSettings settings;

    private Pathfinder pathfinder;
    private AntiBan antiBan;
    private long nextBreakTime;

    private static final String REQUIRED_GOLD_NAME = "Coins";
    private static final int REQUIRED_GOLD_AMOUNT = 20;

    private long lastAntiBanTime = 0;
    private long nextAntiBanCooldown = getRandomCooldown();

    private long getRandomCooldown() {
        return Random.nextInt(10_000, 100_000); // Between 10s and 100s
    }

    private void maybePerformAntiBan() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAntiBanTime >= nextAntiBanCooldown && Random.nextInt(100) < 3) {
            System.out.println("[SimpleFisher] Performing anti-ban");
            antiBan.performAntiBan();
            lastAntiBanTime = currentTime;
            nextAntiBanCooldown = getRandomCooldown(); // Set a new random delay
        }
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
        getEventDispatcher().addListener(this);
        nextBreakTime = System.currentTimeMillis() + (Random.nextInt(10000, 11000) * 1000L);
        System.out.println("GenericCrafter has started.");
    }

    @Override
    public void onLoop() {
        System.out.println("Loop start.");

        if (!Players.getLocal().isVisible()) {
            System.out.println("Player not visible. Waiting...");
            Execution.delay(300, 600);
            return;
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
            walkToArea(tanningArea);
            return;
        }

        // Tan hides
        if (Inventory.contains(settings.getHideType().getRawName()) && Inventory.getQuantity(REQUIRED_GOLD_NAME) >= REQUIRED_GOLD_AMOUNT) {
            System.out.println("Trying to tan hides...");

            var tanner = Npcs.newQuery().names("Ellis", "Sbott").actions("Trade").results().nearest();
            if (tanner != null && tanner.interact("Trade")) {
                System.out.println("Interacted with tanner NPC, waiting for interface...");
                Execution.delayUntil(() -> Interfaces.newQuery().texts("Tan All").results().first() != null, 3000);

                Execution.delayUntil(() -> Interfaces.newQuery().containers(324).results().first() != null, 3000);

                InterfaceComponent hideComponent = Interfaces.newQuery()
                        .containers(324) // Tanning interface container ID
                        .actions("Tan All") // Looks for components with "Tan All" right-click option
                        .names(settings.getHideType().getTannedName()) // Match the tanned hide name, e.g., "Green dragon leather"
                        .results()
                        .first();

                if (hideComponent != null && hideComponent.interact("Tan All")) {
                    System.out.println("Clicked 'Tan All' on " + settings.getHideType().getTannedName());
                    Execution.delayUntil(() -> !Inventory.contains(settings.getHideType().getRawName()), 5000);
                    System.out.println("Tanning completed.");
                    Execution.delay(Random.nextInt(500, 800), Random.nextInt(800, 1200));

                    if (shouldMisclick()) {
                        System.out.println("[GenericCrafter] Misclicking near tanner...");
                        misclickNear(tanner.getPosition());
                    }
                } else {
                    System.out.println("Could not find or interact with tanned hide option.");
                }
            } else {
                System.out.println("Tanner NPC not found or interaction failed.");
            }
        }

        // After tanning, navigate back to the bank
        System.out.println("Tanning complete, walking back to the bank...");
        while (!BANK_AREA.contains(Players.getLocal()))
            walkToArea(BANK_AREA);

        // Anti-ban randomly
        maybePerformAntiBan();

        // Break logic
        if (System.currentTimeMillis() >= nextBreakTime) {
            int duration = Random.nextInt(settings.getBreakMinLength(), settings.getBreakMaxLength() + 1);
            System.out.println("[GenericCrafter] Taking break for " + duration + "s");
            if (RuneScape.isLoggedIn()) RuneScape.logout(true);
            Execution.delay(duration * 1000);
            Execution.delayUntil(RuneScape::isLoggedIn, 5000, 30000);
            scheduleNextBreak();
            return;
        }

        System.out.println("Loop end.");
    }

    private void scheduleNextBreak() {
        int interval = Random.nextInt(settings.getBreakMin(), settings.getBreakMax() + 1);
        nextBreakTime = System.currentTimeMillis() + interval * 1000L;
        System.out.println("[GenericCrafter] Next break in " + interval + "s");
    }

    private void walkToArea(Area area) {
        if (area.contains(Players.getLocal())) return;

        System.out.println("[GenericCrafter] Walking to area: " + area);

        Coordinate destination = area.getRandomCoordinate();
        Path path = pathfinder.pathBuilder()
                .destination(destination)
                .enableHomeTeleport(false)
                .enableTeleports(false)
                .preferAccuracy()
                .findPath();

        if (path != null && path.step()) {
            System.out.println("[GenericCrafter] Path found using Pathfinder.");
            Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1500);
            Execution.delay(Random.nextInt(300, 600), Random.nextInt(600, 900));  // Added random delay
        } else {
            System.out.println("[GenericCrafter] Pathfinder failed, attempting Bresenham fallback...");

            BresenhamPath fb = BresenhamPath.buildTo(destination);
            if (fb != null && fb.step()) {
                System.out.println("[GenericCrafter] Bresenham path successful.");
                Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1500);
                Execution.delay(Random.nextInt(300, 600), Random.nextInt(600, 900));  // Added random delay
            } else {
                System.out.println("[GenericCrafter] Both pathfinding methods failed, clicking nearby random tile.");
                Coordinate playerPos = Players.getLocal().getPosition();

                for (int i = 0; i < 10; i++) {
                    int dx = Random.nextInt(1, 3);
                    int dy = Random.nextInt(1, 3);
                    Coordinate randomTile = playerPos.randomize(dx, dy);

                    if (randomTile.isReachable()) {
                        randomTile.interact("Walk here");
                        System.out.println("[GenericCrafter] Clicking fallback tile: " + randomTile);
                        Execution.delayUntil(() -> Players.getLocal().isMoving(), 300, 1500);
                        break;
                    }
                }
            }
        }
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
    public void onSettingsConfirmed() {}
}
