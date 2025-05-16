package com.runemate.party.agility;

import com.runemate.game.api.client.ClientUI;
import com.runemate.game.api.hybrid.entities.GameObject;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.Coordinate;
import com.runemate.game.api.hybrid.region.GameObjects;
import com.runemate.game.api.hybrid.region.Players;
import com.runemate.game.api.script.Execution;
import com.runemate.game.api.script.framework.LoopingBot;

public class DraynorAgility extends LoopingBot {

    @Override
    public void onStart(String... args) {
        setLoopDelay(200, 400); // Delay between loops in ms
        ClientUI.showAlert("Draynor Agility Bot Started");
    }

    @Override
    public void onLoop() {
        GameObject obstacle = getNextObstacle();

        Coordinate pos = Players.getLocal().getPosition();
        System.out.println("Player position: " + pos);

        if (obstacle != null) {
            String[] actions = {"Climb", "Jump", "Cross", "Balance", "jump-up", "climb-down"};
            for (String action : actions) {
                if (obstacle.interact(action)) {
                    Execution.delayUntil(
                            () -> !Players.getLocal().isMoving() && Players.getLocal().getAnimationId() == -1,
                            1000,
                            4000
                    );
                    return;
                }
            }
            ClientUI.showAlert("Obstacle found but interaction failed.");
        } else {
            ClientUI.showProgressToast("Could not find next obstacle.");
        }
    }

    private GameObject getNextObstacle() {
        Coordinate pos = Players.getLocal().getPosition();
        if (pos == null) return null;

        if (pos.getPlane() == 0) {
            return getObstacle("Rough wall", new Coordinate(3103, 3279, 0));
        }

        if (pos.distanceTo(new Coordinate(3103, 3279, 3)) < 6) {
            return getObstacle("Tightrope", new Coordinate(3098, 3277, 3));
        }

        if (pos.distanceTo(new Coordinate(3098, 3277, 3)) < 6) {
            return getObstacle("Narrow wall", new Coordinate(3091, 3276, 3));
        }

        if (pos.distanceTo(new Coordinate(3091, 3276, 3)) < 6) {
            return getObstacle("Wall", new Coordinate(3089, 3266, 3));
        }

        if (pos.distanceTo(new Coordinate(3089, 3266, 3)) < 6) {
            return getObstacle("Gap", new Coordinate(3088, 3261, 3));
        }

        if (pos.distanceTo(new Coordinate(3088, 3261, 3)) < 6) {
            return getObstacle("Crate", new Coordinate(3094, 3260, 3));
        }

        return null;
    }

    private GameObject getObstacle(String name, Coordinate near) {
        Area area = new Area.Rectangular(
                new Coordinate(near.getX() - 2, near.getY() - 2, near.getPlane()),
                new Coordinate(near.getX() + 2, near.getY() + 2, near.getPlane())
        );

        GameObject obstacle = GameObjects.newQuery()
                .names(name)
                .within(area)
                .results()
                .nearest();

        if (obstacle == null) {
            System.out.println("Failed to find obstacle: " + name + " near " + near);
        } else {
            System.out.println("Found obstacle: " + obstacle.getDefinition() + " at " + obstacle.getPosition());
        }

        return obstacle;
    }
}