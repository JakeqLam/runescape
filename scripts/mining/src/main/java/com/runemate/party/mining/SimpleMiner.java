package com.runemate.party.mining;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.basic.BresenhamPath;
import com.runemate.game.api.hybrid.location.navigation.cognizant.ScenePath;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Distance;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.LoopingBot;
import com.runemate.game.api.script.framework.listeners.SettingsListener;
import com.runemate.game.api.script.framework.listeners.events.SettingChangedEvent;
import com.runemate.party.common.AntiBan;
import com.runemate.pathfinder.Pathfinder;
import com.runemate.ui.setting.annotation.open.SettingsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SimpleMiner extends LoopingBot implements SettingsListener {

    private static final Logger logger = LogManager.getLogger(SimpleMiner.class);
    private static final List<BankLocation> BANK_LOCATIONS = Arrays.asList(BankLocation.values());

    @SettingsProvider(updatable = true)
    private MinerSettings settings;

    // Cached settings
    private boolean useP2P;

    private boolean movingToBank;
    private boolean movingToMiningArea;
    private long nextBreakTime;
    private long lastActionTime = System.currentTimeMillis();

    AntiBan antiBan;

    private boolean settingsConfirmed;

    private Pathfinder pathfinder;

    private long lastAntiBanTime = 0;
    private long nextAntiBanCooldown = getRandomCooldown();

    private final Set<Coordinate> contestedRocks = new HashSet<>();
    private long contestedClearTime = System.currentTimeMillis();

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

    @Override
    public void onStart(String... args) {
        // Initialize settings UI and listener
        antiBan= new AntiBan();
        getEventDispatcher().addListener(this);
        // Populate initial values
        nextBreakTime = System.currentTimeMillis() + (Random.nextInt(5000,7000) * 1000L);
        pathfinder = Pathfinder.create(this);
    }

    public void walkToMiningLocation() {
        Area miningArea = settings.getLocation().getArea();

        if (miningArea == null || Players.getLocal() == null) {
            System.out.println("❌ Invalid mining area or player.");
            return;
        }

        if (miningArea.contains(Players.getLocal())) {
            System.out.println("✅ Already in the mining area.");
            return;
        }

        System.out.println("🚶 Attempting to walk to mining area...");

// Retry variables
        int attempts = 0;
        final int MAX_ATTEMPTS = 5;
        boolean reached = false;
        Coordinate target = miningArea.getCenter();

        while (attempts < MAX_ATTEMPTS && !miningArea.contains(Players.getLocal())) {
            target = attempts < 3 ? miningArea.getRandomCoordinate() : miningArea.getCenter();

            Pathfinder.PathBuilder builder = pathfinder.pathBuilder()
                    .enableHomeTeleport(false)
                    .destination(target)
                    .preferAccuracy();

            Path path = builder.findPath();

            if (path != null && path.isValid()) {
                int failedSteps = 0;
                final int MAX_FAILED_STEPS = 4;

                while (!miningArea.contains(Players.getLocal())) {
                    if (!path.step()) {
                        failedSteps++;

                        if (failedSteps >= MAX_FAILED_STEPS) {
                            System.out.println("⚠️ Too many failed steps on current path. Breaking...");
                            break;
                        }
                    } else {
                        failedSteps = 0; // Reset if stepping works
                        System.out.println("Pathing success");
                        maybePerformAntiBan();
                    }

                    Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1200);
                    Execution.delay(800, 1500);
                }

                if (miningArea.contains(Players.getLocal())) {
                    reached = true;
                    break;
                }
            } else {
                System.out.println("⚠️ Attempt " + (attempts + 1) + " failed to find a valid path.");
            }

            Execution.delay(500, 1000);
            attempts++;
        }

        if (reached) {
            System.out.println("✅ Reached the mining area.");
        } else {
            System.out.println("❌ Failed to reach the mining area after " + MAX_ATTEMPTS + " attempts.");
            fallBack(miningArea);
        }
    }

    public void fallBack(Area area) {
        Coordinate target = area.getCenter();
        if (area.contains(Players.getLocal())) {
            System.out.println("✅ Already in the mining area.");
            return;
        }

        // Fallback: Click a nearby walkable tile using Interactable
        Path path = BresenhamPath.buildTo(target.randomize(10,10));
        if (path != null) {
            path.step();
            Execution.delay(800, 1500);
        } else {
            Coordinate start = Players.getLocal().getPosition();

            if (start == null) {
                System.out.println("❌ Invalid start or target coordinate.");
                return;
            }

            // Calculate 1/3rd point between start and target
            int newX = start.getX() + (target.getX() - start.getX()) / 3;
            int newY = start.getY() + (target.getY() - start.getY()) / 3;
            Coordinate oneThird = new Coordinate(newX, newY, start.getPlane());

            // Randomize slightly to avoid exact clicks
            Coordinate destination = oneThird.randomize(1, 1);

            path = pathfinder.pathBuilder().destination(destination)
                    .enableHomeTeleport(false)
                    .avoidWilderness(true)
                    .preferAccuracy().findPath();
            if (path != null && path.isValid()) {
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

    public void depositOresInBank() {
        if (Bank.isOpen()) {
            // Click the "Deposit All" button
            Bank.depositInventory();
            Execution.delayUntil(() -> !Bank.isOpen(), 2000, 4000); // Wait until the bank window closes
            System.out.println("Deposited all items in the bank.");

            // Close the bank window
            if (Bank.isOpen()) {
                Bank.close();
                Execution.delay(1000, 2000); // Add a small delay to make sure the bank closes properly
                System.out.println("Bank window closed.");
                movingToBank = false;
                walkToMiningLocation();
            }

        } else {
            System.out.println("Bank is not open, cannot deposit items.");
        }
    }

    public void walkToAndOpenBank() {
        movingToBank = true;

        BankLocation preferredBank = settings.getPreferredBank();
        Area closestBank;

        if (preferredBank.isNearest()) {
            closestBank = Arrays.stream(BankLocation.values())
                    .filter(b -> b != BankLocation.NEAREST)
                    .map(BankLocation::getArea)
                    .min(Comparator.comparingInt(a -> (int) a.getCenter().distanceTo(Players.getLocal().getPosition())))
                    .orElse(null);
        } else {
            closestBank = preferredBank.getArea();
        }


        if (Players.getLocal() == null) {
            System.out.println("❌ Invalid bank area or player.");
            return;
        }

        if (!closestBank.contains(Players.getLocal()))
        {
            System.out.println("🚶 Attempting to walk to bank...");

// Retry mechanism
            int attempts = 0;
            final int MAX_ATTEMPTS = 5;
            boolean reached = false;
            Coordinate target = closestBank.getCenter();

            while (attempts < MAX_ATTEMPTS && !closestBank.contains(Players.getLocal())) {
                target = attempts < 3 ? closestBank.getRandomCoordinate() : closestBank.getCenter();

                Pathfinder.PathBuilder builder = pathfinder.pathBuilder()
                        .destination(target)
                        .enableHomeTeleport(false)
                        .enableTeleports(false)
                        .avoidWilderness(true)
                        .preferAccuracy();

                Path path = builder.findPath();

                if (path != null && path.isValid()) {
                    while (!closestBank.contains(Players.getLocal()) && path.step()) {
                        System.out.println("Pathing success");
                        Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1200);
                        maybePerformAntiBan();
                        Execution.delay(800, 1500);
                    }

                    if (closestBank.contains(Players.getLocal())) {
                        reached = true;
                        break;
                    }
                } else {
                    System.out.println("⚠️ Attempt " + (attempts + 1) + " failed to find a valid path.");
                }

                Execution.delay(500, 1000);
                attempts++;
            }

            if (reached) {
                System.out.println("✅ Arrived at the bank.");
            } else {
                System.out.println("❌ Failed to reach the bank after " + MAX_ATTEMPTS + " attempts.");
                fallBack(closestBank);
            }
        }

        Execution.delay(8000, 10000);

        // 🔍 Once in range, try to find a bank object and interact
        GameObject nearestBank = GameObjects.newQuery()
                .actions("Bank")
                .results()
                .nearest();

        if (nearestBank != null && nearestBank.interact("Bank")) {
            Execution.delayUntil(Bank::isOpen, Random.nextInt(6000, 8000));
            depositOresInBank();
        } else {
            if (Bank.isOpen())
            {
                depositOresInBank();
            }
            System.out.println("⚠️ Could not find a bank object to interact with.");
        }
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) {
            return;
        }

        // 1) Break logic
        if (System.currentTimeMillis() >= nextBreakTime) {
            int breakSeconds = Random.nextInt(settings.getBreakMinLength(), settings.getBreakMaxLength() + 1);
            if(RuneScape.isLoggedIn()){
                RuneScape.logout(RuneScape.isLoggedIn());
            }
            System.out.println("Taking break for: " + breakSeconds);
            Execution.delay(breakSeconds * 1000); // Wait during logout
            Execution.delayUntil(() -> RuneScape.isLoggedIn(), 5000, 30000); // waits up to 30 seconds for auto-login
            scheduleNextBreak();
        }

//        // 2) World‑hop if too many players
//        if (Players.getLoaded().size() > 2) {
//            Worlds.newQuery()
//                    .filter(w -> w.isMembersOnly() == useP2P && w.getId() != Worlds.getCurrent())
//                    .results()
//                    .random()
//                            .
//            Execution.delayUntil(() -> Players.getLoaded().size() <= 2, 5000);
//            return random.nextInt(500, 1000);
//        }

// Banking if inventory is full
        if (Inventory.isFull()) {
            System.out.println("Walking to bank");
            walkToAndOpenBank();
        }

        maybePerformAntiBan();

        Player player = Players.getLocal();
        if (player == null) {
            logger.warn("Unable to find local player");
            return;
        }


        // 5) Mining
        maybeClearContestedRocks();

        String oreName = settings.getOreType().toString();
        GameObject rock = GameObjects.newQuery()
                .names(oreName)
                .filter(r -> !contestedRocks.contains(r.getPosition()))
                .results()
                .nearest();

        if (rock != null) {
            boolean someoneElseIsMining = Players.newQuery()
                    .filter(p -> !p.equals(Players.getLocal()))
                    .filter(p -> p.getTarget() != null && p.getTarget().equals(rock))
                    .results()
                    .size() > 0;

            if (someoneElseIsMining) {
                System.out.println("⛏️ Rock is being mined by another player, skipping...");
                contestedRocks.add(rock.getPosition());
                Execution.delay(500, 1000);
                return;
            }
        }


        if (!movingToBank && rock == null)
            walkToMiningLocation();

        if (rock != null && !rock.isVisible() && !movingToBank && !movingToMiningArea) {
            if (Distance.between(player, rock) > 8) {
                logger.info("We're far away from {}, walking towards it", rock);

                Area.Rectangular area = rock.getArea();
                if (area == null) {
                    logger.warn("Unable to find an appropriate tile next to the rock to walk to!");
                    return;
                }

                ScenePath path = ScenePath.buildBetween(player, area.getSurroundingCoordinates());
                if (path == null) {
                    logger.warn("Unable to find a path to {}", rock);
                    return;
                }

                path.step();
                return;
            }

            Camera.concurrentlyTurnTo(rock);
        }


        if (!Inventory.isFull()) {

            // If player is idle (not mining or moving)
            if (player.getAnimationId() == -1 && !player.isMoving()) {
                if (rock != null) {
                    // Misclick simulation (8% chance)
                    if (Random.nextInt(0,100) < 8) {
                        System.out.println("🤖 Simulating misclick...");
                        // Click near the rock (but not on it)
                        Mouse.move(rock.getPosition().randomize(1, 3)); // Offset by 5-10 pixels
                        Execution.delay(Random.nextInt(200, 500));
                        Mouse.click(Mouse.Button.LEFT);
                        Execution.delay(Random.nextInt(800, 1200)); // Pretend to realize mistake
                    }

                    // Actual mining attempt
                    if (rock.interact("Mine")) {
                        System.out.println("⛏️ Mining " + "...");

                        // Wait for mining to start (with randomized delay)
                        if (!Execution.delayUntil(() -> player.getAnimationId() != -1 || player.isMoving(),
                                Random.nextInt(400, 800), Random.nextInt(3000, 5000))) {
                            System.out.println("❌ Failed to start mining.");
                            return;
                        }
                    }
                }
            } else {
                // Player is already mining/moving
                Execution.delay(Random.nextInt(300, 700));
            }

            lastActionTime = System.currentTimeMillis();

        }
    }

    private boolean isRockContested(GameObject rock) {
        if (rock == null) return true;

        // Consider it contested if another player is within 1 tile and not you
        return Players.newQuery()
                .filter(p -> !p.equals(Players.getLocal()))
                .filter(p -> Distance.between(p, rock) <= 4)
                .results()
                .size() > 0;
    }

    private void maybeClearContestedRocks() {
        if (System.currentTimeMillis() - contestedClearTime > 250_000) { // 2.5 minutes
            contestedRocks.clear();
            contestedClearTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onStop() {
        logger.info("Universal Miner stopped.");
    }

    // Fired when any single setting changes
    @Override
    public void onSettingChanged(SettingChangedEvent event) {
        applySettings();
    }

    @Override
    public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }

    // Helper to read & cache settings
    private void applySettings() {

        useP2P      = settings.getUseP2P();

    }

    private void scheduleNextBreak() {
        int delay = Random.nextInt(settings.getBreakMax() - settings.getBreakMin() + 1) + settings.getBreakMin();
        nextBreakTime = System.currentTimeMillis() + (delay * 1000);
    }

}
