package com.runemate.party.common;

import com.runemate.game.api.hybrid.RuneScape;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.entities.details.Interactable;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.hud.InteractablePoint;
import com.runemate.game.api.hybrid.local.hud.interfaces.InterfaceWindows;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class AntiBan {

    private long lastAntiBanTime = 0;
    private long nextAntiBanCooldown = getRandomCooldown();
    private long lastZoomTime = 0;
    private long lastCameraMoveTime = 0;
    private long nextBreakTime = System.currentTimeMillis() + (long)(getGaussian(9000, 11000, 10000, 800) * 1000L);

    private long getRandomCooldown() {
        long val = (long)getGaussian(50_000, 100_000, 75_000, 12_500);
        System.out.println("[AntiBan] Next cooldown in: " + val + " ms");
        return val;
    }

    private double getGaussian(double min, double max, double mean, double stdDev) {
        for (int attempts = 0; attempts < 10; attempts++) {
            double value = Random.nextGaussian(min, max, mean, stdDev);
            if (value >= min && value <= max) {

                return value;
            }
        }

        return mean;
    }

    public void maybePerformAntiBan() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAntiBanTime >= nextAntiBanCooldown && Random.nextInt(100) < 3) {
            System.out.println("[AntiBan] Triggering anti-ban behavior...");
            performAntiBan();
            lastAntiBanTime = currentTime;
            nextAntiBanCooldown = getRandomCooldown();
        }
    }

    public void alignCameraTo(Coordinate target) {
        System.out.println("[AntiBan] Aligning camera to: " + target);
        if (target == null || Players.getLocal() == null) return;

        Coordinate playerPos = Players.getLocal().getPosition();
        if (playerPos == null) return;

        double dx = target.getX() - playerPos.getX();
        double dy = target.getY() - playerPos.getY();
        int targetYaw = (int) Math.toDegrees(Math.atan2(dy, dx));
        if (targetYaw < 0) targetYaw += 360;

        int diff = targetYaw - Camera.getYaw();
        if (diff > 180) diff -= 360;
        else if (diff < -180) diff += 360;

        int step = (int) Math.signum(diff) * Math.min(10, Math.abs(diff));
        int newYaw = (Camera.getYaw() + step + 360) % 360;

        Camera.turnTo(newYaw, 60 + Random.nextInt(-10, 10));
    }

    public void rotateCamera() {
        System.out.println("[AntiBan] 🛡️ Rotating camera...");
        try {
            if (Random.nextInt(0,10) < 3) {
                int curYaw = Camera.getYaw();
                double curP = Camera.getPitch();
                int rawDy = ThreadLocalRandom.current().nextInt(-45, 46);
                int dy = (Math.abs(rawDy) < 30 ? (rawDy < 0 ? -30 : 30) : rawDy);
                double rawDp = ThreadLocalRandom.current().nextDouble(-0.25, 0.26);
                double dp = (Math.abs(rawDp) < 0.20 ? (rawDp < 0 ? -0.20 : 0.20) : rawDp);
                int tgtYaw = (curYaw + dy + 360) % 360;
                double tgtP = Math.max(0, Math.min(1, curP + dp));
                double tol = getGaussian(0.05, 0.15, 0.10, 0.03);

                System.out.println("[AntiBan] Concurrent turn to yaw=" + tgtYaw + ", pitch=" + tgtP + ", tol=" + tol);
                Camera.concurrentlyTurnTo(tgtYaw, tgtP, tol);
                Execution.delayUntil(() -> !Camera.isTurning(), 2000);
            } else {
                int[][] keyCombos = {
                        {KeyEvent.VK_LEFT}, {KeyEvent.VK_RIGHT}, {KeyEvent.VK_UP}, {KeyEvent.VK_DOWN},
                        {KeyEvent.VK_LEFT, KeyEvent.VK_UP}, {KeyEvent.VK_RIGHT, KeyEvent.VK_UP},
                        {KeyEvent.VK_LEFT, KeyEvent.VK_DOWN}, {KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN}
                };
                int[] combo = keyCombos[Random.nextInt(0, keyCombos.length)];
                System.out.println("[AntiBan] Keyboard camera rotation using: " + java.util.Arrays.toString(combo));
                for (int key : combo) Keyboard.pressKey(key);
                Execution.delay((int)getGaussian(500, 1800, 1150, 400));
                for (int key : combo) Keyboard.releaseKey(key);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Camera rotation failed: " + e.getMessage());
        }
        lastCameraMoveTime = System.currentTimeMillis();
        Execution.delay((int)getGaussian(200, 1500, 850, 400));
    }

    private void maybeMoveMouseRandomlyOrToObject() {
        System.out.println("[AntiBan] Moving mouse to object or random location...");
        try {
            Coordinate center = Players.getLocal().getPosition();
            if (center == null) return;

            Coordinate southWest = new Coordinate(center.getX() - 4, center.getY() - 4, center.getPlane());
            Coordinate northEast = new Coordinate(center.getX() + 4, center.getY() + 4, center.getPlane());
            Area area = new Area.Rectangular(southWest, northEast);

            GameObject obj = GameObjects.newQuery()
                    .within(area)
                    .filter(o -> o != null && o.isVisible() && o.getInteractionPoint() != null)
                    .results().random();

            if (obj == null) return;

            boolean moveToTarget = getGaussian(0, 1, 0.2, 0.1) < 0.5;
            Point targetPoint = obj.getInteractionPoint();
            if (targetPoint == null) return;

            System.out.println("[AntiBan] Targeting object: " + obj + ", point=" + targetPoint + ", moveToTarget=" + moveToTarget);
            if (!moveToTarget) {
                targetPoint.translate(Random.nextInt(-25, 26), Random.nextInt(-25, 26));
            }

            Point current = Mouse.getPosition();
            if (current.distance(targetPoint) < 2) return;

            int steps = (int) getGaussian(5, 15, 10, 3);
            steps = Math.max(steps, 3);
            System.out.println("[AntiBan] Moving in " + steps + " steps");

            for (int i = 0; i < steps; i++) {
                double t = (i + 1) / (double) steps;
                int x = (int)(current.x + t * (targetPoint.x - current.x)) + Random.nextInt(-2, 3);
                int y = (int)(current.y + t * (targetPoint.y - current.y)) + Random.nextInt(-2, 3);
                Mouse.move(new InteractablePoint(x, y));
                Execution.delay((int)getGaussian(50, 150, 100, 30));
            }

            if (moveToTarget && obj.getInteractionPoint() != null) {
                Mouse.move(obj);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Mouse movement failed: " + e.getMessage());
        }
    }

    public void performAntiBan() {
        System.out.println("🛡️ Performing anti-ban action...");
        try {
            int[] weightedActions = {
                    0,0,0,0,0, 1,1, 2,2, 3,3,3,3, 4, 5, 6,6,6
            };
            int action = weightedActions[Random.nextInt(0, weightedActions.length)];
            System.out.println("[AntiBan] Chosen anti-ban action: " + action);

            switch (action) {
                case 0 -> rotateCamera();
                case 1 -> {
                    Player p = Players.newQuery().filter(pl -> pl != null && !pl.equals(Players.getLocal())).results().random();
                    if (p != null) {
                        System.out.println("[AntiBan] Hovering player: " + p.getName());
                        Mouse.move(p);
                        Execution.delay((int)getGaussian(300, 800, 550, 150));
                        if (Random.nextInt(0, 5) == 0) Mouse.click(Mouse.Button.RIGHT);
                    }
                }
                case 2 -> {
                    Npc npc = Npcs.newQuery().filter(Npc::isValid).results().random();
                    if (npc != null) {
                        System.out.println("[AntiBan] Hovering NPC: " + npc.getName());
                        Mouse.move(npc);
                        Execution.delay((int)getGaussian(300, 800, 550, 150));
                        if (Random.nextInt(0, 5) == 0) Mouse.click(Mouse.Button.RIGHT);
                    }
                }
                case 3 -> maybeMoveMouseRandomlyOrToObject();
                case 4 -> {
                    if (System.currentTimeMillis() - lastZoomTime > TimeUnit.MINUTES.toMillis(2)) {
                        boolean zoomIn = Random.nextBoolean();
                        int scrolls = Random.nextInt(0, 10) < 3 ? 2 : 1;
                        System.out.println("[AntiBan] Scrolling camera (" + (zoomIn ? "in" : "out") + ") " + scrolls + " times");
                        for (int i = 0; i < scrolls; i++) {
                            Mouse.scroll(zoomIn);
                            Execution.delay((int)getGaussian(100, 300, 200, 70));
                        }
                        lastZoomTime = System.currentTimeMillis();
                    }
                }
                case 5 -> {
                    System.out.println("[AntiBan] Opening skills tab...");
                    Keyboard.pressKey(KeyEvent.VK_F2);
                    Execution.delay((int)getGaussian(100, 300, 200, 70));
                    Keyboard.releaseKey(KeyEvent.VK_F2);
                    Execution.delay((int)getGaussian(2500, 5000, 3750, 1000));
                    Keyboard.pressKey(KeyEvent.VK_ESCAPE);
                    Execution.delay((int)getGaussian(100, 300, 200, 70));
                    Keyboard.releaseKey(KeyEvent.VK_ESCAPE);
                }
                case 6 -> {
                    int delay = (Random.nextInt(0, 30) < 2)
                            ? (int)getGaussian(30000, 60000, 45000, 10000)
                            : (int)getGaussian(6000, 15000, 10500, 3000);
                    System.out.println("[AntiBan] Sleeping for " + delay + " ms");
                    Execution.delay(delay);
                }
            }

            Execution.delay((int)getGaussian(200, 1500, 850, 400));
        } catch (Exception e) {
            System.err.println("❌ Anti-ban action failed: " + e.getMessage());
        }
    }

    public void performBreakLogic(int breakmin, int breakmax) {
        if (System.currentTimeMillis() >= nextBreakTime) {
            int length = (int)getGaussian(breakmin, breakmax, (breakmin + breakmax) / 2, (breakmax - breakmin) / 4);
            System.out.println("[SimpleFisher] Taking break for " + length + "s");
            if (RuneScape.isLoggedIn()) RuneScape.logout(true);
            Execution.delay(length * 1000);
            Execution.delayUntil(RuneScape::isLoggedIn, 5000, 30000);
            scheduleNextBreak(breakmin, breakmax);
        }
    }

    private void scheduleNextBreak(int breakmin, int breakmax) {
        int interval = (int)getGaussian(breakmin, breakmax, (breakmin + breakmax) / 2, (breakmax - breakmin) / 4);
        nextBreakTime = System.currentTimeMillis() + interval * 1000L;
        System.out.println("[SimpleFisher] Next break scheduled in " + interval + "s");
    }
}
