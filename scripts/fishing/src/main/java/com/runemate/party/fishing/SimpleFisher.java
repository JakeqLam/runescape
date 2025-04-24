package com.runemate.party.fishing;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SimpleFisher extends LoopingBot implements SettingsListener {

    private static final List<Area> BANK_LOCATIONS = Arrays.asList(
            new Area.Rectangular(new Coordinate(3207, 3218, 2), new Coordinate(3211, 3222, 2)), // Lumbridge Castle 2nd floor bank
            new Area.Rectangular(new Coordinate(3181, 3436, 0), new Coordinate(3186, 3444, 0)), // Varrock West
            new Area.Rectangular(new Coordinate(3251, 3420, 0), new Coordinate(3257, 3426, 0)), // Varrock East
            new Area.Rectangular(new Coordinate(3161, 3476, 0), new Coordinate(3169, 3486, 0)), // Grand Exchange
            new Area.Rectangular(new Coordinate(3092, 3489, 0), new Coordinate(3099, 3499, 0)), // Edgeville
            new Area.Rectangular(new Coordinate(3010, 3352, 0), new Coordinate(3016, 3359, 0)), // Falador East
            new Area.Rectangular(new Coordinate(2942, 3367, 0), new Coordinate(2946, 3375, 0)), // Falador West
            new Area.Rectangular(new Coordinate(3092, 3240, 0), new Coordinate(3099, 3245, 0)), // Draynor
            new Area.Rectangular(new Coordinate(3268, 3161, 0), new Coordinate(3273, 3170, 0)), // Al Kharid
            new Area.Rectangular(new Coordinate(2805, 3439, 0), new Coordinate(2810, 3444, 0)), // Catherby
            new Area.Rectangular(new Coordinate(2721, 3491, 0), new Coordinate(2726, 3494, 0)), // Camelot
            new Area.Rectangular(new Coordinate(2723, 3492, 0), new Coordinate(2730, 3499, 0)), // Seers
            new Area.Rectangular(new Coordinate(2611, 3330, 0), new Coordinate(2617, 3337, 0)), // Ardougne North
            new Area.Rectangular(new Coordinate(2652, 3283, 0), new Coordinate(2658, 3289, 0)), // Ardougne South
            new Area.Rectangular(new Coordinate(2610, 3090, 0), new Coordinate(2615, 3095, 0)), // Yanille
            new Area.Rectangular(new Coordinate(1634, 3762, 0), new Coordinate(1640, 3767, 0))  // Zeah
    );

    @SettingsProvider(updatable = true)
    private FisherSettings settings;

    private Pathfinder pathfinder;
    private AntiBan antiBan;
    private boolean settingsConfirmed;
    private long nextBreakTime;

    @Override
    public void onStart(String... args) {
        antiBan = new AntiBan();
        getEventDispatcher().addListener(this);
        pathfinder = Pathfinder.create(this);
        scheduleNextBreak();
        System.out.println("[SimpleFisher] Started");
    }

    @Override
    public void onLoop() {
        if (!settingsConfirmed) return;
        Player player = Players.getLocal();
        if (player == null) return;

        // Break logic
        if (System.currentTimeMillis() >= nextBreakTime) {
            int length = Random.nextInt(settings.getBreakMinLength(), settings.getBreakMaxLength() + 1);
            System.out.println("[SimpleFisher] Taking break for " + length + "s");
            if (RuneScape.isLoggedIn()) { RuneScape.logout(true); }
            Execution.delay(length * 1000);
            Execution.delayUntil(RuneScape::isLoggedIn, 5000, 30000);
            scheduleNextBreak();
            return;
        }

        // Bank if inventory full
        if (Inventory.isFull()) {
            System.out.println("[SimpleFisher] Inventory full, banking");
            walkToAndDeposit();
            return;
        }

        // Anti-ban randomly
        if (Random.nextInt(100) < 3) {
            System.out.println("[SimpleFisher] Performing anti-ban");
            antiBan.performAntiBan();
        }

        // Fishing
        FishingMethod method = settings.getMethod();
        FishingSpot spotEnum = settings.getSpot();
        Area spotArea = spotEnum.getArea();

        Npc spot = Npcs.newQuery()
                .names("Fishing spot")
                .actions(method.getActionName())
                .within(spotArea)
                .results().nearest();

        if (spot != null && spot.isVisible()) {
            if (player.getAnimationId() == -1 && !player.isMoving()) {
                if (Random.nextInt(0, 100) < 8) {
                    System.out.println("[SimpleFisher] Simulating misclick near spot");
                    Mouse.move(spot.getPosition().randomize(3, 6));
                    Execution.delay(Random.nextInt(200, 500));
                    Mouse.click(Mouse.Button.LEFT);
                    Execution.delay(Random.nextInt(800, 1200));
                }
                System.out.println("[SimpleFisher] Interacting with spot: " + spotEnum + " via " + method);
                if (spot.interact(method.getActionName())) {
                    Execution.delayUntil(
                            () -> player.getAnimationId() != -1,
                            Random.nextInt(500,1000),
                            Random.nextInt(3000,5000)
                    );
                }
            }
        } else {
            if (spot != null) {
                System.out.println("[SimpleFisher] Turning camera to spot");
                Camera.turnTo(spot);
                Execution.delay(200,400);
            } else {
                walkToArea(spotArea);
            }
        }
    }

    private void walkToArea(Area area) {
        if (area.contains(Players.getLocal())) return;
        System.out.println("[SimpleFisher] Walking to area: " + area);
        int attempts = 0;
        boolean reached = false;
        while (attempts < 5 && !area.contains(Players.getLocal())) {
            Coordinate dest = attempts < 3 ? area.getRandomCoordinate() : area.getCenter();
            Path path = pathfinder.pathBuilder()
                    .destination(dest)
                    .preferAccuracy()
                    .enableHomeTeleport(false)
                    .findPath();
            if (path != null && path.isValid()) {
                int fails = 0;
                while (!area.contains(Players.getLocal()) && path.step()) {
                    if (Random.nextInt(100) < 2) { antiBan.performAntiBan(); }
                    Execution.delayUntil(() -> !Players.getLocal().isMoving(), 300, 1200);
                    Execution.delay(800,1500);
                }
                if (area.contains(Players.getLocal())) { reached = true; break; }
            }
            attempts++;
            Execution.delay(500,1000);
        }
        if (!reached) {
            System.out.println("[SimpleFisher] Fallback navigation");
            Path fb = BresenhamPath.buildTo(area.getCenter().randomize(10,10));
            if (fb != null && fb.step()) {
                Execution.delay(800,1500);
            } else {
                Coordinate start = Players.getLocal().getPosition();
                Coordinate center = area.getCenter();
                Coordinate oneThird = new Coordinate(
                        start.getX() + (center.getX()-start.getX())/3,
                        start.getY() + (center.getY()-start.getY())/3,
                        start.getPlane()
                ).randomize(1,1);
                Path fn = pathfinder.pathBuilder().destination(oneThird)
                        .avoidWilderness(true).preferAccuracy().findPath();
                if (fn != null && fn.isValid()) fn.step();
                Execution.delay(800,1500);
            }
        }
    }

    private void walkToAndDeposit() {
        // Check if Lumbridge is the fishing spot
        boolean isFishingInLumbridge = settings.getSpot().getArea().contains(Players.getLocal().getPosition());

        Area closestBank;
        if (isFishingInLumbridge) {
            // Explicitly set Lumbridge Bank if fishing in Lumbridge
            closestBank = BANK_LOCATIONS.get(0); // Lumbridge Bank is at index 0
            System.out.println("[SimpleFisher] Fishing in Lumbridge, choosing Lumbridge Bank.");
        } else {
            // Choose nearest bank area for other fishing spots
            closestBank = BANK_LOCATIONS.stream()
                    .min(Comparator.comparingInt(b -> (int) b.getCenter().distanceTo(Players.getLocal().getPosition())))
                    .orElse(BANK_LOCATIONS.get(1)); // Fallback to the second closest bank if necessary
            System.out.println("[SimpleFisher] Walking to bank area: " + closestBank);
        }

        // Walk to bank area with fallback
        walkToArea(closestBank);

        // Interact with bank booth/chest
        GameObject bank = GameObjects.newQuery()
                .actions("Bank")
                .within(closestBank)
                .results()
                .nearest();

        if (bank != null && bank.interact("Bank")) {
            Execution.delayUntil(Bank::isOpen, 2000, 4000);
            System.out.println("[SimpleFisher] Bank opened, depositing items");
            // Deposit all except tools and bait
            FishingMethod method = settings.getMethod();
            String[] requiredItems = method.getRequiredItems();

            if (requiredItems.length > 0) {
                Bank.depositAllExcept(requiredItems);
                System.out.println("[SimpleFisher] Kept: " + Arrays.toString(requiredItems));
            } else {
                Bank.depositInventory();
                System.out.println("[SimpleFisher] Deposited all items");
            }
            Execution.delayUntil(() -> !Bank.isOpen(), 1000, 3000);
            System.out.println("[SimpleFisher] Deposit complete, closing bank");
        } else {
            System.out.println("[SimpleFisher] Failed to open bank, fallback clicking bank area");
            // fallback: click center of bank area
            Coordinate clickPoint = closestBank.getCenter().randomize(2, 2);
            Mouse.move(clickPoint);
            Execution.delay(200, 400);
            Mouse.click(Mouse.Button.LEFT);
            Execution.delayUntil(Bank::isOpen, 2000, 4000);
        }
    }

    private void scheduleNextBreak() {
        int interval = Random.nextInt(settings.getBreakMin(), settings.getBreakMax() + 1);
        nextBreakTime = System.currentTimeMillis() + interval * 1000L;
        System.out.println("[SimpleFisher] Next break in " + interval + "s");
    }

    @Override public void onStop() { System.out.println("[SimpleFisher] Stopped."); }
    @Override public void onSettingChanged(SettingChangedEvent e) { /* live update if needed */ }
    @Override public void onSettingsConfirmed() { settingsConfirmed = true; }
}
