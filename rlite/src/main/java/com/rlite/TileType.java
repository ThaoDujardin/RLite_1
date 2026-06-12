package com.rlite;

public enum TileType {
    FLOOR(true, '.'),
    WALL(false, '#');

    private final boolean walkable;
    private final char display;

    TileType(boolean walkable, char display) {
        this.walkable = walkable;
        this.display = display;
    }

    public boolean walkable() {
        return walkable;
    }

    public char display() {
        return display;
    }
}
