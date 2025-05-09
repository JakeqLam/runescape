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
import com.runemate.party.common.GPTNavigation;
import com.runemate.pathfinder.Pathfinder;
import com.runemate.ui.setting.annotation.open.SettingsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SimpleMiner extends LoopingBot implements SettingsListener {

    private static final Logger logger = LogManager.getLogger(SimpleMiner.class);

    @SettingsProvider(updatable = true)
    private MinerSettings settings;

    // Cached settings
    private boolean useP2P;

    private boolean movingToBank;

    AntiBan antiBan;
    private GPTNavigation navigation;

    private boolean settingsConfirmed;

    private Pathfinder pathfinder;

    private final Set<Coordinate> contestedRocks = new HashSet<>();
    private long contestedClearTime = System.currentTimeMillis();

    @Override
    public void onStart(String... args) {
        // Initialize settings UI and listener
        antiBan= new AntiBan();
        navigation = new GPTNavigation();
        getEventDispatcher().addListener(this);
        pathfinder = Pathfinder.create(this);
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
                navigation.walkToArea(settings.getLocation().getArea(), pathfinder);
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

        if (!closestBank.contains(Players.getLocal())) {
            System.out.println("🚶 Attempting to walk to bank...");
            navigation.walkToArea(closestBank, pathfinder);
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
        antiBan.performBreakLogic(settings.getBreakMin(), settings.getBreakMax());

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

        antiBan.maybePerformAntiBan();

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
            navigation.walkToArea(settings.getLocation().getArea(), pathfinder);

        if (rock != null && !rock.isVisible() && !movingToBank) {
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
        }
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

}
