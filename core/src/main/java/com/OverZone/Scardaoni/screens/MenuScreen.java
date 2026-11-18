package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;

/** Menú principal: jugar en red, ver controles o salir. */
public class MenuScreen extends BaseMenuScreen {

    private final Rectangle playButton     = new Rectangle(170, 140, 140, 30);
    private final Rectangle controlsButton = new Rectangle(170, 100, 140, 30);
    private final Rectangle exitButton     = new Rectangle(170,  60, 140, 30);

    public MenuScreen(Main game) {
        super(game, 2.2f);
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    play();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                if (clicked(playButton, screenX, screenY)) {
                    play();
                } else if (clicked(controlsButton, screenX, screenY)) {
                    // setScreen (y no replaceScreen) porque se vuelve a este menú.
                    game.setScreen(new ControlsScreen(game, MenuScreen.this));
                } else if (clicked(exitButton, screenX, screenY)) {
                    Gdx.app.exit();
                } else {
                    return false;
                }
                return true;
            }
        };
    }

    private void play() {
        game.replaceScreen(new ConnectScreen(game));
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        drawButton(playButton, mouse, 0.20f, 0.50f, 0.85f);
        drawButton(controlsButton, mouse, 0.25f, 0.40f, 0.55f);
        drawButton(exitButton, mouse, 0.50f, 0.20f, 0.20f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        titleFont.setColor(Color.WHITE);
        drawCentered(titleFont, "OverZone", WIDTH / 2f, 240);
        font.setColor(Color.LIGHT_GRAY);
        drawCentered(font, "Multijugador en red local (UDP)", WIDTH / 2f, 205);

        drawButtonLabel(playButton, "Jugar en red");
        drawButtonLabel(controlsButton, "Controles");
        drawButtonLabel(exitButton, "Salir");

        font.setColor(Color.GRAY);
        drawCentered(font, "ENTER para jugar", WIDTH / 2f, 40);
    }
}
