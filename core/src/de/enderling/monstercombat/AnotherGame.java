package de.enderling.monstercombat;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.*;

public class AnotherGame extends ApplicationAdapter implements InputProcessor {

    OrthographicCamera camera;

    public static class Kreis {
        int kreismittelpunktX = 0;
        int kreismittelpunktY = 0;
        int zielpunktX = 0;
        int zielpunktY = 0;
        Color color = Color.YELLOW;
    }

    Kreis kreis1;
    Kreis kreis2;
    Kreis kreis3;
    Kreis spieler;


    public Kreis createKreis() {
        Kreis kreis = new Kreis();
        kreis.kreismittelpunktX = Gdx.graphics.getWidth() / 2;
        kreis.kreismittelpunktY = Gdx.graphics.getHeight()/2;
        Random random = new Random();
        kreis.zielpunktX = random.nextInt(Gdx.graphics.getWidth());
        kreis.zielpunktY = random.nextInt(Gdx.graphics.getHeight());
        return kreis;
    }

    @Override
    public void create () {
        Gdx.input.setInputProcessor(this);

        kreis1 = createKreis();
        kreis2 = createKreis();
        kreis3 = createKreis();
        spieler = createKreis();
    }

    @Override
    public void render () {


        Gdx.gl.glClearColor(Color.BLACK.r, Color.BLACK.g, Color.BLACK.b, Color.BLACK.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        camera = new OrthographicCamera();
        camera.setToOrtho(true,w,h);

        camera.update();



        ShapeRenderer shapeRenderer = new ShapeRenderer();

        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);


        //shapeRenderer.line(0, 0, 100, 100);
        //shapeRenderer.rect(10, 10, 50, 50);

        Random random = new Random();
        //int zufallszahlX = random.nextInt(1 * 2 + 1) - 1;
        //int zufallszahlY = random.nextInt(1 * 2 + 1) - 1;
        //kreismittelpunktX = kreismittelpunktX + zufallszahlX;
        //kreismittelpunktY = kreismittelpunktY + zufallszahlY;

        //if (kreismittelpunktX < 10) {
        //    kreismittelpunktX = 10;
        //}

        //if (kreismittelpunktY < 10) {
        //    kreismittelpunktY =10;
        //}

        renderKreis(shapeRenderer, random, kreis1);
        renderKreis(shapeRenderer, random, kreis2);
        renderKreis(shapeRenderer, random, kreis3);
        spieler.color = Color.RED;
        shapeRenderer.setColor(spieler.color);
        shapeRenderer.circle(spieler.kreismittelpunktX, spieler.kreismittelpunktY, 10);
        shapeRenderer.end();
    }

    private void renderKreis(ShapeRenderer shapeRenderer, Random random, Kreis kreis) {
        kreis.kreismittelpunktX = kreis.kreismittelpunktX + Math.max(-1, Math.min(1, kreis.zielpunktX - kreis.kreismittelpunktX));
        kreis.kreismittelpunktY = kreis.kreismittelpunktY + Math.max(-1, Math.min(1, kreis.zielpunktY - kreis.kreismittelpunktY));

        if (kreis.kreismittelpunktX == kreis.zielpunktX && kreis.kreismittelpunktY == kreis.zielpunktY) {
            kreis.zielpunktX = random.nextInt(Gdx.graphics.getWidth());
            kreis.zielpunktY = random.nextInt(Gdx.graphics.getHeight());
            kreis.color = new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), 0);
        }

        shapeRenderer.setColor(kreis.color);
        shapeRenderer.circle(kreis.kreismittelpunktX, kreis.kreismittelpunktY, 10);
    }

    @Override
    public boolean keyDown(int keycode) {
        System.out.println(keycode);

        if (keycode == Input.Keys.DOWN) {
            spieler.kreismittelpunktY = spieler.kreismittelpunktY +10;
        }
        if (keycode == Input.Keys.UP) {
            spieler.kreismittelpunktY = spieler.kreismittelpunktY -10;
        }
        if (keycode == Input.Keys.RIGHT) {
            spieler.kreismittelpunktX = spieler.kreismittelpunktX +10;
        }
        if (keycode == Input.Keys.LEFT) {
            spieler.kreismittelpunktX = spieler.kreismittelpunktX -10;
        }
        return false;
    }


    @Override
    public boolean keyUp(int keycode) {
        return false;
    }



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
}