package com.runemate.party.cooking;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.InteractablePoint;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.cognizant.ScenePath;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Distance;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.LoopingBot;
import com.runemate.game.api.script.framework.listeners.SettingsListener;
import com.runemate.game.api.script.framework.listeners.events.SettingChangedEvent;
import com.runemate.pathfinder.Pathfinder;
import com.runemate.ui.setting.annotation.open.SettingsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public class SimpleMiner extends LoopingBot implements SettingsListener {

    public static final Area VARROCK_WEST_BANK = new Area.Rectangular(
            new Coordinate(3181, 3436, 0), new Coordinate(3186, 3444, 0));

    public static final Area VARROCK_EAST_BANK = new Area.Rectangular(
            new Coordinate(3251, 3420, 0), new Coordinate(3257, 3426, 0));

    public static final Area GRAND_EXCHANGE_BANK = new Area.Rectangular(
            new Coordinate(3161, 3476, 0), new Coordinate(3169, 3486, 0));

    public static final Area EDGEVILLE_BANK = new Area.Rectangular(
            new Coordinate(3092, 3489, 0), new Coordinate(3099, 3499, 0));

    public static final Area FALADOR_EAST_BANK = new Area.Rectangular(
            new Coordinate(3010, 3352, 0), new Coordinate(3016, 3359, 0));

    public static final Area FALADOR_WEST_BANK = new Area.Rectangular(
            new Coordinate(2942, 3367, 0), new Coordinate(2946, 3375, 0));

    public static final Area DRAYNOR_BANK = new Area.Rectangular(
            new Coordinate(3092, 3240, 0), new Coordinate(3099, 3245, 0));

    public static final Area AL_KHARID_BANK = new Area.Rectangular(
            new Coordinate(3268, 3161, 0), new Coordinate(3273, 3170, 0));

    public static final Area CAMELOT_BANK = new Area.Rectangular(
            new Coordinate(2721, 3491, 0), new Coordinate(2726, 3494, 0));

    public static final Area SEERS_VILLAGE_BANK = new Area.Rectangular(
            new Coordinate(2723, 3492, 0), new Coordinate(2730, 3499, 0));

    public static final Area ARDOUGNE_NORTH_BANK = new Area.Rectangular(
            new Coordinate(2611, 3330, 0), new Coordinate(2617, 3337, 0));

    public static final Area ARDOUGNE_SOUTH_BANK = new Area.Rectangular(
            new Coordinate(2652, 3283, 0), new Coordinate(2658, 3289, 0));

    public static final Area CATHERBY_BANK = new Area.Rectangular(
            new Coordinate(2805, 3439, 0), new Coordinate(2810, 3444, 0));

    public static final Area YANILLE_BANK = new Area.Rectangular(
            new Coordinate(2610, 3090, 0), new Coordinate(2615, 3095, 0));

    public static final Area ZEAH_BANK = new Area.Rectangular(
            new Coordinate(1634, 3762, 0), new Coordinate(1640, 3767, 0));

    private static final List<Area> BANK_LOCATIONS = Arrays.asList(
            VARROCK_WEST_BANK,
            VARROCK_EAST_BANK,
            GRAND_EXCHANGE_BANK,
            EDGEVILLE_BANK,
            FALADOR_EAST_BANK,
            FALADOR_WEST_BANK,
            DRAYNOR_BANK,
            AL_KHARID_BANK,
            CAMELOT_BANK,
            SEERS_VILLAGE_BANK,
            ARDOUGNE_NORTH_BANK,
            ARDOUGNE_SOUTH_BANK,
            CATHERBY_BANK,
            YANILLE_BANK,
            ZEAH_BANK
    );

    // Use a shared safe waypoint list (can be saved/loaded if needed)
    Set<Coordinate> safeWaypoints = new HashSet<>(Arrays.asList(
            new Coordinate(3183, 3440, 0), // Varrock West Bank
            new Coordinate(3253, 3423, 0), // Varrock East Bank
            new Coordinate(3165, 3480, 0) // Grand Exchange Bank
    ));

    private static final Logger logger = LogManager.getLogger(SimpleMiner.class);

    @SettingsProvider(updatable = true)
    private MinerSettings settings;

    // Cached settings
    private boolean useP2P;

    private boolean movingToBank;
    private boolean movingToMiningArea;
    private long nextBreakTime;
    private long lastZoomTime = 0;

    private boolean settingsConfirmed;

    private Pathfinder pathfinder;

    @Override
    public void onStart(String... args) {
        // Initialize settings UI and listener

        getEventDispatcher().addListener(this);
        // Populate initial values
        nextBreakTime = System.currentTimeMillis() + (20000 * 1000);
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

        while (attempts < MAX_ATTEMPTS && !miningArea.contains(Players.getLocal())) {
            Coordinate target = attempts < 3 ? miningArea.getRandomCoordinate() : miningArea.getCenter();

            Pathfinder.PathBuilder builder = pathfinder.pathBuilder()
                    .start(Players.getLocal().getPosition())
                    .enableHomeTeleport(false)
                    .destination(target)
                    .preferAccuracy();

            Path path = builder.findPath();

            if (path != null && path.isValid()) {
                int failedSteps = 0;
                final int MAX_FAILED_STEPS = 8;

                while (!miningArea.contains(Players.getLocal())) {
                    if (!path.step()) {
                        failedSteps++;

                        if (failedSteps >= MAX_FAILED_STEPS) {
                            System.out.println("⚠️ Too many failed steps on current path. Breaking...");
                            break;
                        }
                    } else {
                        failedSteps = 0; // Reset if stepping works
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

        Area closestBank = BANK_LOCATIONS.stream()
                .min(Comparator.comparingInt(b -> (int) b.getCenter().distanceTo(Players.getLocal().getPosition())))
                .orElse(VARROCK_WEST_BANK);

        if (closestBank == null || Players.getLocal() == null) {
            System.out.println("❌ Invalid bank area or player.");
            return;
        }

        System.out.println("🚶 Attempting to walk to bank...");

// Retry mechanism
        int attempts = 0;
        final int MAX_ATTEMPTS = 5;
        boolean reached = false;

        while (attempts < MAX_ATTEMPTS && !closestBank.contains(Players.getLocal())) {
            Coordinate target = attempts < 3 ? closestBank.getRandomCoordinate() : closestBank.getCenter();

            Pathfinder.PathBuilder builder = pathfinder.pathBuilder()
                    .start(Players.getLocal().getPosition())
                    .destination(target)
                    .enableHomeTeleport(false)
                    .enableTeleports(false)
                    .avoidWilderness(true)
                    .preferAccuracy();

            Path path = builder.findPath();

            if (path != null && path.isValid()) {
                while (!closestBank.contains(Players.getLocal()) && path.step()) {
                    Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1200);
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

        if (Random.nextInt(100) < 20) { // 20% chance
            System.out.println("🛡️ Performing anti-ban action...");

// Weighted action pool: more entries = higher chance
            int[] weightedActions = {
                    0, 0, 0, 0, // Camera rotation (case 0, more weight)
                    1,    // Hover over player (case 1)
                    2,    // Hover over NPC (case 2)
                    3,    // Move mouse (case 3)
                    4,    // Camera zoom (case 4)
                    5, 5, // Idle action (case 5, more weight)
                    6     // Switch tab (case 6)
            };

            int action = weightedActions[Random.nextInt(weightedActions.length)];

            switch (action) {
                case 0 -> {
                    // All possible key combinations: solo + diagonal
                    int[][] keyCombos = {
                            {KeyEvent.VK_LEFT},                 // Rotate left
                            {KeyEvent.VK_RIGHT},                // Rotate right
                            {KeyEvent.VK_UP},                   // Pitch up
                            {KeyEvent.VK_DOWN},                 // Pitch down
                            {KeyEvent.VK_LEFT, KeyEvent.VK_UP},    // Diagonal up-left
                            {KeyEvent.VK_RIGHT, KeyEvent.VK_UP},   // Diagonal up-right
                            {KeyEvent.VK_LEFT, KeyEvent.VK_DOWN},  // Diagonal down-left
                            {KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN}  // Diagonal down-right
                    };

                    int[] combo = keyCombos[Random.nextInt(keyCombos.length)];

                    // Press all keys in the combo
                    for (int key : combo) {
                        Keyboard.pressKey(key);
                    }

                    // Hold the keys for a random duration
                    Execution.delay(Random.nextInt(500, 1800));

                    // Release the keys
                    for (int key : combo) {
                        Keyboard.releaseKey(key);
                    }

                    // Build a readable direction string
                    StringBuilder direction = new StringBuilder();
                    for (int key : combo) {
                        direction.append(
                                switch (key) {
                                    case KeyEvent.VK_LEFT -> "LEFT ";
                                    case KeyEvent.VK_RIGHT -> "RIGHT ";
                                    case KeyEvent.VK_UP -> "UP ";
                                    case KeyEvent.VK_DOWN -> "DOWN ";
                                    default -> "";
                                }
                        );
                    }

                    System.out.println("🎥 Camera moved: " + direction.toString().trim());

                    // Optional: misclick delay
                    if (Random.nextInt(100) < 25) {
                        int delay = Random.nextInt(120, 300);
                        System.out.println("⌛ Simulating misclick delay: " + delay + "ms");
                        Execution.delay(delay);
                    }
                }
                case 1 -> {
                    // Hover over a random nearby player
                    Player local = Players.getLocal();
                    Player player = Players.newQuery()
                            .filter(p -> p != null && !p.equals(local) && p.isVisible())
                            .results()
                            .random();

                    if (player != null) {
                        player.hover();
                        System.out.println("👤 Hovered over player: " + player.getName());
                        Execution.delay(Random.nextInt(300, 600));
                    }
                }
                case 2 -> {
                    // Hover over a random visible NPC
                    Npc npc = Npcs.newQuery().filter(Npc::isVisible).results().random();
                    if (npc != null) {
                        npc.hover();
                        System.out.println("🧟 Hovered over NPC: " + npc.getName());
                        Execution.delay(Random.nextInt(300, 600));
                    }
                }
                case 3 -> {
                    // Move mouse to a random screen position using an InteractablePoint
                    int x = Random.nextInt(200, 500);
                    int y = Random.nextInt(100, 400);
                    InteractablePoint fakePoint = new InteractablePoint(new Point(x, y));
                    if (Mouse.move(fakePoint)) {
                        System.out.println("🖱️ Moved mouse to random interactable point: (" + x + ", " + y + ")");
                    } else {
                        System.out.println("⚠️ Failed to move mouse to random point.");
                    }
                    Execution.delay(Random.nextInt(300, 500));
                }
                case 4 -> {
                    // Simulate camera zoom (if enough time has passed)
                    if (System.currentTimeMillis() - lastZoomTime > 2 * 60 * 1000) {
                        boolean zoomIn = Random.nextBoolean();
                        Mouse.scroll(zoomIn); // true = scroll up (zoom in), false = scroll down (zoom out)
                        System.out.println("🔍 Camera zoom " + (zoomIn ? "in" : "out"));
                        lastZoomTime = System.currentTimeMillis();
                        Execution.delay(Random.nextInt(300, 500));
                    } else {
                        System.out.println("⏳ Skipped zoom: cooldown active.");
                    }
                }
                case 5 -> {
                    // Idle for a realistic human-like duration
                    int idleDuration = Random.nextInt(1200, 6000); // 1.2 to 6 seconds
                    System.out.println("💤 Idling for " + idleDuration + "ms...");
                    Execution.delay(idleDuration);

                    // 50% chance to do a subtle mouse twitch while idling
                    if (Random.nextBoolean()) {
                        Point current = Mouse.getPosition();
                        Point twitch = new Point(
                                current.x + Random.nextInt(-3, 3),
                                current.y + Random.nextInt(-3, 3)
                        );

                        InteractablePoint interactableTwitch = new InteractablePoint(twitch);
                        if (Mouse.move(interactableTwitch)) {
                            System.out.println("🖱️ Idle mouse twitch to: (" + twitch.x + ", " + twitch.y + ")");
                        } else {
                            System.out.println("❌ Failed to move mouse during idle twitch.");
                        }
                    }
                }
                case 6 -> {
                    // OSRS default F-key bindings (modify based on your settings)
                    final int[] TAB_KEYS = {
                            KeyEvent.VK_F2, // Stats
                            KeyEvent.VK_F4, // Inventory
                            KeyEvent.VK_F8, // Friends
                    };

                    int randomIndex = Random.nextInt(TAB_KEYS.length);
                    int tabKey = TAB_KEYS[randomIndex];

                    Keyboard.pressKey(tabKey);
                    Execution.delay(Random.nextInt(100, 200));
                    Keyboard.releaseKey(tabKey);

                    System.out.println("🎹 Switched tab using F-key: F" + (randomIndex + 1));

                    // Optional: Add delay to simulate reading/interaction
                    Execution.delay(Random.nextInt(400, 800));

                    // 25% chance to switch back to inventory (F4)
                    if (Random.nextInt(4) == 0) {
                        Keyboard.pressKey(KeyEvent.VK_F4);
                        Execution.delay(Random.nextInt(100, 200));
                        Keyboard.releaseKey(KeyEvent.VK_F4);
                        System.out.println("↩️ Returned to Inventory (F4)");
                    }
                }
            }

            // Post-action delay
            Execution.delay(Random.nextInt(200, 500));
        }

        Player player = Players.getLocal();
        if (player == null) {
            logger.warn("Unable to find local player");
            return;
        }


        // 5) Mining
        String oreName = settings.getOreType().toString();
        GameObject rock = GameObjects.newQuery()
                .names(oreName)
                .results()
                .nearest();

        if (!movingToBank && (rock == null || !rock.isVisible()))
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

            if (player.getAnimationId() == -1 && !player.isMoving()) {
                if (rock != null && rock.interact("Mine")) {
                    // Wait until mining animation starts or the player starts moving
                    if (Execution.delayUntil(() -> player.getAnimationId() != -1 || player.isMoving(), 600, 3000)) {
                        // Once mining starts, wait while it's ongoing
                        Execution.delayWhile(() -> player.getAnimationId() != -1 || player.isMoving(), 3000, 6000);
                    }
                } else {
                    Execution.delay(Random.nextInt(500, 1000));
                }
            } else {
                // Already mining or moving, just wait a bit
                Execution.delay(Random.nextInt(300, 700));
            }
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
