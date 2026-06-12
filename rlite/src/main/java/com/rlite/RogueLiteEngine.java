package com.rlite;

public class RogueLiteEngine {
    private final GameState gameState;

    public RogueLiteEngine(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState gameState() {
        return gameState;
    }

    public boolean performTurn(Direction direction) {
        if (direction == null) {
            gameState.waitTurn();
            return false;
        }
        return gameState.attemptMove(direction);
    }
}
