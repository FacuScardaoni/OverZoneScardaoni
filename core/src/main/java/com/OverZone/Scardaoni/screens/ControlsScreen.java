package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.Main;


public class ControlsScreen implements Screen {

    private final Main game;
    private final Screen previousScreen;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();

    private final Rectangle backButton = new Rectangle(190, 22, 100, 26);

    private static final String[][] CONTROLS = {
        {"Moverse",              "W  A  S  D"},
        {"Apuntar",              "Mouse"},
        {"Disparar",             "Click izquierdo"},
        {"Impulso Over (dash)",  "Click derecho / Espacio"},
        {"Pausa",                "ESC"},
        {"Silenciar sonido",     "M"},
        {"Subir / bajar volumen","+  /  -"}
    };

    public ControlsScreen(Main game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        viewport = new FitViewport(480, 270, camera);
        camera.position.set(240, 135, 0);
        titleFont.getData().setScale(1.6f);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    goBack();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                if (backButton.contains(world.x, world.y)) {
                    goBack();
                    return true;
                }
                return false;
            }
        });
    }

    private void goBack() {
        game.setScreen(previousScreen);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        Vector3 mouseWorld = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        boolean hover = backButton.contains(mouseWorld.x, mouseWorld.y);

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Panel de fondo, para separar visualmente la lista del resto
        shapes.setColor(0.12f, 0.14f, 0.2f, 1f);
        shapes.rect(50, 55, 380, 165);
        shapes.setColor(hover ? 0.35f : 0.25f, hover ? 0.6f : 0.5f, hover ? 0.95f : 0.85f, 1f);
        shapes.rect(backButton.x, backButton.y, backButton.width, backButton.height);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "Controles", 175, 250);

        float y = 205;
        for (String[] row : CONTROLS) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, row[0], 70, y);
            font.setColor(Color.WHITE);
            font.draw(batch, row[1], 265, y);
            y -= 21;
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "Volver", backButton.x + 30, backButton.y + 18);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        titleFont.dispose();
        font.dispose();
    }
}
