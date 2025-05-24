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
        return (long)getGaussian(50_000, 100_000, 75_000, 12_500); // Gaussian between 10s and 100s
    }

    // Gaussian random number within bounds (min, max) with mean and std deviation
    private double getGaussian(double min, double max, double mean, double stdDev) {
        double value;
        do {
            value = Random.nextGaussian(min, max, mean, stdDev);
        } while (value < min || value > max);
        return value;
    }


    public void maybePerformAntiBan() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAntiBanTime >= nextAntiBanCooldown && Random.nextInt(100) < 3) {
            System.out.println("[SimpleFisher] Performing anti-ban");
            performAntiBan();
            lastAntiBanTime = currentTime;
            nextAntiBanCooldown = getRandomCooldown();
        }
    }

    public void alignCameraTo(Coordinate target) {
        if (target == null || Players.getLocal() == null || Players.getLocal().getPosition() == null) {
            return;
        }

        Coordinate playerPos = Players.getLocal().getPosition();

        double dx = target.getX() - playerPos.getX();
        double dy = target.getY() - playerPos.getY();

        double angleRad = Math.atan2(dy, dx);
        int targetYaw = (int) Math.toDegrees(angleRad);
        if (targetYaw < 0) {
            targetYaw += 360;
        }

        int currentYaw = Camera.getYaw();

        int diff = targetYaw - currentYaw;
        if (diff > 180) {
            diff -= 360;
        } else if (diff < -180) {
            diff += 360;
        }

        int step = (int) Math.signum(diff) * Math.min(10, Math.abs(diff));

        int newYaw = (currentYaw + step) % 360;
        if (newYaw < 0) newYaw += 360;

        int randomPitch = 60 + Random.nextInt(-10, 10);

        Camera.turnTo(newYaw, randomPitch);
    }


    public void rotateCamera() {
        System.out.println("🛡️ Performing anti-ban action...");

        if (Random.nextInt(0,10) < 3) { // 30% chance for middle-mouse drag
            int curYaw   = Camera.getYaw();
            double curP  = Camera.getPitch();
            System.out.printf("[Cam] current yaw=%d, pitch=%.2f%n", curYaw, curP);

            int rawDy     = ThreadLocalRandom.current().nextInt(-45, 46);
            int dy        = (Math.abs(rawDy) < 30 ? (rawDy < 0 ? -30 : 30) : rawDy);
            double rawDp  = ThreadLocalRandom.current().nextDouble(-0.25, 0.26);
            double dp     = (Math.abs(rawDp) < 0.20 ? (rawDp < 0 ? -0.20 : 0.20) : rawDp);

            int tgtYaw   = (curYaw + dy + 360) % 360;
            double tgtP  = Math.max(0, Math.min(1, curP + dp));
            System.out.printf("[Cam] target yaw=%d (Δ%+d), pitch=%.2f (Δ%+.2f)%n",
                    tgtYaw, dy, tgtP, dp);

            double tol = getGaussian(0.05, 0.15, 0.10, 0.03);
            System.out.printf("[Cam] turning tol=%.2f%n", tol);
            Camera.concurrentlyTurnTo(tgtYaw, tgtP, tol);

            Execution.delayUntil(
                    () -> !Camera.isTurning(),
                    () -> false,
                    (int)getGaussian(500, 800, 650, 100)
            );
        } else { // 70% chance for arrow keys
            int[][] keyCombos = {
                    {KeyEvent.VK_LEFT},
                    {KeyEvent.VK_RIGHT},
                    {KeyEvent.VK_UP},
                    {KeyEvent.VK_DOWN},
                    {KeyEvent.VK_LEFT, KeyEvent.VK_UP},
                    {KeyEvent.VK_RIGHT, KeyEvent.VK_UP},
                    {KeyEvent.VK_LEFT, KeyEvent.VK_DOWN},
                    {KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN}
            };

            int[] combo = keyCombos[Random.nextInt(0, keyCombos.length)];
            for (int key : combo) Keyboard.pressKey(key);
            Execution.delay((int)getGaussian(500, 1800, 1150, 400));
            for (int key : combo) Keyboard.releaseKey(key);
            System.out.println("🎥 Arrow-key camera move");
        }
        lastCameraMoveTime = System.currentTimeMillis();

        // Post-action delay (gaussian)
        Execution.delay((int)getGaussian(200, 1500, 850, 400));
    }

    private void maybeMoveMouseRandomlyOrToObject() {
        GameObject obj = GameObjects.newQuery()
                .filter(o -> o != null && o.isVisible())
                .results()
                .random();

        if (obj != null) {
            double result = getGaussian(0, 1, 0.2, 0.1); // mean = 0.2
            boolean moveToTarget = result < 0.5; // ~70% true, 30% false

            Point targetPoint = obj.getInteractionPoint();
            if (targetPoint == null) return;

            if (!moveToTarget) {
                // Add ±25px random offset
                int offsetX = Random.nextInt(-25, 26);
                int offsetY = Random.nextInt(-25, 26);
                targetPoint = new Point(targetPoint.x + offsetX, targetPoint.y + offsetY);
                System.out.println("🖱️ Moving mouse near object");
            } else {
                System.out.println("🖱️ Moving mouse toward object");
            }

            Point current = Mouse.getPosition();
            int steps = (int) getGaussian(5, 15, 10, 3);
            for (int i = 0; i < steps; i++) {
                double t = (i + 1) / (double) steps;
                int x = (int) (current.x + t * (targetPoint.x - current.x));
                int y = (int) (current.y + t * (targetPoint.y - current.y));

                x += Random.nextInt(-2, 3); // small jitter
                y += Random.nextInt(-2, 3);

                Mouse.move(new InteractablePoint(x, y));
                Execution.delay((int) getGaussian(50, 150, 100, 30));
            }

            if (moveToTarget) {
                Mouse.move(obj); // final snap to interactable
            }
        }
    }

    public void performAntiBan() {
        System.out.println("🛡️ Performing anti-ban action...");

        int[] weightedActions = {
                0, 0, 0, 0, 0, // Camera movement (most common)
                1, 1,             // Hover player
                2, 2,            // Hover NPC
                3, 3, 3, 3,       // Move mouse (gradual)
                4,                // Camera zoom
                5,        // tab switching
                6, 6, 6        // Idle (AFK)
        };

        int action = weightedActions[Random.nextInt(0, weightedActions.length)];

        switch (action) {
            case 0: // Camera Movement (Arrow Keys or Middle-Mouse)
                rotateCamera();
                break;

            case 1: // Hover Player
                Player local = Players.getLocal();
                Player player = Players.newQuery().filter(p -> p != null && !p.equals(local)).results().random();
                if (player != null) {
                    Mouse.move(player);
                    System.out.println("👤 Hovered player: " + player.getName());
                    Execution.delay((int)getGaussian(300, 800, 550, 150));

                    if (Random.nextInt(0, 5) == 0) { // ~20% chance to right-click
                        Mouse.click(Mouse.Button.RIGHT);
                        System.out.println("🖱️ Right-clicked player.");
                        Execution.delay((int)getGaussian(200, 400, 300, 70));
                    }
                }
                break;

            case 2: // Hover NPC
                Npc npc = Npcs.newQuery().filter(Npc::isValid).results().random();
                if (npc != null) {
                    Mouse.move(npc);
                    System.out.println("🧟 Hovered NPC: " + npc.getName());
                    Execution.delay((int)getGaussian(300, 800, 550, 150));

                    if (Random.nextInt(0, 5) == 0) { // ~20% chance to right-click
                        Mouse.click(Mouse.Button.RIGHT);
                        System.out.println("🖱️ Right-clicked NPC.");
                        Execution.delay((int)getGaussian(200, 400, 300, 70));
                    }
                }
                break;

            case 3: // Gradual mouse movement
                maybeMoveMouseRandomlyOrToObject();
                break;

            case 4: // Camera Zoom (with cooldown & double scroll chance)
                if (System.currentTimeMillis() - lastZoomTime > TimeUnit.MINUTES.toMillis(2)) {
                    boolean zoomIn = Random.nextBoolean();
                    int scrolls = Random.nextInt(0,10) < 3 ? 2 : 1;
                    for (int i = 0; i < scrolls; i++) {
                        Mouse.scroll(zoomIn);
                        Execution.delay((int)getGaussian(100, 300, 200, 70));
                    }
                    System.out.println("🔍 Zoomed " + (zoomIn ? "in" : "out") + (scrolls > 1 ? " (x2)" : ""));
                    lastZoomTime = System.currentTimeMillis();
                }
                break;

            case 5: // Enhanced Tab Switching & Interface Interaction
                if (!InterfaceWindows.getInventory().isOpen())
                    Keyboard.pressKey(KeyEvent.VK_F2);
                System.out.println("📊 Switched to Skills tab.");
                Execution.delay((int)getGaussian(100, 300, 200, 70));
                Keyboard.releaseKey(KeyEvent.VK_F2);
                Execution.delay((int)getGaussian(2500, 5000, 3750, 1000));

                Keyboard.pressKey(KeyEvent.VK_F3);
                Execution.delay((int)getGaussian(100, 300, 200, 70));
                Keyboard.releaseKey(KeyEvent.VK_F3);
                System.out.println("📂 Switched to Inventory tab.");


                break;

            case 6: // Idle (AFK)
                int delay = Random.nextInt(0, 30) < 2 ?
                        (int)getGaussian(30000, 60000, 45000, 10000) :
                        (int)getGaussian(6000, 15000, 10500, 3000);
                System.out.println("😴 Idling for " + (delay / 1000) + "s");
                Execution.delay(delay);
                break;
        }

        // Post-action delay (gaussian)
        Execution.delay((int)getGaussian(200, 1500, 850, 400));
    }

    public void performBreakLogic(int breakmin, int breakmax) {
        if (System.currentTimeMillis() >= nextBreakTime) {
            int length = (int)getGaussian(breakmin, breakmax, (breakmin+breakmax)/2, (breakmax-breakmin)/4);
            System.out.println("[SimpleFisher] Taking break for " + length + "s");
            if (RuneScape.isLoggedIn()) { RuneScape.logout(true); }
            Execution.delay(length * 1000);
            Execution.delayUntil(RuneScape::isLoggedIn, 5000, 30000);
            scheduleNextBreak(breakmin,breakmax);
        }
    }

    private void scheduleNextBreak(int breakmin, int breakmax) {
        int interval = (int)getGaussian(breakmin, breakmax, (breakmin+breakmax)/2, (breakmax-breakmin)/4);
        nextBreakTime = System.currentTimeMillis() + interval * 1000L;
        System.out.println("[SimpleFisher] Next break in " + interval + "s");
    }
}