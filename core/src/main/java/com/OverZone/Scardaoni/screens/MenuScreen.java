package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.ui.UiDraw;
import com.OverZone.Scardaoni.ui.UiKit;

/**
 * Menú principal: logo, los dos personajes a los costados y las opciones
 * Jugar en red, Controles y Salir. Se puede usar con el mouse o con las
 * flechas y ENTER.
 */
public class MenuScreen extends BaseMenuScreen {

    private final Rectangle playButton     = new Rectangle(160, 118, 160, 32);
    private final Rectangle controlsButton = new Rectangle(172, 80, 136, 26);
    private final Rectangle exitButton     = new Rectangle(172, 48, 136, 26);
    private final Rectangle[] buttons = {playButton, controlsButton, exitButton};
    /** Botón elegido con el teclado (0 = Jugar). */
    private int selected = 0;

    private final Texture voltSheet = new Texture(Gdx.files.internal("volt_walk.png"));
    private final Texture brutusSheet = new Texture(Gdx.files.internal("brutus_walk.png"));
    private final TextureRegion volt = new TextureRegion(voltSheet, 0, 64, 32, 32);     // mirando a la derecha
    private final TextureRegion brutus = new TextureRegion(brutusSheet, 0, 32, 32, 32); // mirando a la izquierda
    private final Texture irisSheet = new Texture(Gdx.files.internal("iris_walk.png"));
    private final Texture phantomSheet = new Texture(Gdx.files.internal("phantom_walk.png"));
    private final TextureRegion iris = new TextureRegion(irisSheet, 0, 64, 32, 32);
    private final TextureRegion phantom = new TextureRegion(phantomSheet, 0, 32, 32, 32);

    public MenuScreen(Main game) {
        super(game, 2.2f);
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.UP:
                    case Input.Keys.W:
                        selected = (selected + buttons.length - 1) % buttons.length;
                        return true;
                    case Input.Keys.DOWN:
                    case Input.Keys.S:
                        selected = (selected + 1) % buttons.length;
                        return true;
                    case Input.Keys.ENTER:
                    case Input.Keys.SPACE:
                        activate(selected);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                for (int i = 0; i < buttons.length; i++) {
                    if (clicked(buttons[i], screenX, screenY)) selected = i;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                for (int i = 0; i < buttons.length; i++) {
                    if (clicked(buttons[i], screenX, screenY)) {
                        activate(i);
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private void activate(int index) {
        switch (index) {
            case 0:
                game.replaceScreen(new ConnectScreen(game));
                break;
            case 1:
                // setScreen (y no replaceScreen) porque se vuelve a este menú.
                game.setScreen(new ControlsScreen(game, this));
                break;
            default:
                Gdx.app.exit();
        }
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        drawButton(playButton, mouse, 1f, 0.45f, 0.08f, selected == 0);
        drawButton(controlsButton, mouse, 0.13f, 0.88f, 1f, selected == 1);
        drawButton(exitButton, mouse, 1f, 0.25f, 0.3f, selected == 2);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        // Personajes a los costados, sobre una luz de color, flotando suave
        float bobV = MathUtils.sin(time * 2.4f) * 2f;
        float bobB = MathUtils.sin(time * 2.0f + 1.3f) * 1.5f;
        UiDraw.glow(batch, ui.glow, 70, 70, 150, UiKit.CYAN, 0.45f);
        UiDraw.glow(batch, ui.glow, 410, 70, 150, UiKit.ORANGE, 0.45f);
        // Iris y Phantom un poco más atrás (más chicos y oscuros)
        batch.setColor(0.7f, 0.7f, 0.75f, 1f);
        batch.draw(iris, 70, 58 - bobB, 64, 64);
        batch.draw(phantom, 346, 58 - bobV, 64, 64);
        batch.setColor(Color.WHITE);
        batch.draw(volt, 22, 28 + bobV, 96, 96);
        batch.draw(brutus, 362, 28 + bobB, 96, 96);
        smallFont.setColor(UiKit.CYAN);
        drawCentered(smallFont, "VOLT · ASALTO", 70, 26);
        smallFont.setColor(UiKit.ORANGE);
        drawCentered(smallFont, "BRUTUS · TANQUE", 410, 26);

        // Logo con un leve latido
        float pulse = 1f + 0.012f * MathUtils.sin(time * 3f);
        float logoW = 310 * pulse, logoH = logoW * ui.logo.getHeight() / ui.logo.getWidth();
        batch.draw(ui.logo, WIDTH / 2f - logoW / 2f, 172 - (logoH - 73) / 2f, logoW, logoH);

        smallFont.setColor(0.75f, 0.85f, 1f, 1f);
        drawCentered(smallFont, "ARENA MULTIJUGADOR  ·  RED LOCAL UDP", WIDTH / 2f, 172);

        drawButtonLabel(playButton, "Jugar en red");
        drawButtonLabel(controlsButton, "Controles");
        drawButtonLabel(exitButton, "Salir");

        smallFont.setColor(0.7f, 0.75f, 0.85f, 1f);
        drawCentered(smallFont, "Flechas para elegir  ·  ENTER para confirmar", WIDTH / 2f, 16);
    }

    @Override
    public void dispose() {
        super.dispose();
        voltSheet.dispose();
        brutusSheet.dispose();
        irisSheet.dispose();
        phantomSheet.dispose();
    }
}
