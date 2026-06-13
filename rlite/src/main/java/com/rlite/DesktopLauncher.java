package com.rlite;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("RogueLite");
        config.setWindowedMode(800, 640);
        config.useVsync(true);
        config.setForegroundFPS(60);

        // Create game engine
        DungeonMap map = DungeonMap.simpleRoom(20, 16);
        GameState gameState = new GameState(map, new Position(1, 1));
        RogueLiteEngine engine = new RogueLiteEngine(gameState);

        // Launch game
        new Lwjgl3Application(new RogueLiteGame(engine), config);
    }
}
