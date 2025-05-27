package com.runemate.party.combat;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.GroundItem;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
import com.runemate.game.api.hybrid.local.hud.interfaces.InterfaceWindows;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.basic.BresenhamPath;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.GroundItems;
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

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;

public class GPTCombat extends LoopingBot implements SettingsListener {

    @SettingsProvider(updatable = true)
    private CombatSettings settings;

    private Pathfinder pathfinder;
    private AntiBan antiBan;
    private GPTNavigation navigation;
    private boolean settingsConfirmed;

    private static final Coordinate[] BANK_LOCATIONS = new Coordinate[] {
            new Coordinate(3182, 3436, 0),   // Varrock West
            new Coordinate(3253, 3420, 0),   // Varrock East
            new Coordinate(3094, 3492, 0),   // Edgeville
            new Coordinate(3208, 3219, 2),   // Lumbridge (top floor)
            new Coordinate(3267, 3167, 0),   // Al Kharid
            new Coordinate(2946, 3368, 0),   // Falador West
            new Coordinate(3013, 3355, 0),   // Falador East
            new Coordinate(3093, 3243, 0),   // Draynor
            new Coordinate(2615, 3332, 0)    // Ardougne West
    };
    private long earlyBankCooldown;
    private long lastEarlyBankCheck;


    private long lastAttackAttemptTime = 0;
    private Coordinate lastPositionBeforeStuck = null;
    private int stuckCheckCounter = 0;
    private static final int MAX_STUCK_CHECKS = 3;

    private boolean isPlayerStuck() {
        Player player = Players.getLocal();
        if (player == null) return false;

        // Check if we've been trying to attack for too long without success
        if (System.currentTimeMillis() - lastAttackAttemptTime > 10000) { // 10 seconds
            Coordinate currentPos = player.getPosition();
            if (lastPositionBeforeStuck != null && currentPos.distanceTo(lastPositionBeforeStuck) < 2) {
                stuckCheckCounter++;
                if (stuckCheckCounter >= MAX_STUCK_CHECKS) {
                    System.out.println("[Stuck Detection] Player appears stuck at position: " + currentPos);
                    return true;
                }
            } else {
                stuckCheckCounter = 0;
                lastPositionBeforeStuck = currentPos;
            }
        }
        return false;
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

    private void openObstacle() {
        Player player = Players.getLocal();
        if (player == null) return;

        // First check for obstacles near the player
        GameObject obstacle = GameObjects.newQuery()
                .within(new Area.Circular(player.getPosition(), 3)) // 3 tile radius
                .names("Door", "Gate", "Ladder", "Staircase", "Web", "Barrier", "Fence")
                .actions("Open", "Climb", "Slash", "Pass", "Push", "Go-through", "Walk-through", "Enter")
                .visibility(3)
                .results()
                .nearest();

        if (obstacle != null && obstacle.isVisible() && obstacle.isValid()) {
            System.out.println("[Obstacle Handling] Found obstacle: " + obstacle);
            String[] actions = obstacle.getDefinition().getActions().toArray(new String[0]);
            if (actions != null) {
                for (String action : actions) {
                    if (action != null && Arrays.asList("Open", "Climb", "Slash", "Pass", "Push", "Go-through", "Walk-through", "Enter").contains(action)) {
                        System.out.println("[Obstacle Handling] Attempting to " + action + " the obstacle");
                        if (obstacle.interact(action)) {
                            Execution.delay((int)getGaussian(1000, 2000, 1500, 300));
                            return;
                        }
                    }
                }
            }
        }
    }

    public static Coordinate parseCoordinate(String coordinateString) {
        if (coordinateString == null || !coordinateString.contains("(") || !coordinateString.contains(")")) {
            return new Coordinate(3222, 3218, 0); // Fallback to Lumbridge
        }

        coordinateString = coordinateString.replace("Coordinate", "").trim();
        String[] parts = coordinateString.replaceAll("[()]", "").split(", ");
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int plane = Integer.parseInt(parts[2]);
            return new Coordinate(x, y, plane);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return new Coordinate(3222, 3218, 0);
        }
    }

