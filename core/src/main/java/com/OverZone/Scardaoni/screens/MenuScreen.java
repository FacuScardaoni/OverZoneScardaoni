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

public class MenuScreen implements Screen {

    private final Main game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();

    private final Rectangle playButton     = new Rectangle(190, 140, 100, 30);
    private final Rectangle controlsButton = new Rectangle(190, 100, 100, 30);
    private final Rectangle exitButton     = new Rectangle(190,  60, 100, 30);

    public MenuScreen(Main game) {
        this.game = game;
        viewport = new FitViewport(480, 270, camera);
        camera.position.set(240, 135, 0);
        titleFont.getData().setScale(2.2f);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                // Atajo de teclado: ENTER inicia la partida directamente.
                if (keycode == Input.Keys.ENTER) {
                    game.setScreen(new GameScreen(game));
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                if (playButton.contains(world.x, world.y)) {
                    game.setScreen(new GameScreen(game));
                    return true;
                }
                if (controlsButton.contains(world.x, world.y)) {
                    game.setScreen(new ControlsScreen(game, MenuScreen.this));
                    return true;
                }
                if (exitButton.contains(world.x, world.y)) {
                    Gdx.app.exit();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        Vector3 mouseWorld = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawButton(playButton, mouseWorld, 0.20f, 0.50f, 0.85f);
        drawButton(controlsButton, mouseWorld, 0.25f, 0.40f, 0.55f);
        drawButton(exitButton, mouseWorld, 0.50f, 0.20f, 0.20f);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "OverZone", 155, 240);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Prototipo jugable - Segunda pre-entrega", 130, 205);

        font.setColor(Color.WHITE);
        font.draw(batch, "Jugar", playButton.x + 34, playButton.y + 20);
        font.draw(batch, "Controles", controlsButton.x + 22, controlsButton.y + 20);
        font.draw(batch, "Salir", exitButton.x + 36, exitButton.y + 20);

        font.setColor(Color.GRAY);
        font.draw(batch, "ENTER para jugar", 190, 40);
        batch.end();
    }

    private void drawButton(Rectangle rect, Vector3 mouseWorld, float r, float g, float b) {
        boolean hover = rect.contains(mouseWorld.x, mouseWorld.y);
        float boost = hover ? 0.15f : 0f;
        shapes.setColor(r + boost, g + boost, b + boost, 1f);
        shapes.rect(rect.x, rect.y, rect.width, rect.height);
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
