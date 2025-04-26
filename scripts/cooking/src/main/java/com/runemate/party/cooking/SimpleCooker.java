package com.runemate.party.cooking;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.basic.BresenhamPath;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.LoopingBot;
import com.runemate.game.api.script.framework.listeners.SettingsListener;
import com.runemate.game.api.script.framework.listeners.events.SettingChangedEvent;
import com.runemate.party.common.AntiBan;
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
    private long nextBreakTime;
    AntiBan antiBan;
    private boolean settingsConfirmed;

    static {
        // F2P
        cookingLocationToBank.put(CookingLocation.AL_KHARID, new Area.Rectangular(new Coordinate(3269, 3167, 0), new Coordinate(3271, 3165, 0))); // Al Kharid Bank
//        cookingLocationToBank.put(CookingLocation.VARROCK_EAST, new Area.Rectangular(new Coordinate(3250, 3420, 0), new Coordinate(3253, 3418, 0))); // Varrock East Bank
//        cookingLocationToBank.put(CookingLocation.LUMBRIDGE_CASTLE, new Area.Rectangular(new Coordinate(3207, 3220, 2), new Coordinate(3209, 3218, 2))); // Lumbridge Top Floor Bank
//        cookingLocationToBank.put(CookingLocation.EDGEVILLE_FIRE, new Area.Rectangular(new Coordinate(3092, 3499, 0), new Coordinate(3094, 3497, 0))); // Edgeville Bank

        // P2P
        cookingLocationToBank.put(CookingLocation.ROGUES_DEN, new Area.Rectangular(new Coordinate(3044, 4971, 1), new Coordinate(3046, 4969, 1))); // Rogues' Den Bank
        cookingLocationToBank.put(CookingLocation.HOSIDIUS_KITCHEN, new Area.Rectangular(new Coordinate(1714, 3616, 0), new Coordinate(1716, 3614, 0))); // Hosidius Bank
        cookingLocationToBank.put(CookingLocation.CATHERBY_RANGE, new Area.Rectangular(new Coordinate(2808, 3441, 0), new Coordinate(2810, 3439, 0))); // Catherby Bank
        cookingLocationToBank.put(CookingLocation.COOKING_GUILD, new Area.Rectangular(new Coordinate(3209, 3217, 1), new Coordinate(3211, 3215, 1))); // Cooking Guild Bank
    }

    @Override
    public void onStart(String... args) {
        // Initialize settings UI and listener
        antiBan= new AntiBan();
        getEventDispatcher().addListener(this);
        // Populate initial values
        nextBreakTime = System.currentTimeMillis() + (7000 * 1000);
        pathfinder = Pathfinder.create(this);
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) {
            return;
        }

        if (Players.getLocal() == null || !RuneScape.isLoggedIn()) return;

        // 1) Break logic
        if (System.currentTimeMillis() >= nextBreakTime) {
            int breakSeconds = Random.nextInt(settings.getBreakMinLength(), settings.getBreakMaxLength() + 1);
            System.out.println("Scheduled break triggered. Logging out for " + breakSeconds + " seconds.");
            if (RuneScape.isLoggedIn()) {
                RuneScape.logout(true);
            }
            Execution.delay(breakSeconds * 1000); // Wait during logout
            Execution.delayUntil(RuneScape::isLoggedIn, 5000, 30000); // waits up to 30 seconds for auto-login
            System.out.println("Back from break. Logged in again.");
            scheduleNextBreak();
        }

        if (Random.nextInt(100) < 10) { // 10% chance
            System.out.println("Performing anti-ban action.");
            antiBan.performAntiBan();
        }

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
                Execution.delayUntil(Bank::isOpen, 2000, 5000);
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

    public void fallBack(Area target) {
        // Fallback: Click a nearby walkable tile using Interactable
        Path path = BresenhamPath.buildTo(target.getCenter().randomize(10,10));
        if (path != null && !target.contains(Players.getLocal())) {
            path.step();
            Execution.delay(800, 1500);
        } else {
            Coordinate start = Players.getLocal().getPosition();

            if (start == null) {
                System.out.println("❌ Invalid start or target coordinate.");
                return;
            }

            // Calculate 1/3rd point between start and target
            int newX = start.getX() + (target.getCenter().getX() - start.getX()) / 3;
            int newY = start.getY() + (target.getCenter().getY() - start.getY()) / 3;
            Coordinate oneThird = new Coordinate(newX, newY, start.getPlane());

            // Randomize slightly to avoid exact clicks
            Coordinate destination = oneThird.randomize(1, 1);

            path = pathfinder.pathBuilder().destination(destination)
                    .enableHomeTeleport(false)
                    .avoidWilderness(true)
                    .preferAccuracy().findPath();
            if (path != null && path.isValid() && !target.contains(Players.getLocal())) {
                if (path.step()) {
                    System.out.println("🚶 Stepping to fallback (1/3rd point): " + destination);
                    Execution.delay(800, 1500);
                } else {
                    System.out.println("⚠️ Failed to step to fallback destination.");
                }
            } else {
                System.out.println("❌ Fallback path is invalid.");
            }
        }
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

            // --- MISCLICK SIMULATION (15% chance) ---
            if (Random.nextInt(0, 100) < 15) {
                System.out.println("🤖 Simulating misclick...");

                // Misclick near the object but still inside the building
                Coordinate objectTile = cookingObject.getPosition();
                Coordinate misclickTile = null;

                int tries = 0;
                while (misclickTile == null && tries < 10) {
                    Coordinate attempt = objectTile.randomize(2, 2);
                    if (cookingArea.contains(attempt)) {
                        misclickTile = attempt;
                    }
                    tries++;
                }

                if (misclickTile != null) {
                    Mouse.move(misclickTile);
                    Execution.delay(Random.nextInt(150, 300));
                    Mouse.click(Mouse.Button.LEFT);

                    Execution.delay(Random.nextInt(800, 1200));
                    System.out.println("🔄 Correcting misclick...");
                }
            }

            // --- MAIN COOKING LOGIC ---
            Execution.delay(Random.nextInt(500, 800)); // Human reaction delay

            if (cookingObject.interact("Cook")) {

                Execution.delay(Random.nextInt(4000, 5100)); // Human reaction delay

                    System.out.println("📋 Cooking interface appeared");

                    // Random delay before pressing space (human hesitation)
                    Execution.delay(Random.nextInt(300, 800));

                    // Simulate potential keypress fumble (4% chance)
                    if (Random.nextInt(0,100) < 4) {
                        System.out.println("⌨️ Simulating keypress fumble...");
                        Keyboard.typeKey(KeyEvent.VK_SHIFT); // Wrong key
                        Execution.delay(Random.nextInt(200, 400));
                    }

                    Keyboard.typeKey(KeyEvent.VK_SPACE);
                    System.out.println("✅ Confirmed cooking");

                    // Wait for animation with anti-ban checks
                    boolean startedCooking = Execution.delayUntil(
                            () -> Players.getLocal().getAnimationId() != -1,
                            Random.nextInt(800, 1500),
                            Random.nextInt(3000, 5000)
                    );

                    if (startedCooking) {
                        System.out.println("🔥 Cooking started");

                        // --- ANIMATION LOOP WITH ANTI-BAN ---
                        while (Players.getLocal().getAnimationId() != -1) {
                            Execution.delay(Random.nextInt(1000, 2000));

                            // Anti-ban during cooking (5% chance)
                            if (Random.nextInt(0,100) < 5) {
                                antiBan.performAntiBan();
                            }

                            // Small chance of early exit (like misclick)
                            if (Random.nextInt(0,100) < 3) {
                                System.out.println("🚪 Simulating early exit");
                                break;
                            }
                        }
                    } else {
                        System.out.println("⚠️ Failed to start cooking");
                        // Recover by trying again after delay
                        Execution.delay(Random.nextInt(2000, 3000));
                    }

            }

        } else {
            System.out.println("🚶 Cooking object not found or not visible. Walking to cooking area.");
            if (!cookingArea.contains(Players.getLocal())) {
                walkTo(cookingArea);
            }
            Execution.delay(Random.nextInt(600, 1200));
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
            Execution.delay(Random.nextInt(300, 800));
            Bank.depositInventory();

            Execution.delay(Random.nextInt(1000, 2400)); // Delay before withdrawal
            String rawFoodName = settings.getFoodType().getRawName();
            System.out.println("🎣 Withdrawing raw food: " + rawFoodName);
            Bank.withdraw(rawFoodName, 28);

            Execution.delayUntil(() -> Inventory.contains(rawFoodName), 2000, 4000);

            if (!Inventory.contains(rawFoodName)) {
                System.out.println("❌ Withdrawal failed or no raw food left.");
            } else {
                System.out.println("✅ Withdrawal successful.");
            }

            Execution.delay(Random.nextInt(300, 700)); // Delay before closing bank
            System.out.println("🔐 Closing bank.");
            Bank.close();

            Execution.delay(Random.nextInt(400, 1000)); // Short pause after interaction
        } else  {
            System.out.println("🏦 Bank is closed. Attempting to open bank.");
            Execution.delay(Random.nextInt(400, 800));

            GameObject bank = GameObjects.newQuery().actions("Bank").results().nearest();
            if (bank != null) {
                Execution.delay(Random.nextInt(2500, 3500)); // Simulate hesitation
                if (bank.interact("Bank")) {
                    Execution.delayUntil(Bank::isOpen, 2000, 5000);
                } else {
                    System.out.println("⚠️ Failed to interact with bank object.");
                    return;
                }
            } else {
                System.out.println("❌ No bank object found nearby.");
                return;
            }
        }

        if (!closestBank.contains(Players.getLocal()))
            walkTo(closestBank);

    }

    private void walkTo(Area targetArea) {
        final int MAX_ATTEMPTS = 5;
        int attempts = 0;
        boolean reached = false;

        Coordinate destination = targetArea.getCenter();

        System.out.println("📍 Attempting to walk to: " + destination);

        while (attempts < MAX_ATTEMPTS && !targetArea.contains(Players.getLocal())) {
            Coordinate target = attempts < 3 ? targetArea.getRandomCoordinate() : destination;

            Pathfinder.PathBuilder builder = pathfinder.pathBuilder()
                    .destination(target)
                    .enableHomeTeleport(false)
                    .enableTeleports(false)
                    .avoidWilderness(true)
                    .preferAccuracy();

            Path path = builder.findPath();

            if (path != null && path.isValid()) {
                System.out.println("✅ Found valid path. Walking to: " + target);
                while (!targetArea.contains(Players.getLocal()) && path.step()) {
                    Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1200);
                    if (Random.nextInt(100) < 6) {
                        antiBan.performAntiBan();
                    }
                    Execution.delay(800, 1500);
                }

                if (targetArea.contains(Players.getLocal())) {
                    reached = true;
                    break;
                }
            } else {
                System.out.println("⚠️ Attempt " + (attempts + 1) + ": No valid path to " + target);
            }

            Execution.delay(500, 1000);
            attempts++;
        }

        if (reached) {
            System.out.println("✅ Arrived at destination: " + destination);
        } else {
            System.out.println("❌ Failed to reach destination after " + MAX_ATTEMPTS + " attempts. Using fallback.");
            fallBack(targetArea);
        }
    }

    private Coordinate getRandomNearbyCoordinate(Coordinate center, int radius) {
        int xOffset = Random.nextInt(-radius, radius + 1);
        int yOffset = Random.nextInt(-radius, radius + 1);
        return new Coordinate(center.getX() + xOffset, center.getY() + yOffset, center.getPlane());
    }

    private void scheduleNextBreak() {
        int delay = Random.nextInt(settings.getBreakMax() - settings.getBreakMin() + 1) + settings.getBreakMin();
        nextBreakTime = System.currentTimeMillis() + (delay * 1000);
        System.out.println("Next break scheduled in " + delay + " seconds.");
    }

    @Override
    public void onStop() {
        RuneScape.logout();
        System.out.println("Cooker stopped.");
    }

    @Override
    public void onSettingChanged(SettingChangedEvent settingChangedEvent) {

    }

    @Override
    public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }

}
