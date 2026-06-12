package com.rlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class App {
    private static final int DEFAULT_MAP_WIDTH = 10;
    private static final int DEFAULT_MAP_HEIGHT = 8;
    private static final Difficulty DEFAULT_DIFFICULTY = Difficulty.NORMAL;
    private static final String PLAYER_CAUGHT_MESSAGE = "An enemy got you. You lose!";
    private static final String CLEAR_SCREEN = "\u001b[H\u001b[2J";
    private static final Map<String, Direction> TEXT_COMMAND_TO_DIRECTION = Map.ofEntries(
            Map.entry("w", Direction.UP),
            Map.entry("up", Direction.UP),
            Map.entry("s", Direction.DOWN),
            Map.entry("down", Direction.DOWN),
            Map.entry("a", Direction.LEFT),
            Map.entry("left", Direction.LEFT),
            Map.entry("d", Direction.RIGHT),
            Map.entry("right", Direction.RIGHT)
    );
    private static final Map<String, Direction> EXACT_COMMAND_TO_DIRECTION = Map.ofEntries(
            Map.entry("\u001b[A", Direction.UP),
            Map.entry("\u001b[B", Direction.DOWN),
            Map.entry("\u001b[D", Direction.LEFT),
            Map.entry("\u001b[C", Direction.RIGHT)
    );

    public static void main(String[] args) throws IOException {
        GameSetup setup;
        try {
            setup = parseGameSetup(args);
        } catch (IllegalArgumentException exception) {
            System.out.println("Invalid game options: " + exception.getMessage());
            System.out.println("Usage: --size=<width>x<height> --difficulty=<easy|normal|hard>");
            return;
        }

        Random random = new Random();
        DungeonMap map = DungeonMap.simpleRoom(setup.mapWidth(), setup.mapHeight());
        GameState gameState = new GameState(map, new Position(1, 1));
        RogueLiteEngine engine = new RogueLiteEngine(gameState);
        Position exit = new Position(map.width() - 2, map.height() - 2);
        List<Position> enemies = initializeEnemies(map, gameState.playerPosition(), exit, setup.enemyCount(), random);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Reach X before turns run out.");
        System.out.println("Controls: W/A/S/D (or up/down/left/right), . to wait, q to quit.");
        System.out.println("Difficulty: " + setup.difficulty().name().toLowerCase()
                + " | Map: " + map.width() + "x" + map.height()
                + " | Enemies: " + enemies.size());

        while (true) {
            System.out.print(CLEAR_SCREEN);
            System.out.flush();
            System.out.println("Turns: " + gameState.turnsSurvived() + "/" + setup.maxTurns());
            System.out.println(renderWithActors(map, gameState.playerPosition(), exit, enemies));

            if (gameState.playerPosition().equals(exit)) {
                System.out.println("You escaped. You win!");
                break;
            }

            if (gameState.turnsSurvived() >= setup.maxTurns()) {
                System.out.println("Out of time. You lose!");
                break;
            }

            if (enemies.contains(gameState.playerPosition())) {
                System.out.println(PLAYER_CAUGHT_MESSAGE);
                break;
            }

            System.out.print("Move> ");
            String input = reader.readLine();
            if (input == null) {
                System.out.println("Input stream closed.");
                break;
            }
            ParsedInput parsedInput = parseInput(input);

            if (parsedInput.action() == InputAction.QUIT) {
                System.out.println("Goodbye.");
                break;
            }

            if (parsedInput.action() == InputAction.INVALID) {
                System.out.println("Invalid input.");
                pause(reader);
                continue;
            }

            boolean moved = engine.performTurn(parsedInput.direction());
            if (parsedInput.action() == InputAction.MOVE && !moved) {
                System.out.println("You bump into a wall.");
                pause(reader);
            }

            if (!gameState.playerPosition().equals(exit)
                    && (parsedInput.action() == InputAction.MOVE || parsedInput.action() == InputAction.WAIT)) {
                boolean playerCaught = advanceEnemies(map, enemies, gameState.playerPosition(), exit, random);
                if (playerCaught) {
                    System.out.println(PLAYER_CAUGHT_MESSAGE);
                    break;
                }
            }
        }
    }

    static ParsedInput parseInput(String rawInput) {
        if (rawInput == null) {
            return ParsedInput.invalid();
        }

        Direction direction = parseDirection(rawInput);
        if (direction != null) {
            return ParsedInput.move(direction);
        }

        String input = rawInput.trim().toLowerCase();
        if (input.isEmpty()) {
            return ParsedInput.invalid();
        }
        if (".".equals(input) || "wait".equals(input)) {
            return ParsedInput.waitTurn();
        }
        if ("q".equals(input) || "quit".equals(input)) {
            return ParsedInput.quit();
        }
        return ParsedInput.invalid();
    }

    static String renderWithActors(DungeonMap map, Position player, Position exit, List<Position> enemies) {
        String rendered = map.render(player);
        String[] lines = rendered.split("\n", -1);
        if (!player.equals(exit) && isPositionWithinRenderedBounds(exit, lines)) {
            char[] lineChars = lines[exit.y()].toCharArray();
            lineChars[exit.x()] = 'X';
            lines[exit.y()] = new String(lineChars);
        }

        for (Position enemy : enemies) {
            if (!enemy.equals(player) && isPositionWithinRenderedBounds(enemy, lines)) {
                char[] lineChars = lines[enemy.y()].toCharArray();
                lineChars[enemy.x()] = 'e';
                lines[enemy.y()] = new String(lineChars);
            }
        }
        return String.join("\n", lines);
    }

    static GameSetup parseGameSetup(String[] args) {
        int mapWidth = DEFAULT_MAP_WIDTH;
        int mapHeight = DEFAULT_MAP_HEIGHT;
        Difficulty difficulty = DEFAULT_DIFFICULTY;

        for (String arg : args) {
            if (arg == null || arg.isBlank()) {
                continue;
            }

            if (arg.startsWith("--size=")) {
                String value = arg.substring("--size=".length());
                int separator = value.toLowerCase().indexOf('x');
                if (separator <= 0 || separator >= value.length() - 1) {
                    throw new IllegalArgumentException("size must use widthxheight format");
                }
                try {
                    mapWidth = Integer.parseInt(value.substring(0, separator));
                    mapHeight = Integer.parseInt(value.substring(separator + 1));
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("size must use numeric width and height");
                }
                continue;
            }

            if (arg.startsWith("--difficulty=")) {
                String value = arg.substring("--difficulty=".length());
                difficulty = Difficulty.from(value);
                continue;
            }

            throw new IllegalArgumentException("Unknown option: " + arg);
        }

        if (mapWidth < 5 || mapHeight < 5) {
            throw new IllegalArgumentException("map size must be at least 5x5");
        }

        return new GameSetup(mapWidth, mapHeight, difficulty.maxTurns(), difficulty.enemyCount(), difficulty);
    }

    static List<Position> initializeEnemies(
            DungeonMap map, Position player, Position exit, int enemyCount, Random random) {
        List<Position> floorTiles = new ArrayList<>();
        for (int y = 1; y < map.height() - 1; y++) {
            for (int x = 1; x < map.width() - 1; x++) {
                Position candidate = new Position(x, y);
                if (!candidate.equals(player) && !candidate.equals(exit)) {
                    floorTiles.add(candidate);
                }
            }
        }
        Collections.shuffle(floorTiles, random);
        return new ArrayList<>(floorTiles.subList(0, Math.min(enemyCount, floorTiles.size())));
    }

    static boolean advanceEnemies(
            DungeonMap map, List<Position> enemies, Position player, Position exit, Random random) {
        Set<Position> occupied = new HashSet<>(enemies);
        List<Position> moved = new ArrayList<>(enemies.size());

        for (Position enemy : enemies) {
            occupied.remove(enemy);
            Position nextPosition = chooseEnemyMove(enemy, player, map, exit, occupied, random);
            moved.add(nextPosition);
            occupied.add(nextPosition);
        }

        enemies.clear();
        enemies.addAll(moved);
        return enemies.contains(player);
    }

    private static Position chooseEnemyMove(
            Position enemy, Position player, DungeonMap map, Position exit, Set<Position> occupied, Random random) {
        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(directions, random);

        Position best = enemy;
        int bestDistance = manhattanDistance(enemy, player);
        for (Direction direction : directions) {
            Position candidate = enemy.move(direction);
            if (!map.isWalkable(candidate) || candidate.equals(exit) || occupied.contains(candidate)) {
                continue;
            }
            int distance = manhattanDistance(candidate, player);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static int manhattanDistance(Position source, Position target) {
        return Math.abs(source.x() - target.x()) + Math.abs(source.y() - target.y());
    }

    private static Direction parseDirection(String rawInput) {
        if (rawInput == null) {
            return null;
        }
        Direction directMatch = EXACT_COMMAND_TO_DIRECTION.get(rawInput);
        if (directMatch != null) {
            return directMatch;
        }
        return TEXT_COMMAND_TO_DIRECTION.get(rawInput.trim().toLowerCase());
    }

    private static boolean isPositionWithinRenderedBounds(Position position, String[] lines) {
        return position.y() >= 0
                && position.y() < lines.length
                && position.x() >= 0
                && position.x() < lines[position.y()].length();
    }

    private static void pause(BufferedReader reader) throws IOException {
        System.out.print("Press Enter to continue...");
        reader.readLine();
    }

    enum InputAction {
        MOVE,
        WAIT,
        QUIT,
        INVALID
    }

    enum Difficulty {
        EASY(40, 1),
        NORMAL(30, 2),
        HARD(24, 3);

        private final int maxTurns;
        private final int enemyCount;

        Difficulty(int maxTurns, int enemyCount) {
            this.maxTurns = maxTurns;
            this.enemyCount = enemyCount;
        }

        int maxTurns() {
            return maxTurns;
        }

        int enemyCount() {
            return enemyCount;
        }

        static Difficulty from(String value) {
            String normalized = value == null ? "" : value.trim().toUpperCase();
            for (Difficulty difficulty : values()) {
                if (difficulty.name().equals(normalized)) {
                    return difficulty;
                }
            }
            throw new IllegalArgumentException("difficulty must be easy, normal or hard");
        }
    }

    record GameSetup(int mapWidth, int mapHeight, int maxTurns, int enemyCount, Difficulty difficulty) {
    }

    record ParsedInput(InputAction action, Direction direction) {
        static ParsedInput move(Direction direction) {
            return new ParsedInput(InputAction.MOVE, direction);
        }

        static ParsedInput waitTurn() {
            return new ParsedInput(InputAction.WAIT, null);
        }

        static ParsedInput quit() {
            return new ParsedInput(InputAction.QUIT, null);
        }

        static ParsedInput invalid() {
            return new ParsedInput(InputAction.INVALID, null);
        }
    }
}
