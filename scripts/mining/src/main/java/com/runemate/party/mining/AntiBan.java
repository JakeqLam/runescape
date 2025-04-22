package com.runemate.party.mining;

import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.hud.InteractablePoint;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;

import java.awt.*;
import java.awt.event.KeyEvent;

public class AntiBan {

    private long lastZoomTime = 0;

    public void performAntiBan() {
        System.out.println("🛡️ Performing anti-ban action...");

// Weighted action pool: more entries = higher chance
        int[] weightedActions = {
                0, 0, 0, 0, // Camera rotation (case 0, more weight)
                1,    // Hover over player (case 1)
                2,    // Hover over NPC (case 2)
                3,    // Move mouse (case 3)
                4,    // Camera zoom (case 4)
                5, 5, // Idle action (case 5, more weight)
                6     // Switch tab (case 6)
        };

        int action = weightedActions[Random.nextInt(weightedActions.length)];

        switch (action) {
            case 0 -> {
                // All possible key combinations: solo + diagonal
                int[][] keyCombos = {
                        {KeyEvent.VK_LEFT},                 // Rotate left
                        {KeyEvent.VK_RIGHT},                // Rotate right
                        {KeyEvent.VK_UP},                   // Pitch up
                        {KeyEvent.VK_DOWN},                 // Pitch down
                        {KeyEvent.VK_LEFT, KeyEvent.VK_UP},    // Diagonal up-left
                        {KeyEvent.VK_RIGHT, KeyEvent.VK_UP},   // Diagonal up-right
                        {KeyEvent.VK_LEFT, KeyEvent.VK_DOWN},  // Diagonal down-left
                        {KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN}  // Diagonal down-right
                };

                int[] combo = keyCombos[Random.nextInt(keyCombos.length)];

                // Press all keys in the combo
                for (int key : combo) {
                    Keyboard.pressKey(key);
                }

                // Hold the keys for a random duration
                Execution.delay(Random.nextInt(500, 1800));

                // Release the keys
                for (int key : combo) {
                    Keyboard.releaseKey(key);
                }

                // Build a readable direction string
                StringBuilder direction = new StringBuilder();
                for (int key : combo) {
                    direction.append(
                            switch (key) {
                                case KeyEvent.VK_LEFT -> "LEFT ";
                                case KeyEvent.VK_RIGHT -> "RIGHT ";
                                case KeyEvent.VK_UP -> "UP ";
                                case KeyEvent.VK_DOWN -> "DOWN ";
                                default -> "";
                            }
                    );
                }

                System.out.println("🎥 Camera moved: " + direction.toString().trim());

                // Optional: misclick delay
                if (Random.nextInt(100) < 25) {
                    int delay = Random.nextInt(120, 300);
                    System.out.println("⌛ Simulating misclick delay: " + delay + "ms");
                    Execution.delay(delay);
                }
            }
            case 1 -> {
                // Hover over a random nearby player
                Player local = Players.getLocal();
                Player player = Players.newQuery()
                        .filter(p -> p != null && !p.equals(local) && p.isVisible())
                        .results()
                        .random();

                if (player != null) {
                    player.hover();
                    System.out.println("👤 Hovered over player: " + player.getName());
                    Execution.delay(Random.nextInt(300, 600));
                }
            }
            case 2 -> {
                // Hover over a random visible NPC
                Npc npc = Npcs.newQuery().filter(Npc::isVisible).results().random();
                if (npc != null) {
                    npc.hover();
                    System.out.println("🧟 Hovered over NPC: " + npc.getName());
                    Execution.delay(Random.nextInt(300, 600));
                }
            }
            case 3 -> {
                // Move mouse to a random screen position using an InteractablePoint
                int x = Random.nextInt(200, 500);
                int y = Random.nextInt(100, 400);
                InteractablePoint fakePoint = new InteractablePoint(new Point(x, y));
                if (Mouse.move(fakePoint)) {
                    System.out.println("🖱️ Moved mouse to random interactable point: (" + x + ", " + y + ")");
                } else {
                    System.out.println("⚠️ Failed to move mouse to random point.");
                }
                Execution.delay(Random.nextInt(300, 500));
            }
            case 4 -> {
                // Simulate camera zoom (if enough time has passed)
                if (System.currentTimeMillis() - lastZoomTime > 2 * 60 * 1000) {
                    boolean zoomIn = Random.nextBoolean();
                    Mouse.scroll(zoomIn); // true = scroll up (zoom in), false = scroll down (zoom out)
                    System.out.println("🔍 Camera zoom " + (zoomIn ? "in" : "out"));
                    lastZoomTime = System.currentTimeMillis();
                    Execution.delay(Random.nextInt(300, 500));
                } else {
                    System.out.println("⏳ Skipped zoom: cooldown active.");
                }
            }
            case 5 -> {
                // Idle for a realistic human-like duration
                int idleDuration = Random.nextInt(1200, 6000); // 1.2 to 6 seconds
                System.out.println("💤 Idling for " + idleDuration + "ms...");
                Execution.delay(idleDuration);

                // 50% chance to do a subtle mouse twitch while idling
                if (Random.nextBoolean()) {
                    Point current = Mouse.getPosition();
                    Point twitch = new Point(
                            current.x + Random.nextInt(-3, 3),
                            current.y + Random.nextInt(-3, 3)
                    );

                    InteractablePoint interactableTwitch = new InteractablePoint(twitch);
                    if (Mouse.move(interactableTwitch)) {
                        System.out.println("🖱️ Idle mouse twitch to: (" + twitch.x + ", " + twitch.y + ")");
                    } else {
                        System.out.println("❌ Failed to move mouse during idle twitch.");
                    }
                }
            }
            case 6 -> {
                // OSRS default F-key bindings (modify based on your settings)
                final int[] TAB_KEYS = {
                        KeyEvent.VK_F2, // Stats
                        KeyEvent.VK_F4, // Inventory
                        KeyEvent.VK_F8, // Friends
                };

                int randomIndex = Random.nextInt(TAB_KEYS.length);
                int tabKey = TAB_KEYS[randomIndex];

                Keyboard.pressKey(tabKey);
                Execution.delay(Random.nextInt(100, 200));
                Keyboard.releaseKey(tabKey);

                System.out.println("🎹 Switched tab using F-key: F" + (randomIndex + 1));

                // Optional: Add delay to simulate reading/interaction
                Execution.delay(Random.nextInt(400, 800));

                // 25% chance to switch back to inventory (F4)
                if (Random.nextInt(4) == 0) {
                    Keyboard.pressKey(KeyEvent.VK_F4);
                    Execution.delay(Random.nextInt(100, 200));
                    Keyboard.releaseKey(KeyEvent.VK_F4);
                    System.out.println("↩️ Returned to Inventory (F4)");
                }
            }
        }

        // Post-action delay
        Execution.delay(Random.nextInt(200, 500));
    }
}
