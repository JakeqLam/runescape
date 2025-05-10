package com.runemate.party.common;

import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.location.navigation.basic.BresenhamPath;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;
import com.runemate.pathfinder.Pathfinder;

public class GPTNavigation {

    AntiBan antiBan = new AntiBan();
    private long lastAntiBanTime = 0;
    private final long antiBanCooldown = 30_000; // 30 seconds in ms

    private Coordinate getSafeCoordinateInside(Area area, Pathfinder pathfinder) {
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

    public void walkToArea(Area targetArea, Pathfinder pathfinder) {
        final int MAX_ATTEMPTS = 5;
        int attempts = 0;
        boolean reached = false;

        System.out.println("📍 [GenericCrafter] Attempting to walk inside area: " + targetArea.getCenter());

        while (attempts < MAX_ATTEMPTS && !targetArea.contains(Players.getLocal())) {
            Coordinate target = getSafeCoordinateInside(targetArea, pathfinder);

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
                Execution.delay(gaussianDelay(750, 150, 400, 1200));

                long now = System.currentTimeMillis();
                if (now - lastAntiBanTime > antiBanCooldown && Random.nextInt(0, 100) < 20) { // 20% chance
                    antiBan.rotateCamera();
                    System.out.println("🌀 [AntiBan] Rotated camera (cooldown passed).");
                    lastAntiBanTime = now;
                }
                if (targetArea.contains(Players.getLocal())) {
                    reached = true;
                    break;
                }
            } else {
                System.out.println("⚠️ Path not found on attempt " + (attempts + 1));
            }

            attempts++;
            Execution.delay(gaussianDelay(600, 200, 300, 1000));
        }

        if (!reached) {
            System.out.println("❌ Failed to enter area after " + MAX_ATTEMPTS + " attempts. Falling back.");
            fallBack(targetArea, pathfinder);
        }

        Execution.delay(gaussianDelay(600, 150, 300, 1000));
    }

    public void fallBack(Area target, Pathfinder pathfinder) {
        Path path = BresenhamPath.buildTo(target.getCenter().randomize(10, 10));
        if (path != null && !target.contains(Players.getLocal())) {
            path.step();
            Execution.delay(gaussianDelay(1000, 200, 600, 1500));
        } else {
            Coordinate start = Players.getLocal().getPosition();

            if (start == null) {
                System.out.println("❌ Invalid start or target coordinate.");
                return;
            }

            int newX = start.getX() + (target.getCenter().getX() - start.getX()) / 3;
            int newY = start.getY() + (target.getCenter().getY() - start.getY()) / 3;
            Coordinate oneThird = new Coordinate(newX, newY, start.getPlane());

            Coordinate destination = gaussianRandomized(oneThird, 0, 1, 2);

            path = pathfinder.pathBuilder().destination(destination)
                    .enableHomeTeleport(false)
                    .avoidWilderness(true)
                    .preferAccuracy()
                    .findPath();

            if (path != null && path.isValid() && !target.contains(Players.getLocal())) {
                if (path.step()) {
                    System.out.println("🚶 Stepping to fallback (1/3rd point): " + destination);
                    Execution.delay(gaussianDelay(1000, 200, 600, 1500));
                } else {
                    System.out.println("⚠️ Failed to step to fallback destination.");
                }
            } else {
                System.out.println("❌ Fallback path is invalid.");
            }
        }
    }

    // Gaussian-based delay helper
    private int gaussianDelay(int mean, int deviation, int min, int max) {
        int delay = (int) Random.nextGaussian(min, max, mean, deviation);;
        return Math.max(min, Math.min(max, delay));
    }

    // Gaussian-based coordinate randomization helper
    private Coordinate gaussianRandomized(Coordinate coord, int mean, int deviation, int bounds) {
        int offsetX = (int) Random.nextGaussian(mean, deviation);
        int offsetY = (int) Random.nextGaussian(mean, deviation);
        offsetX = Math.max(-bounds, Math.min(bounds, offsetX));
        offsetY = Math.max(-bounds, Math.min(bounds, offsetY));
        return new Coordinate(coord.getX() + offsetX, coord.getY() + offsetY, coord.getPlane());
    }
}
