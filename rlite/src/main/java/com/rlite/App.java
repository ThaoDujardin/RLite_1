package com.rlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class App {
    private static final int MAX_TURNS = 30;
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
        DungeonMap map = DungeonMap.simpleRoom(10, 8);
        GameState gameState = new GameState(map, new Position(1, 1));
        RogueLiteEngine engine = new RogueLiteEngine(gameState);
        Position exit = new Position(map.width() - 2, map.height() - 2);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Reach X before turns run out.");
        System.out.println("Controls: W/A/S/D (or up/down/left/right), . to wait, q to quit.");

        while (true) {
            System.out.print(CLEAR_SCREEN);
            System.out.flush();
            System.out.println("Turns: " + gameState.turnsSurvived() + "/" + MAX_TURNS);
            System.out.println(renderWithExit(map, gameState.playerPosition(), exit));

            if (gameState.playerPosition().equals(exit)) {
                System.out.println("You escaped. You win!");
                break;
            }

            if (gameState.turnsSurvived() >= MAX_TURNS) {
                System.out.println("Out of time. You lose!");
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

    private static String renderWithExit(DungeonMap map, Position player, Position exit) {
        String rendered = map.render(player);
        if (player.equals(exit)) {
            return rendered;
        }

        String[] lines = rendered.split("\n", -1);
        if (isPositionWithinRenderedBounds(exit, lines)) {
            char[] lineChars = lines[exit.y()].toCharArray();
            lineChars[exit.x()] = 'X';
            lines[exit.y()] = new String(lineChars);
        }
        return String.join("\n", lines);
    }

    private static Direction parseDirection(String rawInput) {
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
