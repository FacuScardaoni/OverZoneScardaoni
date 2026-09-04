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


public class ResultsScreen implements Screen {

    private final Main game;
    private final boolean victory;
    private final int score;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();

    private final Rectangle menuButton = new Rectangle(190, 90, 100, 30);

    public ResultsScreen(Main game, boolean victory, int score) {
        this.game = game;
        this.victory = victory;
        this.score = score;
        viewport = new FitViewport(480, 270, camera);
        camera.position.set(240, 135, 0);
        titleFont.getData().setScale(2f);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                if (menuButton.contains(world.x, world.y)) {
                    game.setScreen(new MenuScreen(game));
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

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapes.rect(menuButton.x, menuButton.y, menuButton.width, menuButton.height);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        titleFont.setColor(victory ? Color.GOLD : Color.SCARLET);
        titleFont.draw(batch, victory ? "VICTORIA" : "DERROTA", 170, 210);
        font.setColor(Color.WHITE);
        font.draw(batch, "Puntaje final: " + score, 190, 170);
        font.draw(batch, "Menu principal", menuButton.x - 5, menuButton.y + 20);
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
