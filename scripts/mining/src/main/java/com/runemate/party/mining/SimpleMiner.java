package com.runemate.party.mining;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
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

import java.awt.event.KeyEvent;
import java.util.*;

public class SimpleMiner extends LoopingBot implements SettingsListener {

    private static final Logger logger = LogManager.getLogger(SimpleMiner.class);

    @SettingsProvider(updatable = true)
    private MinerSettings settings;

    // Cached settings
    private boolean useP2P;
    private boolean movingToBank;
    private AntiBan antiBan;
    private GPTNavigation navigation;
    private boolean settingsConfirmed;
    private Pathfinder pathfinder;
    private final Set<Coordinate> contestedRocks = new HashSet<>();
    private long contestedClearTime = System.currentTimeMillis();
    private long lastEarlyBankCheck;
    private long earlyBankCooldown;

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
    }

    public void depositOresInBank(Area bank) {

        Bank.open();
        Execution.delayUntil(Bank::isOpen,
                (int) getGaussian(6000, 8000, 7000, 700));

        if (Bank.isOpen()) {
            Bank.depositInventory();
            Execution.delayUntil(() -> !Bank.isOpen(),
                    (int)getGaussian(2000, 4000, 3000, 700));
            System.out.println("Deposited all items in the bank.");

            Execution.delay((int)getGaussian(1000, 2000, 1500, 100));


        } else {
            System.out.println("Bank is not open, cannot deposit items.");
            // fallback: click center of bank area
            Coordinate clickPoint = bank.getCenter().randomize(1,1);
            Mouse.move(clickPoint);
            Execution.delay((int) getGaussian(200, 400, 300, 70));
            Mouse.click(Mouse.Button.LEFT);

            Execution.delayUntil(Bank::isOpen,
                    (int) getGaussian(4000, 6000, 5000, 700),
                    (int) getGaussian(6000, 8000, 7000, 700));
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

        while (!closestBank.contains(Players.getLocal())) {
            System.out.println("🚶 Attempting to walk to bank...");
            navigation.walkToArea(closestBank, pathfinder);
        }

        GameObject nearestBank = GameObjects.newQuery()
                .actions("Bank")
                .results()
                .nearest();

        while (nearestBank != null && !Bank.isOpen() && closestBank.contains(Players.getLocal())) {
             depositOresInBank(closestBank);
             System.out.println("⚠️ Could not find a bank object to interact with.");
        }

        if (Bank.isOpen()) {
            Bank.close();
            Execution.delay((int)getGaussian(1000, 2000, 1500, 300));
            System.out.println("Bank window closed.");
            movingToBank = false;
            navigation.walkToArea(settings.getLocation().getArea(), pathfinder);
        }
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) {
            return;
        }

        if (!InterfaceWindows.getInventory().isOpen()) {
            System.out.println("📂 Inventory tab is NOT open. Opening now...");
            Keyboard.pressKey(KeyEvent.VK_ESCAPE); // ESC is usually the Inventory hotkey
            Execution.delay(300, 600);
        }

        antiBan.performBreakLogic(settings.getBreakMin(), settings.getBreakMax());

        // Bank if inventory full
        int inventoryCount = Inventory.getItems().size();
        long currentTime = System.currentTimeMillis();

// Only evaluate early bank once every cooldown period
        boolean shouldCheckEarlyBank = (currentTime - lastEarlyBankCheck) > earlyBankCooldown;
        boolean earlyBankChance = false;

        if (shouldCheckEarlyBank) {
            earlyBankChance = inventoryCount >= 24 && Random.nextInt(100) < 5;

            // Reset cooldown timer whether or not we bank
            earlyBankCooldown = (long) getGaussian(2500, 4000, 3200, 500);
            lastEarlyBankCheck = currentTime;
        }

        if (Inventory.isFull() || earlyBankChance) {
            System.out.println("Banking (reason: " +
                    (Inventory.isFull() ? "inventory full" : "early bank at " + inventoryCount + " items") + ")");

            walkToAndOpenBank();
            return;
        }

        antiBan.maybePerformAntiBan();

        Player player = Players.getLocal();
        if (player == null) {
            logger.warn("Unable to find local player");
            return;
        }

        GameObject rock;

        if (settings.mineOtherAreaIfContested()) {
            maybeClearContestedRocks();

            String oreName = settings.getOreType().toString();
            rock = GameObjects.newQuery()
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
                    Execution.delay((int)getGaussian(500, 1000, 750, 150));
                    return;
                }
            }
        } else {
            // Fallback for when contest check is off
            rock = GameObjects.newQuery()
                    .names(settings.getOreType().toString())
                    .results()
                    .nearest();
        }

        if (!movingToBank && !settings.getLocation().getArea().contains(Players.getLocal())) {
            navigation.walkToArea(settings.getLocation().getArea(), pathfinder);
        }

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
            if (player.getAnimationId() == -1 && !player.isMoving()) {
                if (rock != null) {
                    // Step 2: Misclick simulation (8% chance)
                    if (Random.nextInt(0, 100) < 8) {
                        System.out.println("🤖 Simulating misclick...");
                        Coordinate misclickTile = rock.getPosition().randomize(1, 1);
                        Mouse.move(misclickTile);
                        Execution.delay((int) getGaussian(200, 500, 350, 100));

                        // Only click if mouse is NOT hovering an NPC (prevents attacking)
                        if (!Npcs.newQuery()
                                .actions("Attack")
                                .filter(npc -> npc.contains(Mouse.getPosition()))
                                .results()
                                .isEmpty()) {
                            System.out.println("⚠️ Misclick landed on a monster. Skipping click.");
                        } else {
                            Mouse.click(Mouse.Button.LEFT);
                            Execution.delay((int) getGaussian(800, 1200, 1000, 150));
                        }
                    }

                    // Actual mining attempt
                    if (rock.interact("Mine")) {
                        System.out.println("⛏️ Mining " + "...");

                        if (!Execution.delayUntil(() -> player.getAnimationId() != -1 || player.isMoving(),
                                (int)getGaussian(400, 800, 600, 150),
                                (int)getGaussian(3000, 5000, 4000, 700))) {
                            System.out.println("❌ Failed to start mining.");
                            return;
                        }
                    }
                }
            } else {
                Execution.delay((int)getGaussian(300, 700, 500, 150));
            }
        }
    }

    private void maybeClearContestedRocks() {
        if (System.currentTimeMillis() - contestedClearTime > 150_000) { // 2.5 minutes
            contestedRocks.clear();
            contestedClearTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onStop() {
        logger.info("Universal Miner stopped.");
    }

    @Override
    public void onSettingChanged(SettingChangedEvent event) {
        applySettings();
    }

    @Override
    public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }

    private void applySettings() {
        useP2P = settings.getUseP2P();
    }
}