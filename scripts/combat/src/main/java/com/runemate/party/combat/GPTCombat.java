package com.runemate.party.combat;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.GroundItem;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank;
import com.runemate.game.api.hybrid.local.hud.interfaces.Health;
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
import com.runemate.pathfinder.Pathfinder;
import com.runemate.ui.setting.annotation.open.SettingsProvider;

import java.util.Arrays;
import java.util.Comparator;

public class GPTCombat extends LoopingBot implements SettingsListener {

    @SettingsProvider(updatable = true)
    private CombatSettings settings;

    private Pathfinder pathfinder;
    private AntiBan antiBan;
    private boolean settingsConfirmed;
    private long nextBreakTime;
    private Coordinate customStartLocation;

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

    private void openObstacle() {
        // Try opening nearby doors, gates, or ladders
        GameObject obstacle = GameObjects.newQuery()
                .names("Door", "Gate", "Ladder", "Staircase", "Web", "Barrier", "Fence")
                .actions("Open", "Climb", "Slash", "Pass", "Push", "Go-through", "Walk-through", "Enter")
                .visibility(3) // limit distance
                .results()
                .nearest();

        if (obstacle != null && obstacle.isVisible() && obstacle.isValid()) {
            String[] actions = obstacle.getDefinition().getActions().toArray(new String[0]);
            if (actions != null) {
                for (String action : actions) {
                    if (action != null && Arrays.asList("Open", "Climb", "Slash", "Pass", "Push", "Go-through", "Walk-through", "Enter").contains(action)) {
                        System.out.println("[SimpleFighter] Interacting with: " + obstacle + " -> " + action);
                        obstacle.interact(action);
                        Execution.delay(1000, 2000);
                        break;
                    }
                }
            }
        }
    }