    private boolean eatToMakeSpaceIfLootNearby(Area combatArea) {
        if (!Inventory.isFull()) return false;

        GroundItem lootNearby = GroundItems.newQuery()
                .within(combatArea)
                .filter(item -> ValuableLoot.getEstimatedValueByName(item.getDefinition().getName()) >= settings.getLootValueThreshold())
                .results()
                .nearest();

        if (lootNearby != null) {
            String foodName = settings.getFoodType().getCookedName();
            if (Inventory.contains(foodName)) {
                System.out.println("[SimpleFighter] Eating food to make space for loot: " + lootNearby.getDefinition().getName());
                Inventory.getItems(foodName).first().interact("Eat");
                Execution.delayUntil(() -> !Inventory.isFull(),
                        (int)getGaussian(600, 1200, 900, 200));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStart(String... args) {
        pathfinder = Pathfinder.create(this);
        antiBan = new AntiBan();
        navigation = new GPTNavigation();
        getEventDispatcher().addListener(this);
        System.out.println("[SimpleFighter] Started.");

        if (InterfaceWindows.getInventory().isOpen()) {
            System.out.println("📂 Inventory tab is open.");
        } else {
            System.out.println("📂 Inventory tab is NOT open. Opening now...");
            Keyboard.pressKey(KeyEvent.VK_ESCAPE); // VK_ESCAPE is usually the Inventory hotkey
            Execution.delay(300, 600);
        }
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed || settings == null) return;

        if (!InterfaceWindows.getInventory().isOpen()) {
            System.out.println("📂 Inventory tab is NOT open. Opening now...");
            Keyboard.pressKey(KeyEvent.VK_F3); // F3 is usually the Inventory hotkey
            Execution.delay(300, 600);
        }

        Player player = Players.getLocal();
        if (player == null) return;

        antiBan.performBreakLogic(settings.getBreakMin(), settings.getBreakMax());

        int hpPercent = (int) (Health.getCurrentPercent());
        if (hpPercent < settings.getEatBelowPercent()) {
            if (Inventory.contains(settings.getFoodType().getCookedName())) {
                System.out.println("[SimpleFighter] Eating food at " + hpPercent + "% HP");
                Inventory.getItems(settings.getFoodType().getCookedName()).first().interact("Eat");
                Execution.delay((int)getGaussian(800, 1200, 1000, 150));
                return;
            } else {
                System.out.println("[SimpleFighter] Out of food, going to bank.");
                walkToBankAndWithdrawFood();
                return;
            }
        }

        antiBan.maybePerformAntiBan();

        if (settings.shouldUpdateLocation()) {
            Coordinate current = Players.getLocal().getPosition();
            settings.setPlayerLocation(current);
            settings.setShouldUpdateLocation(false);
        }

        Coordinate startLocation = parseCoordinate(settings.getPlayerLocation());
        Area combatArea = new Area.Rectangular(
                new Coordinate(startLocation.getX() - settings.getCombatRadius(), startLocation.getY() - settings.getCombatRadius(), startLocation.getPlane()),
                new Coordinate(startLocation.getX() + settings.getCombatRadius(), startLocation.getY() + settings.getCombatRadius(), startLocation.getPlane())
        );

        if (!combatArea.contains(player)) {
            navigation.walkToArea(combatArea, pathfinder);
            return;
        }

        GroundItem loot = GroundItems.newQuery()
                .within(combatArea)
                .filter(item -> ValuableLoot.getEstimatedValueByName(item.getDefinition().getName()) >= settings.getLootValueThreshold())
                .results()
                .nearest();

        // Bank if inventory full
        int inventoryCount = Inventory.getItems().size();
        long currentTime = System.currentTimeMillis();

        boolean shouldCheckEarlyBank = (currentTime - lastEarlyBankCheck) > earlyBankCooldown;
        boolean earlyBankChance = false;

        if (shouldCheckEarlyBank) {
            earlyBankChance = inventoryCount >= 24 && Random.nextInt(100) < 5;

            // Always reset cooldown to prevent repeated triggers
            earlyBankCooldown = (long) getGaussian(2500, 4000, 3200, 500);
            lastEarlyBankCheck = currentTime;
        }

        if (Inventory.isFull() || earlyBankChance) {
            System.out.println("Banking (reason: " +
                    (Inventory.isFull() ? "inventory full" : "early bank at " + inventoryCount + " items") + ")");

            walkToBankAndWithdrawFood();
            return;
        }
        
        if (loot != null) {
            if (Inventory.isFull()) {
                if (eatToMakeSpaceIfLootNearby(combatArea)) {
                    return;
                } else {
                    System.out.println("[SimpleFighter] Inventory full and no food to eat for loot: " + loot.getDefinition().getName());
                    walkToBankAndWithdrawFood();
                    return;
                }
            }

            if (loot.isVisible() && loot.isValid()) {
                System.out.println("[SimpleFighter] Looting: " + loot.getDefinition().getName());
                if (shouldMisclick()) {
                    System.out.println("[SimpleFighter] Misclicking near loot.");
                    misclickNear(loot.getPosition());
                } else {
                    loot.interact("Take");
                    Execution.delayUntil(() -> !loot.isValid(),
                            (int)getGaussian(1000, 3000, 2000, 700));
                    return;
                }
                Execution.delayUntil(() -> !loot.isValid(),
                        (int)getGaussian(1000, 3000, 2000, 700));
                return;
            } else {
                Camera.turnTo(loot);
            }
        }

        if (player.getAnimationId() == -1 && !player.isMoving() && player.getTarget() == null) {
            Npc target = Npcs.newQuery()
                    .names(settings.getMonsterName())
                    .within(combatArea)
                    .actions("Attack")
                    .filter(npc -> {
                        Player interactingPlayer = (Player) npc.getTarget();
                        return interactingPlayer == null || interactingPlayer.equals(Players.getLocal());
                    })
                    .results()
                    .nearest();

            Execution.delay((int)getGaussian(300, 450, 375, 50));

            if (target != null) {
                lastAttackAttemptTime = System.currentTimeMillis();

                // Check for obstacles if we're stuck
                if (isPlayerStuck()) {
                    System.out.println("[Obstacle Handling] Player stuck, checking for obstacles...");
                    openObstacle();
                    stuckCheckCounter = 0; // Reset counter after handling
                    Execution.delay((int)getGaussian(1000, 2000, 1500, 300));
                    return; // Let the next loop iteration handle attacking again
                }

                if (target.isVisible() && target.isValid()) {
                    System.out.println("[SimpleFighter] Attacking " + target.getName());
                    if (shouldMisclick()) {
                        System.out.println("[SimpleFighter] Misclicking near NPC.");
                        misclickNear(target.getPosition());
                    } else {
                        target.interact("Attack");
                    }
                    Execution.delay((int)getGaussian(600, 800, 700, 70));
                } else {
                    Camera.turnTo(target);
                }
            } else {
                System.out.println("[SimpleFighter] No target found.");
            }
        }
    }

    private Coordinate getNearestBank() {
        Coordinate playerPos = Players.getLocal().getPosition();
        return Arrays.stream(BANK_LOCATIONS)
                .min(Comparator.comparingInt(bank -> (int) bank.distanceTo(playerPos)))
                .orElse(null);
    }

    private void walkToBankAndWithdrawFood() {
        Area bankArea = getNearestBank().getArea();

        while (!bankArea.contains(Players.getLocal())) {
            navigation.walkToArea(bankArea, pathfinder);
        }

        Execution.delay((int)getGaussian(2000, 3500, 2750, 500));

        while (!Bank.isOpen()) {
            Bank.open();
            Execution.delayUntil(Bank::isOpen,
                    (int)getGaussian(2000, 4000, 3000, 700));

            if (!Bank.isOpen()) {
                // fallback: click center of bank area
                Coordinate clickPoint = bankArea.getCenter().randomize(1,1);
                Mouse.move(clickPoint);
                Execution.delay((int) getGaussian(200, 400, 300, 70));
                Mouse.click(Mouse.Button.LEFT);

                Execution.delayUntil(Bank::isOpen,
                        (int) getGaussian(4000, 6000, 5000, 700),
                        (int) getGaussian(6000, 8000, 7000, 700));
            }
        }

        if (Bank.isOpen()) {
            Bank.depositInventory();
            Execution.delayUntil(() -> Inventory.isEmpty(),
                    (int)getGaussian(1000, 3000, 2000, 700));

            if (!Inventory.contains(settings.getFoodType().getCookedName())) {
                int foodAmount = (int) getGaussian(
                        settings.getFoodWithdrawAmount() - 1,  // min
                        settings.getFoodWithdrawAmount() + 1,  // max
                        settings.getFoodWithdrawAmount(),      // mean
                        0.8                                    // stdDev — small enough to stay near the mean
                );

                Bank.withdraw(settings.getFoodType().getCookedName(), foodAmount);
                Execution.delayUntil(() -> Inventory.contains(settings.getFoodType().getCookedName()),
                        (int) getGaussian(2000, 3000, 2500, 300));
            }

            Execution.delay((int)getGaussian(1000, 2000, 1500, 100));

            Bank.close();
            Execution.delay((int)getGaussian(300, 800, 550, 150));
        }
    }

    @Override
    public void onStop() {
        System.out.println("[SimpleFighter] Stopped.");
    }

    @Override
    public void onSettingChanged(SettingChangedEvent settingChangedEvent) {}

    @Override
    public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }
}