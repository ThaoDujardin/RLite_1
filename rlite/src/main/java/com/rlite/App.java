package com.rlite;

public class App {
    public static void main(String[] args) {
        DungeonMap map = DungeonMap.simpleRoom(10, 8);
        GameState gameState = new GameState(map, new Position(1, 1));
        RogueLiteEngine engine = new RogueLiteEngine(gameState);

        engine.performTurn(Direction.RIGHT);
        engine.performTurn(Direction.DOWN);
        engine.performTurn(null);

        System.out.println("Turns: " + gameState.turnsSurvived());
        System.out.println(map.render(gameState.playerPosition()));
    }
}
