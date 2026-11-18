package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.game.MatchPhase;

/**
 * Lista de controles. Se abre desde el menú principal o desde la pausa, y al
 * volver regresa a la pantalla desde la que se abrió.
 */
public class ControlsScreen extends BaseMenuScreen {

    private final Screen previousScreen;
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
        super(game, 1.6f);
        this.previousScreen = previousScreen;
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
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
                if (button == Input.Buttons.LEFT && clicked(backButton, screenX, screenY)) {
                    goBack();
                    return true;
                }
                return false;
            }
        };
    }

    /** Vuelve a la pantalla anterior (que sigue viva) y libera esta. */
    private void goBack() {
        game.replaceScreen(previousScreen);
    }

    @Override
    protected boolean update(float delta) {
        // Si se abrió durante una partida en red, se sigue vigilando la
        // conexión: si se cae o termina la partida, se sale de acá.
        if (game.getNetwork() != null && game.followServer(MatchPhase.PLAYING)) {
            previousScreen.dispose(); // la pausa (y con ella la partida) ya no se usa
            return true;
        }
        return false;
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        shapes.setColor(0.12f, 0.14f, 0.2f, 1f);
        shapes.rect(50, 55, 380, 165);
        drawButton(backButton, mouse, 0.25f, 0.5f, 0.85f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        titleFont.setColor(Color.WHITE);
        drawCentered(titleFont, "Controles", WIDTH / 2f, 250);

        float y = 205;
        for (String[] row : CONTROLS) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, row[0], 70, y);
            font.setColor(Color.WHITE);
            font.draw(batch, row[1], 265, y);
            y -= 21;
        }
        drawButtonLabel(backButton, "Volver");
    }
}
