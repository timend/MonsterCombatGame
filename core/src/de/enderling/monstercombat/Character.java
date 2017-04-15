package de.enderling.monstercombat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.Timer;

import static com.badlogic.gdx.graphics.GL20.GL_BLEND;
import static com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA;
import static com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA;

class Character {
    protected MonsterCombatGame monsterCombatGame;
    private TiledMapTileLayer.Cell cell;
    private TiledMapTileLayer.Cell effectCell;
    private int x;
    private int y;
    private Float lifePoints;
    private Timer.Task task;
    private Timer.Task effectTask;

    public Character(MonsterCombatGame monsterCombatGame, TiledMapTileLayer.Cell cell, int x, int y) {
        this.monsterCombatGame = monsterCombatGame;
        this.cell = cell;
        this.x = x;
        this.y = y;
        this.lifePoints = getMaximumLifePoints();
    }

    private Float getMaximumLifePoints() {
        return cell.getTile().getProperties().get("leben", null, Float.class);
    }

    public TiledMapTileLayer.Cell getCell() {
        return cell;
    }

    public int getX() {
        return x;
    }

    public void setCell(TiledMapTileLayer.Cell cell) {
        this.cell = cell;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void init() {
        float geschwindigkeit = cell.getTile().getProperties().get("schnell", 1.5f, Float.class);
        task = new Timer.Task() {
            @Override
            public void run() {
                if (monsterCombatGame.moveableLayer.getCell(x, y) != cell) {
                    this.cancel();
                }

                if (monsterCombatGame.random.nextFloat() > 0.6) {
                    if (monsterCombatGame.random.nextBoolean()) {
                        moveCharacter(monsterCombatGame.random.nextInt(3) - 1, 0);
                    } else {
                        moveCharacter(0, monsterCombatGame.random.nextInt(3) - 1);
                    }
                }
            }
        };
        monsterCombatGame.timer.scheduleTask(task, 0, 1 / geschwindigkeit);
    }

    public void hit(float attack, TiledMapTile effect) {
        if (lifePoints == null) {
            return;
        }

        if (effect != null) {
            effectCell = new TiledMapTileLayer.Cell();
            effectCell.setTile(effect);
            monsterCombatGame.fireLayer.setCell(x, y, effectCell);

            float effectDuration = effect.getProperties().get("dauer", 1f, Float.class);

            if (effectTask != null) {
                effectTask.cancel();
                effectTask = null;
            }

            effectTask = monsterCombatGame.timer.scheduleTask(new Timer.Task() {
                @Override
                public void run() {
                    effectCell = null;
                    effectTask = null;
                    monsterCombatGame.fireLayer.setCell(x, y, null);
                }
            }, effectDuration);

        }

        lifePoints -= attack;

        if (lifePoints <= 0) {
            monsterCombatGame.moveableLayer.setCell(x, y, null);
            monsterCombatGame.fireLayer.setCell(x, y, null);
            task.cancel();
            monsterCombatGame.monsters.remove(this);
        }
    }

    /**
     * Returns if the character moved.
     *
     * @param dx
     * @param dy
     * @return
     */
    public boolean moveCharacter(int dx, int dy) {
        int newPlayerX = getX() + dx;
        int newPlayerY = getY() + dy;

        if (newPlayerX < 0 || newPlayerX >= monsterCombatGame.moveableLayer.getWidth()) {
            return false;
        }

        if (newPlayerY < 0 || newPlayerY >= monsterCombatGame.moveableLayer.getHeight()) {
            return false;
        }

        if (!handleBlockingCell(dx, dy, newPlayerX, newPlayerY)) {
            return false;
        }

        monsterCombatGame.moveableLayer.setCell(getX(), getY(), null);
        monsterCombatGame.fireLayer.setCell(x, y, null);
        setX(newPlayerX);
        setY(newPlayerY);
        monsterCombatGame.moveableLayer.setCell(getX(), getY(), getCell());
        monsterCombatGame.fireLayer.setCell(x, y, effectCell);
        return true;
    }

    public void draw(ShapeRenderer shapeRenderer) {
        if (lifePoints == null) {
            return;
        }

        float relativeLifePoints = lifePoints / getMaximumLifePoints();

        if (relativeLifePoints >= 1.0f) {
            return;
        }

        Gdx.gl.glEnable(GL_BLEND);
        Gdx.gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (relativeLifePoints <= 0.25) {
            shapeRenderer.setColor(1, 0, 0, .8f);
        } else if (relativeLifePoints <= 0.5) {
            shapeRenderer.setColor(1, 1, 0, .8f);
        } else {
            shapeRenderer.setColor(0, 1, 0, .8f);
        }

        shapeRenderer.rect(getX() * 32, getY() * 32, 32.0f * relativeLifePoints, 5);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 1, .8f);
        shapeRenderer.rect(getX() * 32, getY() * 32, 32, 5);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL_BLEND);
    }

    /**
     * Returns if the blocking cell got dissolved (or there was none at the first place)
     *
     * @param dx
     * @param dy
     * @param newPlayerX
     * @param newPlayerY
     * @return
     */
    protected boolean handleBlockingCell(int dx, int dy, int newPlayerX, int newPlayerY) {
        TiledMapTileLayer.Cell blockingCell = monsterCombatGame.moveableLayer.getCell(newPlayerX, newPlayerY);
        if (blockingCell == null) {
            return true;
        }

        return false;
    }
}
