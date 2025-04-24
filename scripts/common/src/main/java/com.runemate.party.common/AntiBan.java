package com.runemate.party.common;

import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.entities.Npc;
import com.runemate.game.api.hybrid.entities.Player;
import com.runemate.game.api.hybrid.input.Keyboard;
import com.runemate.game.api.hybrid.input.Mouse;
import com.runemate.game.api.hybrid.local.Camera;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Npcs;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.hybrid.util.calculations.Random;
import com.runemate.game.api.script.Execution;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AntiBan {

    private long lastZoomTime = 0;
    private long lastCameraMoveTime = 0;

    public void performAntiBan() {
        System.out.println("🛡️ Performing anti-ban action...");

        // Weighted action pool (more entries = higher chance)
        int[] weightedActions = {
                0, 0, 0, 0, 0, 0, // Camera movement (most common)
                1, 1,             // Hover player
                2, 2,             // Hover NPC
                3, 3, 3, 3,       // Move mouse (gradual)
                4,                // Camera zoom
                5, 5,          // Tab switching
                6, 6, 6        // Idle (AFK)
        };

        int action = weightedActions[Random.nextInt(0, weightedActions.length)];

        switch (action) {
            case 0: // Camera Movement (Arrow Keys or Middle-Mouse)
                if (Random.nextInt(0,10) < 3) { // 30% chance for middle-mouse drag
                    Camera.concurrentlyTurnTo(Random.nextInt(0, 360), Random.nextInt(0, 90));
                    System.out.println("🎥 Middle-mouse camera drag");
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
                }
                break;

            case 2: // Hover NPC
                Npc npc = Npcs.newQuery().filter(Npc::isValid).results().random();
                if (npc != null) {
                    Mouse.move(npc);
                    System.out.println("🧟 Hovered NPC: " + npc.getName());
                    Execution.delay(Random.nextInt(300, 800));
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

            case 5: // Tab Switching (F-Keys)
                int[] tabs = {
                        KeyEvent.VK_F1, // Combat
                        KeyEvent.VK_F2, // Stats
                        KeyEvent.VK_F3, // Quest
                        KeyEvent.VK_F4, // Inventory
                        KeyEvent.VK_F5, // Prayer
                        KeyEvent.VK_F6, // Magic
                };

                int tab = tabs[Random.nextInt(0, tabs.length)];
                Keyboard.pressKey(tab);
                Execution.delay(Random.nextInt(100, 250));
                Keyboard.releaseKey(tab);
                System.out.println("📖 Switched tab: F" + (tab - KeyEvent.VK_F1 + 1));

                if (Random.nextInt(0,10) < 3) { // 30% chance to return to inventory
                    Keyboard.pressKey(KeyEvent.VK_F4);
                    Execution.delay(Random.nextInt(100, 250));
                    Keyboard.releaseKey(KeyEvent.VK_F4);
                    System.out.println("↩️ Returned to Inventory");
                }
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
}
