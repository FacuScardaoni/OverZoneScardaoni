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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.audio.AudioManager;


public class PauseScreen implements Screen {

    private final Main game;
    private final GameScreen gameScreen;
    private final AudioManager audio;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont titleFont = new BitmapFont();
    private final BitmapFont font = new BitmapFont();

    private final Rectangle resumeButton   = new Rectangle(190, 160, 100, 30);
    private final Rectangle controlsButton = new Rectangle(190, 122, 100, 30);
    private final Rectangle menuButton     = new Rectangle(190,  84, 100, 30);
    private final Rectangle muteButton     = new Rectangle(150,  10, 72, 20);

    // --- Barra de volumen ---
    private static final float SLIDER_X = 150f;
    private static final float SLIDER_Y = 42f;
    private static final float SLIDER_WIDTH = 200f;
    private static final float SLIDER_HEIGHT = 6f;
    private static final float KNOB_RADIUS = 7f;
    private final Rectangle sliderHitBox = new Rectangle(
        SLIDER_X - KNOB_RADIUS, SLIDER_Y - 11, SLIDER_WIDTH + KNOB_RADIUS * 2, 28);

    private boolean draggingVolume = false;

    public PauseScreen(Main game, GameScreen gameScreen) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.audio = game.getAudioManager();
        viewport = new FitViewport(480, 270, camera);
        camera.position.set(240, 135, 0);
        titleFont.getData().setScale(1.8f);
    }

    @Override
    public void show() {
        gameScreen.pause();
        audio.playMusic();
        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean keyUp(int keycode) {
                switch (keycode) {
                    case Input.Keys.ESCAPE: resumeGame(); return true;
                    case Input.Keys.M: audio.MutearDesmutear(); return true;
                    case Input.Keys.PLUS:
                    case Input.Keys.EQUALS: audio.SubirVolumen(); return true;
                    case Input.Keys.MINUS: audio.BajarVolumen(); return true;
                    default: return false;
                }
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));

                if (sliderHitBox.contains(world.x, world.y)) {
                    draggingVolume = true;
                    updateVolumeFromX(world.x);
                    return true;
                }
                if (muteButton.contains(world.x, world.y)) {
                    audio.MutearDesmutear();
                    return true;
                }
                if (resumeButton.contains(world.x, world.y)) {
                    resumeGame();
                    return true;
                }
                if (controlsButton.contains(world.x, world.y)) {
                    game.setScreen(new ControlsScreen(game, PauseScreen.this));
                    return true;
                }
                if (menuButton.contains(world.x, world.y)) {
                    gameScreen.dispose();
                    game.setScreen(new MenuScreen(game));
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!draggingVolume) return false;
                Vector3 world = viewport.unproject(new Vector3(screenX, screenY, 0));
                updateVolumeFromX(world.x);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                draggingVolume = false;
                return false;
            }
        });
    }

    /** Convierte una posición horizontal del mundo en un volumen de 0 a 1. */
    private void updateVolumeFromX(float worldX) {
        float ratio = (worldX - SLIDER_X) / SLIDER_WIDTH;
        audio.setVolumeXBarra(MathUtils.clamp(ratio, 0f, 1f));
    }

    private void resumeGame() {
        game.setScreen(gameScreen);
        gameScreen.resumeGame();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        Vector3 mouseWorld = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        float volume = audio.getVolume();
        boolean muted = audio.isMuted();
        // Si está silenciado, la barra se dibuja vacía aunque el volumen
        // guardado sea mayor, para reflejar lo que realmente se escucha.
        float shownVolume = muted ? 0f : volume;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        drawButton(resumeButton, mouseWorld, 0.20f, 0.50f, 0.85f);
        drawButton(controlsButton, mouseWorld, 0.25f, 0.40f, 0.55f);
        drawButton(menuButton, mouseWorld, 0.45f, 0.30f, 0.30f);
        drawButton(muteButton, mouseWorld, muted ? 0.45f : 0.25f, 0.25f, 0.30f);

        // Riel de la barra
        shapes.setColor(0.18f, 0.18f, 0.22f, 1f);
        shapes.rect(SLIDER_X, SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT);
        // Parte llena
        if (muted) {
            shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
        } else {
            shapes.setColor(0.30f, 0.65f, 0.95f, 1f);
        }
        shapes.rect(SLIDER_X, SLIDER_Y, SLIDER_WIDTH * shownVolume, SLIDER_HEIGHT);

        boolean sliderActive = draggingVolume || sliderHitBox.contains(mouseWorld.x, mouseWorld.y);
        shapes.setColor(sliderActive ? 1f : 0.85f, sliderActive ? 1f : 0.85f, 0.9f, 1f);
        shapes.circle(SLIDER_X + SLIDER_WIDTH * shownVolume, SLIDER_Y + SLIDER_HEIGHT / 2f, KNOB_RADIUS);

        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "PAUSA", 190, 240);

        font.setColor(Color.WHITE);
        font.draw(batch, "Reanudar", resumeButton.x + 24, resumeButton.y + 20);
        font.draw(batch, "Controles", controlsButton.x + 22, controlsButton.y + 20);
        font.draw(batch, "Menu principal", menuButton.x + 5, menuButton.y + 20);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Volumen", 62, SLIDER_Y + 11);
        font.setColor(Color.WHITE);
        font.draw(batch, muted ? "OFF" : (int) (volume * 100) + "%", 360, SLIDER_Y + 11);
        font.draw(batch, muted ? "Activar" : "Silenciar", muteButton.x + 8, muteButton.y + 14);

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
