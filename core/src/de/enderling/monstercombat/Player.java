package de.enderling.monstercombat;

import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class Player extends Character {
    public Player(MonsterCombatGame monsterCombatGame, TiledMapTileLayer.Cell cell, int x, int y) {
        super(monsterCombatGame, cell, x, y);
    }

    @Override
    protected boolean handleBlockingCell(int dx, int dy, int newX, int newY) {

        TiledMapTileLayer.Cell blockingCell = monsterCombatGame.wallLayer.getCell(newX, newY);

//        TiledMapTileLayer.Cell blockingCell = monsterCombatGame.moveableLayer.getCell(newX, newY);
        if (blockingCell == null) {
            return true;
        }

        MapProperties blockingCellProperties = blockingCell.getTile().getProperties();
        if (blockingCellProperties.containsKey("zauber")) {
            monsterCombatGame.destroyStones += blockingCellProperties.get("zauber", 0, Integer.class);
            monsterCombatGame.destroyStonesLabel.setText(Integer.toString(monsterCombatGame.destroyStones));
            return true;
        }

        if (!blockingCellProperties.containsKey("verschiebbar")) {
            return false;
        }

        if (monsterCombatGame.destroyStones > 0) {
            monsterCombatGame.destroyStones--;
            monsterCombatGame.destroyStonesLabel.setText(Integer.toString(monsterCombatGame.destroyStones));
            return true;
        }

        //TODO: Find existing Character instance instead!
        Character pushedCharacter = new PushableBlock(monsterCombatGame, blockingCell, newX, newY);

        return pushedCharacter.moveCharacter(dx, dy);
    }
}
