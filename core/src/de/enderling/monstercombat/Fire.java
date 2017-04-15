package de.enderling.monstercombat;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.Timer;

/**
 * Created by Admin on 15.04.17.
 */
class Fire {
    private MonsterCombatGame monsterCombatGame;
    private TiledMapTileLayer.Cell cell;
    private int x;
    private int y;
    private int dx;
    private int dy;

    public Fire(MonsterCombatGame monsterCombatGame, TiledMapTile tile, int x, int y, int dx, int dy) {
        this.monsterCombatGame = monsterCombatGame;
        cell = new TiledMapTileLayer.Cell();
        cell.setTile(tile);

        monsterCombatGame.fireLayer.setCell(x, y, cell);

        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public void init() {
        float geschwindigkeit = cell.getTile().getProperties().get("schnell", 1.5f, Float.class);
        Timer.Task task = new Timer.Task() {
            @Override
            public void run() {
                int newX = x + dx;
                int newY = y + dy;

                if (handleCollision(newX, newY)) {
                    monsterCombatGame.fireLayer.setCell(x, y, null);
                    this.cancel();
                    return;
                }

                monsterCombatGame.fireLayer.setCell(x, y, null);
                x = newX;
                y = newY;
                monsterCombatGame.fireLayer.setCell(x, y, cell);
            }
        };
        monsterCombatGame.timer.scheduleTask(task, 0, 1 / geschwindigkeit);
    }

    private boolean handleCollision(int newX, int newY) {
        if (newX < 0 || newX >= monsterCombatGame.moveableLayer.getWidth()) {
            return true;
        }

        if (newY < 0 || newY >= monsterCombatGame.moveableLayer.getHeight()) {
            return true;
        }

        TiledMapTileLayer.Cell targetCell = monsterCombatGame.moveableLayer.getCell(newX, newY);
        if (targetCell != null) {

            Character monster = monsterCombatGame.findMonster(newX, newY);

            if (monster != null) {
                float stärke = cell.getTile().getProperties().get("stärke", 1f, Float.class);

                Integer effectIndex = cell.getTile().getProperties().get("effekt", null, Integer.class);
                TiledMapTile effect = null;

                if (effectIndex != null) {
                    effect = monsterCombatGame.tiledMap.getTileSets().getTileSet("dungeon").getTile(effectIndex + 1);
                }

                monster.hit(stärke, effect);
            }

            return true;
        }


        return false;
    }

}
