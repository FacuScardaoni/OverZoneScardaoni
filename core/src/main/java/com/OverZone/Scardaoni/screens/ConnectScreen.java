package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.network.ClientNetwork;

/**
 * Busca el servidor en la red local (broadcast) y se conecta.
 *
 * No hace falta escribir ninguna IP. Si no encuentra servidor, lo informa y
 * permite reintentar o volver al menú.
 */
public class ConnectScreen extends BaseMenuScreen {

    private final Rectangle retryButton = new Rectangle(110, 40, 120, 30);
    private final Rectangle backButton  = new Rectangle(250, 40, 120, 30);
    private float elapsed = 0f;

    public ConnectScreen(Main game) {
        super(game, 1.6f);
    }

    @Override
    public void show() {
        super.show();
        game.connect();
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    cancel();
                    return true;
                }
                if (keycode == Input.Keys.ENTER && hasFailed()) {
                    retry();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                if (hasFailed() && clicked(retryButton, screenX, screenY)) {
                    retry();
                    return true;
                }
                if (clicked(backButton, screenX, screenY)) {
                    cancel();
                    return true;
                }
                return false;
            }
        };
    }

    private boolean hasFailed() {
        ClientNetwork net = game.getNetwork();
        return net == null || net.getStatus() == ClientNetwork.Status.FAILED;
    }

    private void retry() {
        elapsed = 0f;
        game.connect();
    }

    private void cancel() {
        game.disconnect();
        game.replaceScreen(new MenuScreen(game));
    }

    @Override
    protected boolean update(float delta) {
        elapsed += delta;
        ClientNetwork net = game.getNetwork();
        // Ya conectado y con el primer estado recibido: se pasa a la pantalla
        // que corresponda a la etapa de la partida (sala, juego o resultados).
        if (net != null && net.getStatus() == ClientNetwork.Status.CONNECTED
            && net.getLatestSnapshot() != null) {
            return game.followServer();
        }
        return false;
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        if (hasFailed()) drawButton(retryButton, mouse, 0.20f, 0.50f, 0.85f);
        drawButton(backButton, mouse, 0.45f, 0.30f, 0.30f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        drawTitle("Conectando", WIDTH / 2f, 232, Color.WHITE);

        String dots = ".".repeat(1 + (int) (elapsed * 3) % 3);
        font.setColor(Color.LIGHT_GRAY);
        if (net == null) {
            drawCentered(font, "Sin conexion", WIDTH / 2f, 170);
        } else {
            switch (net.getStatus()) {
                case SEARCHING:
                    drawCentered(font, "Buscando servidor en la red local" + dots, WIDTH / 2f, 170);
                    drawCentered(font, "(broadcast UDP al puerto 5555)", WIDTH / 2f, 150);
                    break;
                case CONNECTING:
                case CONNECTED:
                    drawCentered(font, "Servidor encontrado: " + net.getServerName()
                        + " (" + net.getServerAddress() + ")", WIDTH / 2f, 170);
                    drawCentered(font, "Uniendose a la partida" + dots, WIDTH / 2f, 150);
                    break;
                case FAILED:
                    font.setColor(Color.ORANGE);
                    float y = 180;
                    for (String line : net.getFailureMessage().split("\n")) {
                        drawCentered(font, line, WIDTH / 2f, y);
                        y -= 20;
                    }
                    break;
                default:
                    break;
            }
        }

        if (hasFailed()) drawButtonLabel(retryButton, "Reintentar");
        drawButtonLabel(backButton, "Volver");
    }
}
