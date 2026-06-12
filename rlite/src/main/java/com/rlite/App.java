package com.rlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class App {
    private static final int MAX_TURNS = 30;
    private static final String CLEAR_SCREEN = "\u001b[H\u001b[2J";

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

        if ("\u001b[A".equals(rawInput)) {
            return ParsedInput.move(Direction.UP);
        }
        if ("\u001b[B".equals(rawInput)) {
            return ParsedInput.move(Direction.DOWN);
        }
        if ("\u001b[D".equals(rawInput)) {
            return ParsedInput.move(Direction.LEFT);
        }
        if ("\u001b[C".equals(rawInput)) {
            return ParsedInput.move(Direction.RIGHT);
        }

        String input = rawInput.trim().toLowerCase();
        if (input.isEmpty() || ".".equals(input) || "wait".equals(input)) {
            return ParsedInput.waitTurn();
        }
        if ("q".equals(input) || "quit".equals(input)) {
            return ParsedInput.quit();
        }
        return switch (input) {
            case "w", "up" -> ParsedInput.move(Direction.UP);
            case "s", "down" -> ParsedInput.move(Direction.DOWN);
            case "a", "left" -> ParsedInput.move(Direction.LEFT);
            case "d", "right" -> ParsedInput.move(Direction.RIGHT);
            default -> ParsedInput.invalid();
        };
    }

    private static String renderWithExit(DungeonMap map, Position player, Position exit) {
        String rendered = map.render(player);
        if (player.equals(exit)) {
            return rendered;
        }

        String[] lines = rendered.split("\n", -1);
        if (exit.y() >= 0 && exit.y() < lines.length && exit.x() >= 0 && exit.x() < lines[exit.y()].length()) {
            char[] lineChars = lines[exit.y()].toCharArray();
            lineChars[exit.x()] = 'X';
            lines[exit.y()] = new String(lineChars);
        }
        return String.join("\n", lines);
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
