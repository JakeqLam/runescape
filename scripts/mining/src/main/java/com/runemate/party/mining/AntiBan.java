package com.runemate.party.mining;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

public class AntiBan {

    private long lastZoomTime = 0;

    public void performAntiBan() {
        System.out.println("🛡️ Performing anti-ban action...");

// Weighted action pool: more entries = higher chance
        int[] weightedActions = {
                0, 0, 0, 0, // Camera rotation (case 0, more weight)
                1,    // Hover over player (case 1)
                2,    // Hover over NPC (case 2)
                3, 3, 3, 3,    // Move mouse (case 3)
                4,    // Camera zoom (case 4)
                5
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
                // Get the local player
                Player localPlayer = Players.getLocal();

                if (localPlayer == null) {
                    System.out.println("Local player not found.");
                    return;
                }

                // Get the player's current position
                Coordinate playerPosition = localPlayer.getPosition();

                // Use newQuery() to find all interactable GameObjects and filter them by distance (within 30 tiles)
                List<GameObject> nearbyGameObjects = GameObjects.newQuery()
                        .filter(gameObject -> gameObject != null &&
                                gameObject.isValid() &&  // Ensure object is interactable
                                Objects.requireNonNull(gameObject.getPosition()).distanceTo(playerPosition) <= 30) // Check if it's within 30 tiles
                        .results()
                        .asList();

                if (nearbyGameObjects.isEmpty()) {
                    System.out.println("No interactable GameObjects found within 30 tiles, skipping hover.");
                    return;
                }

                // Select a random GameObject from the nearby list
                GameObject randomGameObject = nearbyGameObjects.get(Random.nextInt(nearbyGameObjects.size()));

                // Log the selected GameObject
                System.out.println("Selected interactable GameObject: " + randomGameObject);

                // Ensure the GameObject is interactable before moving the mouse
                if (!randomGameObject.isValid()) {
                    System.out.println("The selected GameObject is not interactable.");
                    return;
                }

                // Get the interactable position of the GameObject
                Coordinate targetPosition = randomGameObject.getPosition();

                // Log the initial mouse movement
                System.out.println("Starting mouse movement towards GameObject at: (" + targetPosition.getX() + ", " + targetPosition.getY() + ")");

                // Human-like movement: Random delay and gradual approach
                int steps = Random.nextInt(10, 20); // Number of small movement steps
                for (int i = 0; i < steps; i++) {
                    // Slightly randomize each step's target within a small range to simulate non-linear movement
                    int stepX = targetPosition.getX() + Random.nextInt(-5, 5);
                    int stepY = targetPosition.getY() + Random.nextInt(-5, 5);

                    // Log each step of the movement
                    System.out.println("Step " + (i + 1) + ": Moving to (" + stepX + ", " + stepY + ")");

                    // Move the mouse to the new position within the interactable area of the object
                    Mouse.move(randomGameObject.getPosition());  // Move to the GameObject's interactable position

                    // Add a small delay between each movement to simulate human hesitation
                    Execution.delay(Random.nextInt(50, 100));  // 50-100ms between steps
                }

                // Once the mouse is in the area, hover over the game object
                System.out.println("Final move to target: (" + targetPosition.getX() + ", " + targetPosition.getY() + ")");

                // Move mouse to interactable position of the GameObject
                Mouse.move(randomGameObject.getPosition());  // Finally, position it directly over the object
                randomGameObject.hover();  // Hover over the game object

                // Log hover action
                System.out.println("Hovered over GameObject: " + randomGameObject);

                // Hover time, a random delay to simulate waiting
                Execution.delay(Random.nextInt(500, 1000));  // Simulate hover time (500ms - 1s)
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

