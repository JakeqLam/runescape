package com.runemate.party.miner;

import com.runemate.game.api.hybrid.entities.*;
import com.runemate.game.api.hybrid.local.*;
import com.runemate.game.api.hybrid.local.hud.interfaces.*;
import com.runemate.game.api.hybrid.location.*;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.cognizant.*;
import com.runemate.game.api.hybrid.location.navigation.web.WebPath;
import com.runemate.game.api.hybrid.region.*;
import com.runemate.game.api.hybrid.util.calculations.*;
import com.runemate.game.api.script.*;
import com.runemate.game.api.script.framework.*;
import com.runemate.game.api.script.framework.listeners.*;
import com.runemate.game.api.script.framework.listeners.events.*;
import com.runemate.pathfinder.Pathfinder;
import com.runemate.ui.setting.annotation.open.*;

import com.runemate.ui.setting.open.SettingsManager;
import org.apache.logging.log4j.*;

import org.jetbrains.annotations.Nullable;

import javax.xml.stream.Location;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.runemate.game.api.hybrid.local.Screen.getLocation;

public class SimpleMiner extends LoopingBot implements SettingsListener {

    public static final Area VARROCK_WEST_BANK = new Area.Rectangular(
            new Coordinate(3185, 3435, 0), new Coordinate(3181, 3445, 0));

    public static final Area VARROCK_EAST_BANK = new Area.Rectangular(
            new Coordinate(3250, 3420, 0), new Coordinate(3257, 3426, 0));

    public static final Area GRAND_EXCHANGE_BANK = new Area.Rectangular(
            new Coordinate(3160, 3476, 0), new Coordinate(3169, 3487, 0));

    public static final Area EDGEVILLE_BANK = new Area.Rectangular(
            new Coordinate(3091, 3488, 0), new Coordinate(3099, 3499, 0));

    public static final Area FALADOR_EAST_BANK = new Area.Rectangular(
            new Coordinate(3010, 3352, 0), new Coordinate(3016, 3359, 0));

    public static final Area FALADOR_WEST_BANK = new Area.Rectangular(
            new Coordinate(2942, 3367, 0), new Coordinate(2946, 3375, 0));

    public static final Area DRAYNOR_BANK = new Area.Rectangular(
            new Coordinate(3091, 3240, 0), new Coordinate(3099, 3245, 0));

    public static final Area AL_KHARID_BANK = new Area.Rectangular(
            new Coordinate(3268, 3160, 0), new Coordinate(3273, 3170, 0));

    public static final Area CAMELOT_BANK = new Area.Rectangular(
            new Coordinate(2721, 3491, 0), new Coordinate(2726, 3494, 0));

    public static final Area SEERS_VILLAGE_BANK = new Area.Rectangular(
            new Coordinate(2722, 3492, 0), new Coordinate(2730, 3499, 0));

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

    private static final Logger logger = LogManager.getLogger(SimpleMiner.class);

    @SettingsProvider(updatable = true)
    private MinerSettings   settings;

    // Cached settings
    private boolean    useP2P;
    private boolean movingToBank;

    private long nextBreakTime;

    private boolean settingsConfirmed;
    String group = "SimpleMiner";  // must match your @SettingsGroup

    private Pathfinder pathfinder;

    @Override
    public void onStart(String... args) {
        // Initialize settings UI and listener

        getEventDispatcher().addListener(this);
        // Populate initial values
        nextBreakTime = System.currentTimeMillis() + (60 * 1000);
        pathfinder = Pathfinder.create();
    }

