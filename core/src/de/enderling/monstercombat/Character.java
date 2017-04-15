package de.enderling.monstercombat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.*;
import com.badlogic.gdx.ai.pfa.indexed.IndexedAStarPathFinder;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
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
    private Float attack;

    private Float lifePoints;
    private Timer.Task task;
    private Timer.Task effectTask;

    public Character(MonsterCombatGame monsterCombatGame, TiledMapTileLayer.Cell cell, int x, int y) {
        this.monsterCombatGame = monsterCombatGame;
        this.cell = cell;
        this.x = x;
        this.y = y;
        this.attack = cell.getTile().getProperties().get("stärke", Float.class);
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


    private static class TileMapNode {
        public int x;
        public int y;

        public TileMapNode(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x +
                    "," + y +
                    ')';
        }
    }

    private class TileMapGraph implements IndexedGraph<TileMapNode> {

        public TileMapGraph(int minX, int minY, int width, int height) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
            this.nodes = new TileMapNode[width][height];
        }

        TileMapNode[][] nodes;

        int minX;
        int minY;
        int width;
        int height;

        public TileMapNode getNode(int x, int y) {
            if (nodes[x - minX][y - minY] == null) {
                nodes[x - minX][y - minY] = new TileMapNode(x, y);
            }

            return nodes[x - minX][y - minY];
        }

        @Override
        public Array<Connection<TileMapNode>> getConnections(TileMapNode fromNode) {
            Array<Connection<TileMapNode>> connections = new Array<Connection<TileMapNode>>(4);

            for (int dx = -1; dx <= 1; dx+=2) {
                addConnection(fromNode, connections, dx, 0);
            }

            for (int dy = -1; dy <= 1; dy+=2) {
                addConnection(fromNode, connections, 0, dy);
            }

            return connections;
        }

        private void addConnection(TileMapNode fromNode, Array<Connection<TileMapNode>> connections, int dx, int dy) {
            int targetX = fromNode.x + dx;
            int targetY = fromNode.y + dy;

            if (targetX < minX || targetX >= minX + width) {
                return;
            }

            if (targetY < minY || targetY >= minY + height) {
                return;
            }

            if (monsterCombatGame.moveableLayer.getCell(targetX, targetY) == null ||
                    ((targetX == monsterCombatGame.player.getX() &&
                    targetY == monsterCombatGame.player.getY())) ||
                    monsterCombatGame.findMonster(targetX, targetY) != null) {
                connections.add(new DefaultConnection<TileMapNode>(fromNode,
                        getNode(targetX, targetY)));
            }
        }

        @Override
        public int getIndex(TileMapNode node) {
            return (node.y - minY) * width + node.x - minX;
        }

        @Override
        public int getNodeCount() {
            return width * height;
        }
    }

    private static class TileMapHeuristic implements Heuristic<TileMapNode> {

        @Override
        public float estimate(TileMapNode node, TileMapNode endNode) {
            return Math.abs(endNode.x - node.x) + Math.abs(endNode.y - node.y);
        }
    }

    public TileMapNode findMoveToPlayer() {
        TileMapGraph tileMapGraph = new TileMapGraph(0, 0, monsterCombatGame.moveableLayer.getWidth(), monsterCombatGame.moveableLayer.getHeight());
        IndexedAStarPathFinder<TileMapNode> pathFinder = new IndexedAStarPathFinder<TileMapNode>(
                tileMapGraph, false);

        TileMapNode fromNode = tileMapGraph.getNode(x,y);
        TileMapNode targetNode = tileMapGraph.getNode(monsterCombatGame.player.getX(), monsterCombatGame.player.getY());

        GraphPath<Connection<TileMapNode>> path = new DefaultGraphPath<Connection<TileMapNode>>();

        if (pathFinder.searchConnectionPath(fromNode, targetNode, new TileMapHeuristic(),  path)) {
            return path.get(0).getToNode();
        } else {
            if (pathFinder.metrics != null) {
                System.out.println("----------------- Indexed A* Path Finder Metrics -----------------");
                System.out.println("Visited nodes................... = " + pathFinder.metrics.visitedNodes);
                System.out.println("Open list additions............. = " + pathFinder.metrics.openListAdditions);
                System.out.println("Open list peak.................. = " + pathFinder.metrics.openListPeak);
            }
            return null;
        }
    }

    public void init() {
        float geschwindigkeit = cell.getTile().getProperties().get("schnell", 1.5f, Float.class);
        task = new Timer.Task() {
            @Override
            public void run() {
                if (monsterCombatGame.moveableLayer.getCell(x, y) != cell) {
                    this.cancel();
                }

                TileMapNode node = null;

                if (attack != null) {
                    node = findMoveToPlayer();
                }

                if (node != null) {
                    moveCharacter(node.x - x, node.y - y);
                } else {
                    if (monsterCombatGame.random.nextFloat() > 0.6) {
                        if (monsterCombatGame.random.nextBoolean()) {
                            moveCharacter(monsterCombatGame.random.nextInt(3) - 1, 0);
                        } else {
                            moveCharacter(0, monsterCombatGame.random.nextInt(3) - 1);
                        }
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
            if (this == monsterCombatGame.player) {
                monsterCombatGame.loadGameState(monsterCombatGame.savePoint);
                return;
            }

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
     * @param newX
     * @param newY
     * @return
     */
    protected boolean handleBlockingCell(int dx, int dy, int newX, int newY) {
        TiledMapTileLayer.Cell blockingCell = monsterCombatGame.moveableLayer.getCell(newX, newY);
        if (blockingCell == null) {
            return true;
        }

        if (attack != null && newX == monsterCombatGame.player.getX() && newY == monsterCombatGame.player.getY()) {
            monsterCombatGame.player.hit(attack, null);
        }

        return false;
    }
}
