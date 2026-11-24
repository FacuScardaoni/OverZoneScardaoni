package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.ui.UiKit;
import com.OverZone.Scardaoni.shared.MatchPhase;

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
        {"Disparar",             "Click izquierdo  /  botón rojo"},
        {"Habilidad especial",   "Click derecho  /  Espacio  /  botón amarillo"},
        {"Pausa",                "ESC"},
        {"Silenciar sonido",     "M"},
        {"Subir / bajar volumen","+  /  -"},
        {"Elegir personaje (sala)", "1 Volt  2 Brutus  3 Iris  4 Phantom"},
        {"Cambiar modo (sala)",  "M  (solo el anfitrión)"}
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
        drawPanel(50, 52, 380, 178, UiKit.CYAN);
        drawButton(backButton, mouse, 0.25f, 0.5f, 0.85f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        drawTitle("Controles", WIDTH / 2f, 256, Color.WHITE);

        float y = 216;
        for (String[] row : CONTROLS) {
            smallFont.setColor(UiKit.CYAN);
            smallFont.draw(batch, row[0].toUpperCase(), 66, y);
            smallFont.setColor(Color.WHITE);
            smallFont.draw(batch, row[1], 200, y);
            y -= 16;
        }
        smallFont.setColor(Color.LIGHT_GRAY);
        drawCentered(smallFont, "Botones del HUD: arrastrá para apuntar y soltá para disparar.", WIDTH / 2f, 64);
        drawButtonLabel(backButton, "Volver");
    }
}
