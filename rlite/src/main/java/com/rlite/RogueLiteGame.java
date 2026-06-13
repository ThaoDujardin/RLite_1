package com.rlite;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;

public class RogueLiteGame extends ApplicationAdapter {
    private static final int TILE_SIZE = 40; // pixels
    private static final Color COLOR_WALL = new Color(0.2f, 0.2f, 0.2f, 1f);
    private static final Color COLOR_FLOOR = new Color(0.8f, 0.8f, 0.8f, 1f);
    private static final Color COLOR_PLAYER = new Color(0f, 1f, 0f, 1f);

    private RogueLiteEngine engine;
    private ShapeRenderer shapeRenderer;
    private float timeSinceLastMove = 0f;
    private static final float MOVE_DELAY = 0.1f; // seconds between moves

    public RogueLiteGame(RogueLiteEngine engine) {
        this.engine = engine;
    }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
    }

    @Override
    public void render() {
        handleInput();

        // Clear screen
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render game
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderDungeon();
        renderPlayer();
        shapeRenderer.end();
    }

    private void handleInput() {
        timeSinceLastMove += Gdx.graphics.getDeltaTime();

        if (timeSinceLastMove < MOVE_DELAY) {
            return;
        }

        Direction direction = null;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            direction = Direction.UP;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            direction = Direction.DOWN;
        } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            direction = Direction.LEFT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            direction = Direction.RIGHT;
        }

        if (direction != null) {
            engine.performTurn(direction);
            timeSinceLastMove = 0f;
        }
    }

    private void renderDungeon() {
        DungeonMap map = engine.gameState().map();

        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                Position pos = new Position(x, y);
                TileType tile = map.tileAt(pos);

                Color tileColor = tile == TileType.WALL ? COLOR_WALL : COLOR_FLOOR;
                shapeRenderer.setColor(tileColor);

                int screenX = x * TILE_SIZE;
                int screenY = y * TILE_SIZE;
                shapeRenderer.rect(screenX, screenY, TILE_SIZE, TILE_SIZE);

                // Draw border
                shapeRenderer.setColor(0f, 0f, 0f, 1f);
                shapeRenderer.rect(screenX, screenY, TILE_SIZE, TILE_SIZE, 0, 0, 0, 0);
            }
        }
    }

    private void renderPlayer() {
        Position playerPos = engine.gameState().playerPosition();
        shapeRenderer.setColor(COLOR_PLAYER);

        int screenX = playerPos.x() * TILE_SIZE;
        int screenY = playerPos.y() * TILE_SIZE;

        // Draw player as a circle in the center of the tile
        shapeRenderer.circle(screenX + TILE_SIZE / 2f, screenY + TILE_SIZE / 2f, TILE_SIZE / 3f);
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }
}
