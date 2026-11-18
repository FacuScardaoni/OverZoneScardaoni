package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;

/**
 * Muestra un aviso (por ejemplo, "se perdió la conexión") con un botón para
 * volver al menú. Así un problema de red nunca cierra ni traba el juego.
 */
public class MessageScreen extends BaseMenuScreen {

    private final String title;
    private final String[] lines;
    private final Rectangle menuButton = new Rectangle(170, 40, 140, 30);

    public MessageScreen(Main game, String title, String message) {
        super(game, 1.6f);
        this.title = title;
        this.lines = message.split("\n");
    }

    @Override
    public void show() {
        super.show();
        game.getAudioManager().pauseMusic();
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.ESCAPE) {
                    backToMenu();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT && clicked(menuButton, screenX, screenY)) {
                    backToMenu();
                    return true;
                }
                return false;
            }
        };
    }

    private void backToMenu() {
        game.replaceScreen(new MenuScreen(game));
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        drawButton(menuButton, mouse, 0.25f, 0.40f, 0.55f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        titleFont.setColor(Color.ORANGE);
        drawCentered(titleFont, title, WIDTH / 2f, 220);
        font.setColor(Color.WHITE);
        float y = 175;
        for (String line : lines) {
            drawCentered(font, line, WIDTH / 2f, y);
            y -= 20;
        }
        drawButtonLabel(menuButton, "Menu principal");
    }
}
