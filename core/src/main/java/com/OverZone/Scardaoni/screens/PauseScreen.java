package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.audio.AudioManager;
import com.OverZone.Scardaoni.shared.MatchPhase;

/**
 * Menú de pausa durante una partida en red.
 *
 * En un juego en red la partida NO se detiene: sigue corriendo en el
 * servidor. Mientras este menú está abierto, el cliente deja de mandar
 * entradas y el servidor detiene al personaje. La conexión sigue activa.
 */
public class PauseScreen extends BaseMenuScreen {

    private final GameScreen gameScreen;
    private final AudioManager audio;
    /** true si se volvió a la partida: en ese caso no hay que liberarla. */
    private boolean resumed = false;

    private final Rectangle resumeButton     = new Rectangle(170, 160, 140, 30);
    private final Rectangle controlsButton   = new Rectangle(170, 122, 140, 30);
    private final Rectangle disconnectButton = new Rectangle(170,  84, 140, 30);
    private final Rectangle muteButton       = new Rectangle(150,  10, 72, 20);

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
        super(game, 1.8f);
        this.gameScreen = gameScreen;
        this.audio = game.getAudioManager();
        background.set(0.04f, 0.05f, 0.08f, 1f);
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
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
                Vector3 world = toWorld(screenX, screenY);

                if (sliderHitBox.contains(world.x, world.y)) {
                    draggingVolume = true;
                    updateVolumeFromX(world.x);
                } else if (muteButton.contains(world.x, world.y)) {
                    audio.MutearDesmutear();
                } else if (resumeButton.contains(world.x, world.y)) {
                    resumeGame();
                } else if (controlsButton.contains(world.x, world.y)) {
                    game.setScreen(new ControlsScreen(game, PauseScreen.this));
                } else if (disconnectButton.contains(world.x, world.y)) {
                    game.disconnect();
                    game.replaceScreen(new MenuScreen(game));
                } else {
                    return false;
                }
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (!draggingVolume) return false;
                updateVolumeFromX(toWorld(screenX, screenY).x);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                draggingVolume = false;
                return false;
            }
        };
    }

    /** Convierte una posición horizontal en un volumen de 0 a 1. */
    private void updateVolumeFromX(float worldX) {
        float ratio = (worldX - SLIDER_X) / SLIDER_WIDTH;
        audio.setVolumeXBarra(MathUtils.clamp(ratio, 0f, 1f));
    }

    private void resumeGame() {
        resumed = true;
        game.replaceScreen(gameScreen); // libera esta pantalla, no la partida
        gameScreen.resumeGame();
    }

    @Override
    protected boolean update(float delta) {
        // Si la partida termina o se pierde la conexión con la pausa abierta,
        // se sale de acá (y se libera la partida).
        return game.followServer(MatchPhase.PLAYING);
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        float volume = audio.getVolume();
        boolean muted = audio.isMuted();
        float shownVolume = muted ? 0f : volume;

        drawButton(resumeButton, mouse, 0.20f, 0.50f, 0.85f);
        drawButton(controlsButton, mouse, 0.25f, 0.40f, 0.55f);
        drawButton(disconnectButton, mouse, 0.45f, 0.30f, 0.30f);
        drawButton(muteButton, mouse, muted ? 0.45f : 0.25f, 0.25f, 0.30f);

        shapes.setColor(0.18f, 0.18f, 0.22f, 1f);
        shapes.rect(SLIDER_X, SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT);
        if (muted) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
        else shapes.setColor(0.30f, 0.65f, 0.95f, 1f);
        shapes.rect(SLIDER_X, SLIDER_Y, SLIDER_WIDTH * shownVolume, SLIDER_HEIGHT);

        boolean sliderActive = draggingVolume || sliderHitBox.contains(mouse.x, mouse.y);
        shapes.setColor(sliderActive ? 1f : 0.85f, sliderActive ? 1f : 0.85f, 0.9f, 1f);
        shapes.circle(SLIDER_X + SLIDER_WIDTH * shownVolume, SLIDER_Y + SLIDER_HEIGHT / 2f, KNOB_RADIUS);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        float volume = audio.getVolume();
        boolean muted = audio.isMuted();

        titleFont.setColor(Color.WHITE);
        drawCentered(titleFont, "PAUSA", WIDTH / 2f, 245);
        font.setColor(Color.ORANGE);
        drawCentered(font, "La partida sigue en curso: tu personaje esta quieto", WIDTH / 2f, 210);

        drawButtonLabel(resumeButton, "Reanudar");
        drawButtonLabel(controlsButton, "Controles");
        drawButtonLabel(disconnectButton, "Desconectarse");

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Volumen", 62, SLIDER_Y + 11);
        font.setColor(Color.WHITE);
        font.draw(batch, muted ? "OFF" : (int) (volume * 100) + "%", 360, SLIDER_Y + 11);
        drawButtonLabel(muteButton, muted ? "Activar" : "Silenciar");
    }

    @Override
    public void dispose() {
        super.dispose();
        // Si no se volvió a la partida, la partida tampoco se va a usar más.
        if (!resumed) gameScreen.dispose();
    }
}
