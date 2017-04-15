package de.enderling.monstercombat;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Timer;
import net.dermetfan.gdx.maps.tiled.TmxMapWriter.Format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA;
import static com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA;

public class MonsterCombatGame extends ApplicationAdapter implements InputProcessor {
    Texture img;
    OrthographicCamera camera;
    TiledMapRenderer tiledMapRenderer;

    TiledMapTileLayer moveableLayer;
    TiledMapTileLayer groundLayer;
    TiledMapTileLayer fireLayer;
    TiledMap tiledMap;

    TiledMapTile waffe;

    List<Character> monsters;
    Character player;
    Timer timer;
    Random random;
    Stage stage;

    Label destroyStonesLabel;
    Label fpsLabel;

    Skin skin;

    int lastPlayerDx = 1;
    int lastPlayerDy = 0;

    public class GameState {
        String fileName;
        int destroyStones;

        public GameState(String fileName, int destroyStones) {
            this.fileName = fileName;
            this.destroyStones = destroyStones;
        }
    }

    GameState savePoint = null;

    @Override
    public void create () {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        stage = new Stage();

        camera = new OrthographicCamera();
        camera.setToOrtho(false,w,h);
        camera.update();

        addObservedKey(Input.Keys.LEFT);
        addObservedKey(Input.Keys.RIGHT);
        addObservedKey(Input.Keys.UP);
        addObservedKey(Input.Keys.DOWN);
        addObservedKey(Input.Keys.SPACE);
        Gdx.input.setInputProcessor(this);

        skin = new Skin();

        // Generate a 1x1 white texture and store it in the skin named "white".
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        // Store the default libgdx font under the name "default".
        skin.add("default", new BitmapFont());

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        skin.add("default", labelStyle);

        destroyStonesLabel = new Label("0", skin);
        destroyStonesLabel.setX(w-20);
        destroyStonesLabel.setY(h-20);

        stage.addActor(destroyStonesLabel);


        fpsLabel = new Label("0 fps", skin);
        stage.addActor(fpsLabel);

        this.timer = new Timer();
        this.random = new Random();

        loadGameState(new GameState("julius.tmx", 0));

        for (TiledMapTile tile : tiledMap.getTileSets().getTileSet("dungeon")) {
            if (tile.getProperties().containsKey(("waffe"))) {
                waffe = tile;
            }
        }


        savePoint();
    }

