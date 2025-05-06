package com.runemate.party.common;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Item;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.entities.details.Interactable;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.local.Screen;
import com.runemate.game.api.hybrid.local.Skill;
import com.runemate.game.api.hybrid.local.hud.interfaces.InterfaceComponent;
import com.runemate.game.api.hybrid.local.hud.interfaces.Interfaces;
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.queries.InterfaceComponentQueryBuilder;
import com.runemate.game.api.hybrid.queries.results.SpriteItemQueryResults;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.StopWatch;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;
import javafx.scene.input.MouseButton;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AntiBan {

    private static final int STATS_GROUP_ID = 320;  // Skills tab group
    private static final int TABS_GROUP_ID = 164;   // Bottom tab group

    private long lastZoomTime = 0;
    private long lastCameraMoveTime = 0;

    public void rotateCamera() {
        System.out.println("🛡️ Performing anti-ban action...");

                if (Random.nextInt(0,10) < 3) { // 30% chance for middle-mouse drag
                    int curYaw   = Camera.getYaw();
                    double curP  = Camera.getPitch();
                    System.out.printf("[Cam] current yaw=%d, pitch=%.2f%n", curYaw, curP);

                    // 2) choose Δ but enforce |Δyaw| ≥ 30°, |Δpitch| ≥ 0.20
                    int rawDy     = ThreadLocalRandom.current().nextInt(-45, 46);
                    int dy        = (Math.abs(rawDy) < 30 ? (rawDy < 0 ? -30 : 30) : rawDy);
                    double rawDp  = ThreadLocalRandom.current().nextDouble(-0.25, 0.26);
                    double dp     = (Math.abs(rawDp) < 0.20 ? (rawDp < 0 ? -0.20 : 0.20) : rawDp);

                    int tgtYaw   = (curYaw + dy + 360) % 360;
                    double tgtP  = Math.max(0, Math.min(1, curP + dp));
                    System.out.printf("[Cam] target yaw=%d (Δ%+d), pitch=%.2f (Δ%+.2f)%n",
                            tgtYaw, dy, tgtP, dp);

                    // 3) async turn (tolerance only affects “when to stop”)
                    double tol = ThreadLocalRandom.current().nextDouble(0.05, 0.15);
                    System.out.printf("[Cam] turning tol=%.2f%n", tol);
                    Camera.concurrentlyTurnTo(tgtYaw, tgtP, tol);  // built-in micro-jitter :contentReference[oaicite:0]{index=0}

                    // 4) wait 500–800ms or until done
                    Execution.delayUntil(
                            () -> !Camera.isTurning(),
                            () -> false,
                            500, 800
                    );                                              // yields to RM loop :contentReference[oaicite:1]{index=1}

                    // 5) log result
                    int finYaw   = Camera.getYaw();
                    double finP  = Camera.getPitch();
                    int diffYaw  = Math.min(Math.abs(finYaw - tgtYaw), 360 - Math.abs(finYaw - tgtYaw));
                    double diffP = Math.abs(finP - tgtP);
                    boolean ok   = diffYaw <= tol * 360 && diffP <= tol;
                    System.out.printf("[Cam] final yaw=%d, pitch=%.2f → Δyaw=%d, Δpitch=%.2f → %s%n",
                            finYaw, finP, diffYaw, diffP, ok ? "OK" : "MISS");
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
                    Execution.delay(Random.nextInt(500, 1800));
                    for (int key : combo) Keyboard.releaseKey(key);
                    System.out.println("🎥 Arrow-key camera move");
                }
                lastCameraMoveTime = System.currentTimeMillis();


        // Post-action delay (variable)
        Execution.delay(Random.nextInt(200, 1500));
    }

    public void performAntiBan() {
        System.out.println("🛡️ Performing anti-ban action...");

        // Weighted action pool (more entries = higher chance)
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
                if (Random.nextInt(0,10) < 3) { // 30% chance for middle-mouse drag
                    int curYaw   = Camera.getYaw();
                    double curP  = Camera.getPitch();
                    System.out.printf("[Cam] current yaw=%d, pitch=%.2f%n", curYaw, curP);

                    // 2) choose Δ but enforce |Δyaw| ≥ 30°, |Δpitch| ≥ 0.20
                    int rawDy     = ThreadLocalRandom.current().nextInt(-45, 46);
                    int dy        = (Math.abs(rawDy) < 30 ? (rawDy < 0 ? -30 : 30) : rawDy);
                    double rawDp  = ThreadLocalRandom.current().nextDouble(-0.25, 0.26);
                    double dp     = (Math.abs(rawDp) < 0.20 ? (rawDp < 0 ? -0.20 : 0.20) : rawDp);

                    int tgtYaw   = (curYaw + dy + 360) % 360;
                    double tgtP  = Math.max(0, Math.min(1, curP + dp));
                    System.out.printf("[Cam] target yaw=%d (Δ%+d), pitch=%.2f (Δ%+.2f)%n",
                            tgtYaw, dy, tgtP, dp);

                    // 3) async turn (tolerance only affects “when to stop”)
                    double tol = ThreadLocalRandom.current().nextDouble(0.05, 0.15);
                    System.out.printf("[Cam] turning tol=%.2f%n", tol);
                    Camera.concurrentlyTurnTo(tgtYaw, tgtP, tol);  // built-in micro-jitter :contentReference[oaicite:0]{index=0}

                    // 4) wait 500–800ms or until done
                    Execution.delayUntil(
                            () -> !Camera.isTurning(),
                            () -> false,
                            500, 800
                    );                                              // yields to RM loop :contentReference[oaicite:1]{index=1}

                    // 5) log result
                    int finYaw   = Camera.getYaw();
                    double finP  = Camera.getPitch();
                    int diffYaw  = Math.min(Math.abs(finYaw - tgtYaw), 360 - Math.abs(finYaw - tgtYaw));
                    double diffP = Math.abs(finP - tgtP);
                    boolean ok   = diffYaw <= tol * 360 && diffP <= tol;
                    System.out.printf("[Cam] final yaw=%d, pitch=%.2f → Δyaw=%d, Δpitch=%.2f → %s%n",
                            finYaw, finP, diffYaw, diffP, ok ? "OK" : "MISS");
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
                    Execution.delay(Random.nextInt(500, 1800));
                    for (int key : combo) Keyboard.releaseKey(key);
                    System.out.println("🎥 Arrow-key camera move");
                }
                lastCameraMoveTime = System.currentTimeMillis();
                break;

            case 1: // Hover Player
                Player local = Players.getLocal();
                Player player = Players.newQuery().filter(p -> p != null && !p.equals(local)).results().random();
                if (player != null) {
                    Mouse.move(player);
                    System.out.println("👤 Hovered player: " + player.getName());
                    Execution.delay(Random.nextInt(300, 800));

                    if (Random.nextInt(0, 5) == 0) { // ~20% chance to right-click
                        Mouse.click(Mouse.Button.RIGHT); // Right-click
                        System.out.println("🖱️ Right-clicked player.");
                        Execution.delay(Random.nextInt(200, 400));
                    }
                }
                break;

            case 2: // Hover NPC
                Npc npc = Npcs.newQuery().filter(Npc::isValid).results().random();
                if (npc != null) {
                    Mouse.move(npc);
                    System.out.println("🧟 Hovered NPC: " + npc.getName());
                    Execution.delay(Random.nextInt(300, 800));

                    if (Random.nextInt(0, 5) == 0) { // ~20% chance to right-click
                        Mouse.click(Mouse.Button.RIGHT); // Right-click
                        System.out.println("🖱️ Right-clicked NPC.");
                        Execution.delay(Random.nextInt(200, 400));
                    }
                }
                break;

            case 3: // Gradual Mouse Movement (Human-Like)
                GameObject obj = GameObjects.newQuery()
                        .filter(o -> o != null && o.isValid())
                        .results()
                        .random();

                if (obj != null) {
                    for (int i = 0; i < Random.nextInt(5, 15); i++) {
                        Mouse.move(obj.getPosition());
                        Execution.delay(Random.nextInt(50, 150));
                    }
                    System.out.println("🖱️ Moved mouse to object");
                }
                break;

            case 4: // Camera Zoom (with cooldown & double scroll chance)
                if (System.currentTimeMillis() - lastZoomTime > TimeUnit.MINUTES.toMillis(2)) {
                    boolean zoomIn = Random.nextBoolean();
                    int scrolls = Random.nextInt(0,10) < 3 ? 2 : 1; // 30% chance for double scroll
                    for (int i = 0; i < scrolls; i++) {
                        Mouse.scroll(zoomIn);
                        Execution.delay(Random.nextInt(100, 300));
                    }
                    System.out.println("🔍 Zoomed " + (zoomIn ? "in" : "out") + (scrolls > 1 ? " (x2)" : ""));
                    lastZoomTime = System.currentTimeMillis();
                }
                break;

            case 5: // Enhanced Tab Switching & Interface Interaction
                boolean goToInventory = Random.nextBoolean();

                if (goToInventory) {
                    Keyboard.pressKey(KeyEvent.VK_F1); // Inventory
                    System.out.println("📂 Switched to Inventory tab.");
                } else {
                    Keyboard.pressKey(KeyEvent.VK_F2); // Skills
                    System.out.println("📊 Switched to Skills tab.");
                }

                Execution.delay(Random.nextInt(400, 1200)); // Random human-like delay
                break;
            case 6: // Idle (AFK)
                int delay = Random.nextInt(0, 30) < 2 ? Random.nextInt(30000, 60000) : Random.nextInt(6000, 15000);
                System.out.println("😴 Idling for " + (delay / 1000) + "s");
                Execution.delay(delay);
                break;
        }

        // Post-action delay (variable)
        Execution.delay(Random.nextInt(200, 1500));
    }

    private static Point getClickablePoint(Rectangle bounds) {
        if (bounds == null) return null;
        int x = Random.nextInt(bounds.x + 2, bounds.x + bounds.width - 2);
        int y = Random.nextInt(bounds.y + 2, bounds.y + bounds.height - 2);
        return new Point(x, y);
    }
}
