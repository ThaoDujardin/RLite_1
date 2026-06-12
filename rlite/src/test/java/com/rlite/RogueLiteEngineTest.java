package com.rlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

    @Test
    void parseInputSupportsMovementWaitingAndQuit() {
        App.ParsedInput up = App.parseInput("w");
        assertEquals(App.InputAction.MOVE, up.action());
        assertEquals(Direction.UP, up.direction());

        App.ParsedInput right = App.parseInput("right");
        assertEquals(App.InputAction.MOVE, right.action());
        assertEquals(Direction.RIGHT, right.direction());

        App.ParsedInput arrowUp = App.parseInput("\u001b[A");
        assertEquals(App.InputAction.MOVE, arrowUp.action());
        assertEquals(Direction.UP, arrowUp.direction());

        App.ParsedInput waitTurn = App.parseInput(".");
        assertEquals(App.InputAction.WAIT, waitTurn.action());
        assertNull(waitTurn.direction());

        App.ParsedInput quit = App.parseInput("q");
        assertEquals(App.InputAction.QUIT, quit.action());
    }

    @Test
    void parseInputRejectsUnknownCommands() {
        App.ParsedInput invalid = App.parseInput("teleport");
        assertEquals(App.InputAction.INVALID, invalid.action());
        assertNull(invalid.direction());

        App.ParsedInput empty = App.parseInput("   ");
        assertEquals(App.InputAction.INVALID, empty.action());
        assertNull(empty.direction());

        App.ParsedInput nullInput = App.parseInput(null);
        assertEquals(App.InputAction.INVALID, nullInput.action());
        assertNull(nullInput.direction());
    }

    @Test
    void parseGameSetupUsesDefaultsWhenNoArgsAreProvided() {
        App.GameSetup setup = App.parseGameSetup(new String[0]);

        assertEquals(10, setup.mapWidth());
        assertEquals(8, setup.mapHeight());
        assertEquals(30, setup.maxTurns());
        assertEquals(2, setup.enemyCount());
        assertEquals(App.Difficulty.NORMAL, setup.difficulty());
    }

    @Test
    void parseGameSetupSupportsCustomSizeAndDifficulty() {
        App.GameSetup setup = App.parseGameSetup(new String[]{"--size=14x10", "--difficulty=hard"});

        assertEquals(14, setup.mapWidth());
        assertEquals(10, setup.mapHeight());
        assertEquals(24, setup.maxTurns());
        assertEquals(3, setup.enemyCount());
        assertEquals(App.Difficulty.HARD, setup.difficulty());
    }

    @Test
    void parseGameSetupRejectsInvalidOptions() {
        assertThrows(IllegalArgumentException.class, () -> App.parseGameSetup(new String[]{"--size=4x4"}));
        assertThrows(IllegalArgumentException.class, () -> App.parseGameSetup(new String[]{"--difficulty=nightmare"}));
        assertThrows(IllegalArgumentException.class, () -> App.parseGameSetup(new String[]{"--unknown=true"}));
    }

    @Test
    void initializeEnemiesAvoidsPlayerExitAndDuplicates() {
        DungeonMap map = DungeonMap.simpleRoom(6, 6);
        Position player = new Position(1, 1);
        Position exit = new Position(4, 4);

        List<Position> enemies = App.initializeEnemies(map, player, exit, 5, new Random(42));

        assertEquals(5, enemies.size());
        assertFalse(enemies.contains(player));
        assertFalse(enemies.contains(exit));
        assertEquals(enemies.size(), enemies.stream().distinct().count());
    }

    @Test
    void advanceEnemiesMovesTowardPlayerAndCanCatch() {
        DungeonMap map = DungeonMap.simpleRoom(6, 6);
        Position player = new Position(1, 1);
        Position exit = new Position(4, 4);
        List<Position> enemies = new ArrayList<>(List.of(new Position(3, 1)));

        boolean caughtOnFirstTurn = App.advanceEnemies(map, enemies, player, exit, new Random(1));
        assertFalse(caughtOnFirstTurn);
        assertEquals(new Position(2, 1), enemies.get(0));

        boolean caughtOnSecondTurn = App.advanceEnemies(map, enemies, player, exit, new Random(1));
        assertTrue(caughtOnSecondTurn);
        assertEquals(player, enemies.get(0));
    }
}
