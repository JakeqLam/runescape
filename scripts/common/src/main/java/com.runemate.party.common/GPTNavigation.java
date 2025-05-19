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
    private static final int MAX_ATTEMPTS = 5;
    private static final long ANTI_BAN_COOLDOWN = 30_000; // 30 seconds in ms
    private static final int MIN_DISTANCE_FOR_ANTIBAN = 10; // Minimum distance to trigger antiban

    private final AntiBan antiBan = new AntiBan();
    private long lastAntiBanTime = 0;
    private Coordinate lastPosition;
    private long lastMovementTime = 0;
    private int stuckCounter = 0;

    public void walkToArea(Area targetArea, Pathfinder pathfinder) {
        if (targetArea == null || pathfinder == null) {
            System.out.println("❌ Invalid target area or pathfinder");
            return;
        }

        System.out.println("📍 [Navigation] Attempting to walk to area: " + targetArea.getCenter());

        int attempts = 0;
        boolean reached = false;

        while (attempts < MAX_ATTEMPTS && !reached) {
            if (targetArea.contains(Players.getLocal())) {
                reached = true;
                break;
            }

            Coordinate target = getOptimalCoordinate(targetArea, pathfinder);
            if (target == null) {
                System.out.println("⚠️ Could not find valid coordinate in target area");
                break;
            }

            Path path = buildPath(pathfinder, target);
            if (path == null || !path.isValid()) {
                System.out.println("⚠️ Path not found on attempt " + (attempts + 1));
                attempts++;
                Execution.delay(gaussianDelay(600, 200, 300, 1000));
                continue;
            }

            System.out.println("✅ Found valid path to: " + target);
            if (executePath(path, targetArea)) {
                reached = true;
            } else {
                attempts++;
                handleStuckSituation(targetArea, pathfinder);
            }
        }

        if (!reached) {
            System.out.println("❌ Failed to reach area after " + MAX_ATTEMPTS + " attempts");
            fallBack(targetArea, pathfinder);
        }
    }

    private Coordinate getOptimalCoordinate(Area area, Pathfinder pathfinder) {
        Coordinate center = area.getCenter();
        if (center == null) return null;

        // First try center point
        if (isReachable(center, pathfinder)) {
            return center;
        }

        // Then try random points
        for (int i = 0; i < 10; i++) {
            Coordinate candidate = area.getRandomCoordinate();
            if (candidate != null && isReachable(candidate, pathfinder)) {
                return candidate;
            }
        }

        // Fallback to center if no valid coordinate was found
        return center;
    }

    private boolean isReachable(Coordinate target, Pathfinder pathfinder) {
        if (target == null) return false;

        Path path = pathfinder.pathBuilder()
                .destination(target)
                .preferAccuracy()
                .findPath();

        return path != null && path.isValid();
    }

    private Path buildPath(Pathfinder pathfinder, Coordinate target) {
        return pathfinder.pathBuilder()
                .destination(target)
                .enableHomeTeleport(true)
                .enableTeleports(false)
                .avoidWilderness(true)
                .preferAccuracy()
                .findPath();
    }

    private boolean executePath(Path path, Area targetArea) {
        Coordinate startPos = Players.getLocal().getPosition();

        if (path.step()) {
            // Always wait for movement to complete (but with shorter timeout)
            Execution.delayUntil(() -> !Players.getLocal().isMoving(), 200, 800);

            // 80% chance to wait, 20% chance to continue immediately
            if (Random.nextInt(1, 100) <= 80) {
                // Randomize the wait time when we do wait
                Execution.delay(gaussianDelay(600, 150, 300, 1200));

                // Small chance (20%) of an extra "hesitation" delay
                if (Random.nextInt(1, 100) <= 20) {
                    Execution.delay(gaussianDelay(300, 100, 150, 600));
                }
            } else {
                // Even when continuing immediately, add a tiny delay (humans can't react instantly)
                Execution.delay(Random.nextInt(50, 200));
            }

            Coordinate currentPos = Players.getLocal().getPosition();
            if (currentPos != null && currentPos.equals(startPos)) {
                stuckCounter++;
                return false;
            }

            handleAntiBan(currentPos);
            return targetArea.contains(currentPos);
        }
        return false;
    }

    private void handleAntiBan(Coordinate currentPos) {
        long now = System.currentTimeMillis();
        double distanceMoved = currentPos != null && lastPosition != null ?
                currentPos.distanceTo(lastPosition) : 0;

        if (distanceMoved > MIN_DISTANCE_FOR_ANTIBAN &&
                now - lastAntiBanTime > ANTI_BAN_COOLDOWN &&
                Random.nextInt(0, 100) < 20) {

            antiBan.rotateCamera();
            System.out.println("🌀 [AntiBan] Rotated camera (cooldown passed)");
            lastAntiBanTime = now;
        }

        lastPosition = currentPos;
    }

    private void handleStuckSituation(Area targetArea, Pathfinder pathfinder) {
        stuckCounter++;
        System.out.println("⚠️ Possible stuck situation detected (" + stuckCounter + "/3)");

        if (stuckCounter >= 3) {
            System.out.println("🚧 Performing stuck recovery actions");
            antiBan.rotateCamera();

            // Randomize recovery delay more
            int recoveryDelay = Random.nextBoolean() ?
                    gaussianDelay(1500, 300, 800, 2500) :
                    gaussianDelay(800, 200, 400, 1500);

            Execution.delay(recoveryDelay);

            // Sometimes try a different approach
            if (Random.nextBoolean()) {
                fallBack(targetArea, pathfinder);
            } else {
                // Just wait and try again
                Execution.delay(gaussianDelay(1000, 200, 500, 1800));
            }
            stuckCounter = 0;
        }
    }

    public void fallBack(Area targetArea, Pathfinder pathfinder) {
        if (targetArea == null || pathfinder == null) return;

        System.out.println("🔄 Attempting fallback navigation");

        // Try simple Bresenham path first
        Path path = BresenhamPath.buildTo(targetArea.getCenter().randomize(10, 10));
        if (path != null && path.isValid() && !targetArea.contains(Players.getLocal())) {
            if (path.step()) {
                System.out.println("🚶 Stepping to fallback (Bresenham path)");
                Execution.delay(gaussianDelay(1000, 200, 600, 1500));
                return;
            }
        }

        // Try partial path if direct path fails
        Coordinate start = Players.getLocal().getPosition();
        if (start == null) {
            System.out.println("❌ Invalid start position");
            return;
        }

        Coordinate target = targetArea.getCenter();
        if (target == null) {
            System.out.println("❌ Invalid target position");
            return;
        }

        // Calculate a point halfway to target
        int newX = start.getX() + (target.getX() - start.getX()) / 2;
        int newY = start.getY() + (target.getY() - start.getY()) / 2;
        Coordinate halfwayPoint = new Coordinate(newX, newY, start.getPlane());
        halfwayPoint = gaussianRandomized(halfwayPoint, 0, 1, 3);

        path = pathfinder.pathBuilder()
                .destination(halfwayPoint)
                .enableHomeTeleport(false)
                .avoidWilderness(true)
                .preferAccuracy()
                .findPath();

        if (path != null && path.isValid() && !targetArea.contains(Players.getLocal())) {
            if (path.step()) {
                System.out.println("🚶 Stepping to fallback (halfway point): " + halfwayPoint);
                Execution.delay(gaussianDelay(1000, 200, 600, 1500));
            } else {
                System.out.println("⚠️ Failed to step to fallback destination");
            }
        } else {
            System.out.println("❌ Fallback path is invalid");
        }
    }

    // Gaussian-based delay helper
    private int gaussianDelay(int mean, int deviation, int min, int max) {
        int delay = (int) Random.nextGaussian(min, max, mean, deviation);
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