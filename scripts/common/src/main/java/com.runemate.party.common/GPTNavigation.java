package com.runemate.party.common;

import com.runemate.game.api.client.ClientUI;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;
import com.runemate.pathfinder.Pathfinder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class GPTNavigation {

    private static final int MAX_ATTEMPTS = 5;
    private static final long ANTI_BAN_COOLDOWN = 30_000;
    private static final int MIN_DISTANCE_FOR_ANTIBAN = 10;
    private static final int PATH_REFINEMENT_INTERVAL_TICKS = 5;

    private final AntiBan antiBan = new AntiBan();
    private final Set<Coordinate> reachedCache = new HashSet<>();

    private long lastAntiBanTime = 0;
    private Coordinate lastPosition;
    private int stuckCounter = 0;

    public void walkToArea(Area targetArea, Pathfinder pathfinder) {
        if (targetArea == null || pathfinder == null) return;
        if (targetArea.contains(Players.getLocal())) return;

        System.out.println("📍 Walking to: " + targetArea.getCenter());

        int attempts = 0;
        boolean reached = false;
        Path path = null;

        while (attempts < MAX_ATTEMPTS && !reached) {
            Coordinate target = getOptimalCoordinate(targetArea, pathfinder);
            if (target == null) break;

            path = buildPath(pathfinder, target);
            if (path == null || !path.isValid()) {
                attempts++;
                Execution.delay(gaussianDelay(500, 200, 300, 1000));
                continue;
            }

            int refinementTickCounter = 0;

            while (!targetArea.contains(Players.getLocal()) && path != null && path.isValid()) {
                Coordinate nextStep = Objects.requireNonNull(path.getNext()).getPosition();
                if (nextStep != null) {
                    antiBan.alignCameraTo(nextStep);
                    Execution.delayUntil(() -> !Players.getLocal().isMoving(), 50, 150);
                    if (!path.step()) break;
                    Execution.delay(gaussianDelay(600, 200, 300, 1200));
                    handleAntiBan(Players.getLocal().getPosition());
                }

                if (++refinementTickCounter >= PATH_REFINEMENT_INTERVAL_TICKS) {
                    path = buildPath(pathfinder, target);  // Refresh path
                    refinementTickCounter = 0;
                }
            }

            if (targetArea.contains(Players.getLocal())) {
                reachedCache.add(target);
                reached = true;
            } else {
                attempts++;
                handleStuckSituation(targetArea, pathfinder);
            }
        }

        if (!reached) {
            System.out.println("❌ Navigation failed. Attempting fallback.");
            fallBack(targetArea, pathfinder);
        }
    }

    private Coordinate getOptimalCoordinate(Area area, Pathfinder pathfinder) {
        Coordinate center = area.getCenter();
        if (center == null) return null;

        for (Coordinate cached : reachedCache) {
            if (area.contains(cached) && isReachable(cached, pathfinder)) return cached;
        }

        if (isReachable(center, pathfinder)) return center;

        for (int i = 0; i < 10; i++) {
            Coordinate random = area.getRandomCoordinate();
            if (random != null && isReachable(random, pathfinder)) return random;
        }

        return null;
    }

    private boolean isReachable(Coordinate target, Pathfinder pathfinder) {
        Path path = buildPath(pathfinder, target);
        return path != null && path.isValid();
    }

    private Path buildPath(Pathfinder pathfinder, Coordinate target) {
        return pathfinder.pathBuilder()
                .destination(target)
                .preferAccuracy()
                .enableHomeTeleport(true)
                .enableTeleports(false)
                .avoidWilderness(true)
                .findPath();
    }

    private void handleAntiBan(Coordinate currentPos) {
        long now = System.currentTimeMillis();
        double distance = currentPos != null && lastPosition != null
                ? currentPos.distanceTo(lastPosition) : 0;

        if (distance > MIN_DISTANCE_FOR_ANTIBAN &&
                now - lastAntiBanTime > ANTI_BAN_COOLDOWN &&
                Random.nextInt(0, 100) < 20) {

            antiBan.rotateCamera();
            lastAntiBanTime = now;
        }

        lastPosition = currentPos;
    }

    private void handleStuckSituation(Area targetArea, Pathfinder pathfinder) {
        stuckCounter++;
        System.out.println("⚠️ Stuck (" + stuckCounter + "/3)");

        if (stuckCounter >= 3) {
            System.out.println("🚧 Performing stuck recovery...");
            antiBan.rotateCamera();
            Execution.delay(gaussianDelay(800, 200, 500, 1800));
            stuckCounter = 0;
        }
    }

    public void fallBack(Area targetArea, Pathfinder pathfinder) {
        System.out.println("🔄 Fallback initiated.");

        Coordinate start = Players.getLocal().getPosition();
        Coordinate target = targetArea.getCenter();
        if (start == null || target == null) return;

        int halfwayX = (start.getX() + target.getX()) / 2;
        int halfwayY = (start.getY() + target.getY()) / 2;
        Coordinate halfway = new Coordinate(halfwayX, halfwayY, start.getPlane());

        Path path = buildPath(pathfinder, halfway);
        if (path != null && path.isValid() && path.step()) {
            Execution.delay(gaussianDelay(1000, 300, 600, 1500));
            Coordinate pos = Players.getLocal().getPosition();
            if (pos != null) reachedCache.add(pos);
        }
    }

    private int gaussianDelay(int mean, int deviation, int min, int max) {
        int delay = (int) Random.nextGaussian(min, max, mean, deviation);
        return Math.max(min, Math.min(max, delay));
    }
}