    public void walkToMiningLocation() {
        Area closestMiningLocation= settings.getLocation().getArea();

        if (closestMiningLocation != null && !closestMiningLocation.contains(Players.getLocal())) {

            Path path = pathfinder.pathBuilder()
                    .start(Players.getLocal().getPosition())
                    .destination(closestMiningLocation.getRandomCoordinate())
                    .preferAccuracy()
                    .findPath();

            if (path != null) {
                while (path != null && !closestMiningLocation.contains(Players.getLocal()) && path.step()) {
                    path = pathfinder.pathBuilder()
                            .start(Players.getLocal().getPosition())
                            .destination(closestMiningLocation.getRandomCoordinate())
                            .preferAccuracy()
                            .findPath();
                    Execution.delay(1200, 2400);
                }
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
        // ✅ Replace with your preferred bank location — this is Varrock West Bank

        movingToBank = true;

        // Get closest bank area
        Area closestBank = BANK_LOCATIONS.stream()
                .min(Comparator.comparingInt(b -> (int) b.getCenter().distanceTo(Players.getLocal().getPosition())))
                .orElse(VARROCK_WEST_BANK); // default fallback

        if (bankArea == null || Players.getLocal() == null) {
            System.out.println("❌ Invalid bank area or player.");
            return;
        }

        // Check if we're already inside the bank
        if (bankArea.contains(Players.getLocal())) {
            System.out.println("✅ Already inside the bank area.");
            return;
        }

        Pathfinder.PathBuilder builder = pathfinder.pathBuilder()
                .start(Players.getLocal().getPosition())
                .destination(bankArea.getRandomCoordinate())
                .enableHomeTeleport(false)
                .enableTeleports(false)
                .avoidWilderness(true)
                .preferAccuracy();

        Path path = builder.findPath();

        if (path == null) {
            System.out.println("❌ Could not find path to bank.");
            return;
        }

        System.out.println("🚶 Walking to the bank...");

        while (!bankArea.contains(Players.getLocal())) {
            if (!path.step()) {
                System.out.println("⚠️ Step failed. Trying to rebuild the path...");

                path = builder.destination(bankArea.getRandomCoordinate()).findPath();

                if (path == null || !path.isValid()) {
                    System.out.println("❌ Failed to rebuild path to bank.");
                    return;
                }
            }

            Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1200);
            Execution.delay(800, 1500);
        }

        System.out.println("✅ Arrived at the bank.");

        Execution.delay(8000, 10000);

        // 🔍 Once in range, try to find a bank object and interact
        GameObject nearestBank = GameObjects.newQuery()
                .actions("Bank")
                .results()
                .nearest();

        if (nearestBank != null && nearestBank.interact("Bank")) {
            Execution.delayUntil(Bank::isOpen, Random.nextInt(2000, 4000));
            depositOresInBank();
        } else {
            if (Bank.isOpen())
            {
                depositOresInBank();
            }
            System.out.println("⚠️ Could not find a bank object to interact with.");
        }
    }

    // In your onLoop() or loop logic
    private void logoutAndWaitToRelogin() {
        if (System.currentTimeMillis() >= nextBreakTime) {

            System.out.println("🛑 Logging out...");

            // OSRS logout tab is interface 182
            InterfaceComponent logoutButton = Interfaces.newQuery()
                    .containers(182)
                    .types(InterfaceComponent.Type.LABEL)
                    .texts("Logout")
                    .results()
                    .first();

            if (logoutButton != null && logoutButton.interact("Logout")) {
                Execution.delayUntil(() -> Players.getLocal() == null, Random.nextInt(settings.getBreakMin(), settings.getBreakMax()));
                System.out.println("🔒 Logged out. Waiting to log back in...");

                // Wait until logged in again
                Execution.delayUntil(() -> Players.getLocal() != null, 300_000); // Up to 5 minutes
                System.out.println("✅ Logged back in.");
            } else {
                System.out.println("❌ Failed to find or click the Logout button.");
            }
        }
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) {
            return;
        }

        // 1) Break logic
        if (System.currentTimeMillis() >= nextBreakTime) {
            int breakSeconds = Random.nextInt(settings.getBreakMin(), settings.getBreakMax() + 1);
            logoutAndWaitToRelogin();
            Execution.delay(breakSeconds * 1000); // Wait during logout
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

        // 3) Banking if inventory is full
        if (Inventory.isFull()) {
            logger.info("Walking to bank");
            walkToAndOpenBank();
        }

        // 4) Anti‑ban random actions
        if (Random.nextInt(100) < 5) {
            // e.g., random camera or mouse movement
            logger.info("Anti‑ban action");
            // (you could add Mouse.moveRandomly() here)
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

        if (rock == null) {
            logger.warn("Unable to find rock with name: {}", settings.getOreType());
            if (!movingToBank)
                walkToMiningLocation();
            return;
        }

        if (!rock.isVisible()) {
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


        if (rock.interact("Mine") && Execution.delayUntil(() -> player.getAnimationId() != -1, () -> player.isMoving(), 3000)) {
            // Wait while player is still mining
            Execution.delayWhile(() -> player.getAnimationId() != -1 || player.isMoving(), 100, 6000);
        } else {
            Execution.delay(Random.nextInt(500, 1500));
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
        walkToMiningLocation();
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
