package de.enderling.monstercombat;


import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class PushableBlock extends Character {
    public PushableBlock(MonsterCombatGame monsterCombatGame, TiledMapTileLayer.Cell cell, int x, int y) {
        super(monsterCombatGame, cell, x, y);
    }

    @Override
    protected boolean handleBlockingCell(int dx, int dy, int newX, int newY) {
        TiledMapTileLayer.Cell blockingCell = monsterCombatGame.moveableLayer.getCell(newX, newY);
        if (blockingCell == null) {
            return true;
        }

        MapProperties blockingCellProperties = blockingCell.getTile().getProperties();

        if (!blockingCellProperties.containsKey("verschiebbar")) {
            return false;
        }

        //TODO: Find existing Character instance instead!
        Character pushedCharacter = new PushableBlock(monsterCombatGame, blockingCell, newX, newY);
        return pushedCharacter.moveCharacter(dx, dy);
    }
}
