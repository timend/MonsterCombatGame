package de.enderling.monstercombat;

import box2dLight.*;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Timer;
import net.dermetfan.gdx.maps.tiled.TmxMapWriter.Format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.badlogic.gdx.graphics.GL20.*;

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
    private RayHandler rayHandler;

    public class GameState {
        String fileName;
        int destroyStones;

        public GameState(String fileName, int destroyStones) {
            this.fileName = fileName;
            this.destroyStones = destroyStones;
        }
    }

    public GameState savePoint = null;
    World world;

    @Override
    public void create () {
        Box2D.init();


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

        player.setLifePoints(player.getMaximumLifePoints());
    }

    public void loadGameState(GameState gameState) {
        timer.clear();
        world = new World(new Vector2(0, 0), true);
        rayHandler = new RayHandler(world);

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

        for (int x = 0; x < moveableLayer.getWidth(); x++) {
            for (int y = 0; y < moveableLayer.getHeight(); y++)  {
                TiledMapTileLayer.Cell cell = moveableLayer.getCell(x, y);
                if (cell == null || cell.getTile() == null) {
                    continue;
                }

                if (cell.getTile().getProperties().getKeys().hasNext()) {
                    continue;
                }

                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;
                bodyDef.position.set(x*32+16, y*32+16);
                Body body = world.createBody(bodyDef);
                PolygonShape polygonShape = new PolygonShape();
                polygonShape.setAsBox(16f, 16f);
                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = polygonShape;
                body.createFixture(fixtureDef);
                polygonShape.dispose();


            }
        }

        //rayHandler.setAmbientLight(0.5f);
        //rayHandler.setShadows(false);
        //new DirectionalLight(rayHandler, 200, new Color(1,1,1,1f), (float) Math.toRadians(45f));

        rayHandler.setBlur(true);
        rayHandler.setBlurNum(2);

        rayHandler.simpleBlendFunc.set( GL_BLEND_SRC_ALPHA,  GL_BLEND_DST_ALPHA);
        TiledMapTileLayer lightLayer = (TiledMapTileLayer)tiledMap.getLayers().get("Licht");

        for (int x = 0; x < lightLayer.getWidth(); x++) {
            for (int y = 0; y < lightLayer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = lightLayer.getCell(x, y);
                if (cell == null || cell.getTile() == null) {
                    continue;
                }

                new PointLight(rayHandler, 100, new Color(1, 1, 1, .7f), 32*14, x*32+16, y*32+16);
            }
        }
    }

    private Character findPlayer() {
        return findCharacters("spieler").get(0);
    }

    @Override
    public void render () {
        processKeys();

        if (player.getLifePoints() <= 0) {
            loadGameState(savePoint);
        }

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


        //Gdx.gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //Gdx.gl.glDisable(GL_BLEND);

        rayHandler.setCombinedMatrix(camera);
        //rayHandler.updateAndRender();

        stage.draw();

//        Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();
//        debugRenderer.render(world, camera.combined);


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
        Fire fire = new Fire(this, waffe, player.getX(), player.getY(), lastPlayerDx, lastPlayerDy);
        fire.init();
    }

    private void movePlayer(int dx, int dy) {
        lastPlayerDx = dx;
        lastPlayerDy = dy;

        if (player.moveCharacter(dx, dy)) {
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

                        if (propertyName.equals("spieler")) {
                            characters.add(new Player(this, cell, x, y));
                        } else {
                            characters.add(new Character(this, cell, x, y));
                        }


                    }
                }
            }
        }
        return characters;
    }

    public Character findMonster(int x, int y) {
        for (Character monster : monsters) {
            if (monster.getX() == x && monster.getY() == y) {
                return monster;
            }
        }
        return null;
    }

}