    public static Coordinate parseCoordinate(String coordinateString) {
        if (coordinateString == null || !coordinateString.contains("(") || !coordinateString.contains(")")) {
            return new Coordinate(3222, 3218, 0); // Fallback to Lumbridge
        }

        // Remove "Coordinate" prefix if present
        coordinateString = coordinateString.replace("Coordinate", "").trim();

        String[] parts = coordinateString.replaceAll("[()]", "").split(", ");
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int plane = Integer.parseInt(parts[2]);
            return new Coordinate(x, y, plane);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return new Coordinate(3222, 3218, 0); // Fallback on parse error
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
                Execution.delayUntil(() -> !Inventory.isFull(), 600, 1200);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onStart(String... args) {
        pathfinder = Pathfinder.create(this);
        antiBan = new AntiBan();
        getEventDispatcher().addListener(this);
        nextBreakTime = System.currentTimeMillis() + (Random.nextInt(10000,11000) * 1000L);
        System.out.println("[SimpleFighter] Started.");
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed || settings == null) return;

        Player player = Players.getLocal();
        if (player == null) return;

        // Break logic
        if (System.currentTimeMillis() >= nextBreakTime) {
            int duration = Random.nextInt(settings.getBreakMinLength(), settings.getBreakMaxLength() + 1);
            System.out.println("[SimpleFighter] Taking break for " + duration + "s");
            if (RuneScape.isLoggedIn()) RuneScape.logout(true);
            Execution.delay(duration * 1000);
            Execution.delayUntil(RuneScape::isLoggedIn, 5000, 30000);
            scheduleNextBreak();
            return;
        }

        // Eat food if low HP
        int hpPercent = (int) (Health.getCurrentPercent());
        if (hpPercent < settings.getEatBelowPercent()) {
            if (Inventory.contains(settings.getFoodType().getCookedName())) {
                System.out.println("[SimpleFighter] Eating food at " + hpPercent + "% HP");
                Inventory.getItems(settings.getFoodType().getCookedName()).first().interact("Eat");
                Execution.delay(800, 1200);
                return;
            } else {
                System.out.println("[SimpleFighter] Out of food, going to bank.");
                walkToBankAndWithdrawFood();
                return;
            }
        }

        // Anti-ban randomly
        maybePerformAntiBan();

        if (settings.shouldUpdateLocation()) {
            Coordinate current = Players.getLocal().getPosition();
            settings.setPlayerLocation(current);
            settings.setShouldUpdateLocation(false); // Reset toggle
        }

        Coordinate startLocation = parseCoordinate(settings.getPlayerLocation());

        Area combatArea = new Area.Rectangular(
                new Coordinate(startLocation.getX() - settings.getCombatRadius(), startLocation.getY() - settings.getCombatRadius(), startLocation.getPlane()),
                new Coordinate(startLocation.getX() + settings.getCombatRadius(), startLocation.getY() + settings.getCombatRadius(), startLocation.getPlane())
        );

        // Combat
        if (!combatArea.contains(player)) {
            walkToArea(combatArea);
            return;
        }

        GroundItem loot = GroundItems.newQuery()
                .within(combatArea)
                .filter(item -> ValuableLoot.getEstimatedValueByName(item.getDefinition().getName()) >= settings.getLootValueThreshold())
                .results()
                .nearest();

        if (Inventory.isFull()) {
            walkToBankAndWithdrawFood();
            return;
        }

        if (loot != null) {
            if (Inventory.isFull()) {
                if (eatToMakeSpaceIfLootNearby(combatArea)) {
                    return; // wait until space is made before trying again
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
                    Execution.delayUntil(() -> !loot.isValid(), 1000, 3000);
                    return;
                }
                Execution.delayUntil(() -> !loot.isValid(), 1000, 3000);
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
            Execution.delay( 300, 450);
            if (target != null && target.isVisible() && target.isValid()) {
                System.out.println("[SimpleFighter] Attacking " + target.getName());
                if (shouldMisclick()) {
                    System.out.println("[SimpleFighter] Misclicking near NPC.");
                    misclickNear(target.getPosition());
                } else {
                    target.interact("Attack");
                }
                Execution.delay( 600, 800);
            } else if (target != null) {
                Camera.turnTo(target);
            } else {
                System.out.println("[SimpleFighter] No target found.");
            }
        }
    }

    private Coordinate getSafeCoordinateInside(Area area) {

        if (pathfinder == null) {
            return area.getCenter();
        }

        for (int i = 0; i < 10; i++) {
            Coordinate candidate = area.getRandomCoordinate();
            if (candidate != null) {
                Path path = pathfinder.pathBuilder()
                        .destination(candidate)
                        .preferAccuracy()
                        .findPath();

                if (path != null && path.isValid()) {
                    return candidate;
                }
            }
        }

        // Fallback to center if no valid coordinate was found
        return area.getCenter();
    }

    private void walkToArea(Area targetArea) {
        final int MAX_ATTEMPTS = 5;
        int attempts = 0;
        boolean reached = false;

        System.out.println("📍 [GenericCrafter] Attempting to walk inside area: " + targetArea.getCenter());

        while (attempts < MAX_ATTEMPTS && !targetArea.contains(Players.getLocal())) {
            Coordinate target = getSafeCoordinateInside(targetArea);

            Path path = pathfinder.pathBuilder()
                    .destination(target)
                    .enableHomeTeleport(false)
                    .enableTeleports(false)
                    .avoidWilderness(true)
                    .preferAccuracy()
                    .findPath();

            if (path != null && path.isValid()) {
                System.out.println("✅ Valid path to inside area: " + target);
                path.step();
                Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1200);
                Execution.delay(500, 1000);

                if (targetArea.contains(Players.getLocal())) {
                    reached = true;
                    break;
                }
            } else {
                System.out.println("⚠️ Path not found on attempt " + (attempts + 1));
            }

            attempts++;
            Execution.delay(500, 1000);
        }

        if (!reached) {
            System.out.println("❌ Failed to enter area after " + MAX_ATTEMPTS + " attempts. Falling back.");
            fallBack(targetArea);
        }
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

    private Coordinate getNearestBank() {
        Coordinate playerPos = Players.getLocal().getPosition();
        return Arrays.stream(BANK_LOCATIONS)
                .min(Comparator.comparingInt(bank -> (int) bank.distanceTo(playerPos)))
                .orElse(null);
    }

    private void walkToBankAndWithdrawFood() {
        Area bankArea = getNearestBank().getArea();

        while (!bankArea.contains(Players.getLocal())) {
            walkToArea(bankArea);
        }

        Execution.delay( 2000, 3500);

        if (!Bank.isOpen()) {
            Bank.open();
            Execution.delayUntil(Bank::isOpen, 2000, 4000);
        }

        if (Bank.isOpen()) {
            Bank.depositInventory();
            Execution.delayUntil(() -> Inventory.isEmpty(), 1000, 3000);

            if (!Inventory.contains(settings.getFoodType().getCookedName())) {
                Bank.withdraw(settings.getFoodType().getCookedName(), settings.getFoodWithdrawAmount());
                Execution.delay(500, 1000);
            }

            Bank.close();
            Execution.delay(300, 800);
        }
    }

    private void scheduleNextBreak() {
        int interval = Random.nextInt(settings.getBreakMin(), settings.getBreakMax() + 1);
        nextBreakTime = System.currentTimeMillis() + interval * 1000L;
        System.out.println("[SimpleFighter] Next break in " + interval + "s");
    }

    @Override
    public void onStop() {
        System.out.println("[SimpleFighter] Stopped.");
    }

    @Override
    public void onSettingChanged(SettingChangedEvent settingChangedEvent) {

    }

    @Override
    public void onSettingsConfirmed() {
        settingsConfirmed = true;
    }
}
