package com.rlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RogueLiteEngineTest {

    @Test
    void simpleRoomHasWallsAndWalkableInterior() {
        DungeonMap map = DungeonMap.simpleRoom(6, 5);

        assertEquals(TileType.WALL, map.tileAt(new Position(0, 0)));
        assertEquals(TileType.WALL, map.tileAt(new Position(5, 4)));
        assertTrue(map.isWalkable(new Position(1, 1)));
        assertTrue(map.isWalkable(new Position(4, 3)));
    }

    @Test
    void performTurnMovesPlayerOnlyOnWalkableTiles() {
        DungeonMap map = DungeonMap.simpleRoom(4, 4);
        GameState gameState = new GameState(map, new Position(1, 1));
        RogueLiteEngine engine = new RogueLiteEngine(gameState);

        assertTrue(engine.performTurn(Direction.RIGHT));
        assertEquals(new Position(2, 1), gameState.playerPosition());

        assertFalse(engine.performTurn(Direction.RIGHT));
        assertEquals(new Position(2, 1), gameState.playerPosition());
        assertEquals(2, gameState.turnsSurvived());
    }

    @Test
    void performTurnSupportsWaiting() {
        DungeonMap map = DungeonMap.simpleRoom(5, 5);
        GameState gameState = new GameState(map, new Position(1, 1));
        RogueLiteEngine engine = new RogueLiteEngine(gameState);

        assertFalse(engine.performTurn(null));
        assertEquals(new Position(1, 1), gameState.playerPosition());
        assertEquals(1, gameState.turnsSurvived());
    }
}