    private void savePoint() {
        File savePointFile = null;
        FileWriter writer = null;
        try {
            savePointFile = new File("savePoint" + System.currentTimeMillis() + ".tmx");
            writer = new FileWriter(savePointFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            new TmxMapWriterFixed(writer).tmx(tiledMap, Format.CSV);
            savePoint = new GameState(savePointFile.getPath(), destroyStones);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void loadGameState(GameState gameState) {
        timer.clear();

        tiledMap = new TmxMapLoader().load(gameState.fileName);
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        moveableLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Monster");
        groundLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Boden");
        fireLayer = (TiledMapTileLayer) tiledMap.getLayers().get("Feuer");

        player = findPlayer();


        camera.position.set((player.getX())*32, (player.getY()) * 32, 0);

        monsters = findCharacters("monster");

        for (Character monster : monsters) {
            monster.init();
        }

        destroyStones = gameState.destroyStones;
        destroyStonesLabel.setText(Integer.toString(destroyStones));
    }

    private Character findPlayer() {
        return findCharacters("spieler").get(0);
    }

    @Override
    public void render () {
        processKeys();

        fpsLabel.setText(Gdx.graphics.getFramesPerSecond() + " fps");

        stage.act();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();


        ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);

        for (Character monster : monsters) {
            monster.draw(shapeRenderer);
        }

        player.draw(shapeRenderer);

        stage.draw();
    }

    public void addObservedKey(int key) {
        observedKeys.put(key, new ObservedKey(key));
    }


    @Override
    public boolean keyDown(int keycode) {
        if(keycode == Input.Keys.NUM_1)
            tiledMap.getLayers().get(0).setVisible(!tiledMap.getLayers().get(0).isVisible());
        if(keycode == Input.Keys.NUM_2)
            tiledMap.getLayers().get(1).setVisible(!tiledMap.getLayers().get(1).isVisible());
        if(keycode == Input.Keys.NUM_3)
            tiledMap.getLayers().get(2).setVisible(!tiledMap.getLayers().get(2).isVisible());
        if (keycode == Input.Keys.BACKSPACE)
            loadGameState(savePoint);
        return false;
    }


    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    private static class ObservedKey {
        private int key;
        private float pressedFor;
        private boolean pressed;
        private static float repeatFrequency = 0.3f;

        public ObservedKey(int key) {
            this.key = key;
            this.pressed = false;
            this.pressedFor = 0;
        }

        public void observe() {
            if(Gdx.input.isKeyPressed(key)) {
                if (pressed) {
                    pressedFor += Gdx.graphics.getRawDeltaTime();
                } else {
                    pressed = true;
                    pressedFor = 0;
                }
            } else {
                pressed = false;
            }
        }

        public boolean justPressedOrRepeated() {
            if (!pressed) {
                return false;
            }

            if (pressedFor == 0) {
                return true;
            }

            if (pressedFor > repeatFrequency) {
                pressedFor -= repeatFrequency;
                return true;
            }

            return false;
        }
    }

    private Map<Integer, ObservedKey> observedKeys = new HashMap<Integer, ObservedKey>();

    public boolean processKeys() {
        for (ObservedKey observedKey : observedKeys.values()) {
            observedKey.observe();
        }

        if(observedKeys.get(Input.Keys.LEFT).justPressedOrRepeated())
            movePlayer(-1, 0);
        if(observedKeys.get(Input.Keys.RIGHT).justPressedOrRepeated())
            movePlayer(1, 0);
        if(observedKeys.get(Input.Keys.UP).justPressedOrRepeated())
            movePlayer(0, 1);
        if(observedKeys.get(Input.Keys.DOWN).justPressedOrRepeated())
            movePlayer(0, -1);
        if (observedKeys.get(Input.Keys.SPACE).justPressedOrRepeated())
            fire();
        return false;
    }

    private void fire() {
        Fire fire = new Fire(waffe, player.getX(), player.getY(), lastPlayerDx, lastPlayerDy);
        fire.init();
    }

    private void movePlayer(int dx, int dy) {
        lastPlayerDx = dx;
        lastPlayerDy = dy;

        if (player.moveCharacter(dx, dy, true)) {
            camera.translate(32 * dx, 32 * dy);

            TiledMapTileLayer.Cell cell = groundLayer.getCell(player.getX(), player.getY());

            if (cell != null) {
                if (cell.getTile().getProperties().containsKey("speichern")) {
                    Integer gespeichert = cell.getTile().getProperties().get("gespeichert", 0, Integer.class);
                    cell.setTile(tiledMap.getTileSets().getTileSet("dungeon").getTile(gespeichert+1));
                    savePoint();
                }
            }
        }

    }

    int destroyStones = 0;

    @Override
    public boolean keyTyped(char character) {

        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }


    public List<Character> findCharacters(String propertyName) {
        List<Character> characters = new ArrayList<Character>();

        for (int x = 0; x < moveableLayer.getWidth(); x++) {
            for (int y = 0; y < moveableLayer.getHeight(); y++)  {
                TiledMapTileLayer.Cell cell = moveableLayer.getCell(x, y);

                if (cell != null && cell.getTile() != null) {
                    if (cell.getTile().getProperties().containsKey(propertyName)) {
                        characters.add(new Character(this, cell, x, y));
                    }
                }
            }
        }
        return characters;
    }

    private class Fire  {
        private TiledMapTileLayer.Cell cell;
        private int x;
        private int y;
        private int dx;
        private int dy;

        public Fire(TiledMapTile tile, int x, int y, int dx, int dy) {
            cell = new TiledMapTileLayer.Cell();
            cell.setTile(tile);

            fireLayer.setCell(x, y, cell);

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
                    int newX = x+dx;
                    int newY = y+dy;

                    if (handleCollision(newX, newY)) {
                        fireLayer.setCell(x, y, null);
                        this.cancel();
                        return;
                    }

                    fireLayer.setCell(x, y, null);
                    x = newX;
                    y = newY;
                    fireLayer.setCell(x, y, cell);
                }
            };
            timer.scheduleTask(task, 0, 1/geschwindigkeit);
        }

        private boolean handleCollision(int newX, int newY) {
            if (newX < 0 || newX >= moveableLayer.getWidth()) {
                return true;
            }

            if (newY < 0 || newY >= moveableLayer.getHeight()) {
                return true;
            }

            TiledMapTileLayer.Cell targetCell = moveableLayer.getCell(newX, newY);
            if (targetCell != null) {

                Character monster = findMonster(newX, newY);

                if (monster != null) {
                    float stärke = cell.getTile().getProperties().get("stärke", 1f, Float.class);

                    Integer effectIndex = cell.getTile().getProperties().get("effekt", null, Integer.class);
                    TiledMapTile effect = null;

                    if (effectIndex != null) {
                        effect = tiledMap.getTileSets().getTileSet("dungeon").getTile(effectIndex + 1);
                    }

                    monster.hit(stärke, effect);
                }

                return true;
            }


            return false;
        }

    }

    private Character findMonster(int x, int y) {
        for (Character monster : monsters) {
            if (monster.getX() == x && monster.getY() == y) {
                return monster;
            }
        }
        return null;
    }

}