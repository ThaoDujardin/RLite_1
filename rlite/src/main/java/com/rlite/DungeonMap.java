package com.rlite;

public class DungeonMap {
    private final TileType[][] tiles;

    public DungeonMap(TileType[][] tiles) {
        if (tiles.length == 0 || tiles[0].length == 0) {
            throw new IllegalArgumentException("Map must not be empty");
        }
        this.tiles = tiles;
    }

    public static DungeonMap simpleRoom(int width, int height) {
        if (width < 3 || height < 3) {
            throw new IllegalArgumentException("Room must be at least 3x3");
        }
        TileType[][] tiles = new TileType[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean border = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                tiles[y][x] = border ? TileType.WALL : TileType.FLOOR;
            }
        }
        return new DungeonMap(tiles);
    }

    public int width() {
        return tiles[0].length;
    }

    public int height() {
        return tiles.length;
    }

    public TileType tileAt(Position position) {
        if (!isInside(position)) {
            return TileType.WALL;
        }
        return tiles[position.y()][position.x()];
    }

    public boolean isWalkable(Position position) {
        return tileAt(position).walkable();
    }

    public boolean isInside(Position position) {
        return position.x() >= 0 && position.x() < width()
                && position.y() >= 0 && position.y() < height();
    }

    public String render(Position playerPosition) {
        StringBuilder builder = new StringBuilder();
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                if (playerPosition.x() == x && playerPosition.y() == y) {
                    builder.append('@');
                } else {
                    builder.append(tiles[y][x].display());
                }
            }
            builder.append('\n');
        }
        return builder.toString();
    }
}
