package com.rlite;

public class GameState {
    private final DungeonMap map;
    private Position playerPosition;
    private int turnsSurvived;

    public GameState(DungeonMap map, Position playerPosition) {
        if (!map.isWalkable(playerPosition)) {
            throw new IllegalArgumentException("Player must start on a walkable tile");
        }
        this.map = map;
        this.playerPosition = playerPosition;
    }

    public DungeonMap map() {
        return map;
    }

    public Position playerPosition() {
        return playerPosition;
    }

    public int turnsSurvived() {
        return turnsSurvived;
    }

    public boolean attemptMove(Direction direction) {
        Position target = playerPosition.move(direction);
        turnsSurvived++;
        if (!map.isWalkable(target)) {
            return false;
        }
        playerPosition = target;
        return true;
    }

    public void waitTurn() {
        turnsSurvived++;
    }
}
