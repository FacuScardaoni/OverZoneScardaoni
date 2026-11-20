package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.shared.GameConstants;
import com.OverZone.Scardaoni.shared.MatchPhase;
import com.OverZone.Scardaoni.shared.Team;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.shared.WorldSnapshot;

/**
 * Sala de espera. Muestra los jugadores conectados por equipo y si están
 * listos. Cuando todos marcan "listo" (mínimo 2), el servidor inicia la
 * cuenta regresiva y luego la partida.
 */
public class LobbyScreen extends BaseMenuScreen {

    private final Rectangle readyButton = new Rectangle(110, 30, 120, 30);
    private final Rectangle leaveButton = new Rectangle(250, 30, 120, 30);

    public LobbyScreen(Main game) {
        super(game, 1.6f);
    }

    @Override
    public void show() {
        super.show();
        // Al entrar a la sala, nadie está listo hasta que lo marque.
        if (game.getNetwork() != null) game.getNetwork().setReady(false);
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    toggleReady();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    leave();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button != Input.Buttons.LEFT) return false;
                if (clicked(readyButton, screenX, screenY)) {
                    toggleReady();
                    return true;
                }
                if (clicked(leaveButton, screenX, screenY)) {
                    leave();
                    return true;
                }
                return false;
            }
        };
    }

    private void toggleReady() {
        ClientNetwork net = game.getNetwork();
        if (net != null) net.setReady(!net.isReady());
    }

    private void leave() {
        game.disconnect();
        game.replaceScreen(new MenuScreen(game));
    }

    @Override
    protected boolean update(float delta) {
        return game.followServer(MatchPhase.LOBBY, MatchPhase.COUNTDOWN);
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        boolean ready = net != null && net.isReady();
        // Paneles de los equipos
        shapes.setColor(0.10f, 0.16f, 0.30f, 1f);
        shapes.rect(40, 95, 190, 110);
        shapes.setColor(0.30f, 0.10f, 0.12f, 1f);
        shapes.rect(250, 95, 190, 110);

        if (ready) drawButton(readyButton, mouse, 0.20f, 0.55f, 0.30f);
        else drawButton(readyButton, mouse, 0.20f, 0.50f, 0.85f);
        drawButton(leaveButton, mouse, 0.45f, 0.30f, 0.30f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        WorldSnapshot snap = net == null ? null : net.getLatestSnapshot();

        titleFont.setColor(Color.WHITE);
        drawCentered(titleFont, "Sala de espera", WIDTH / 2f, 255);
        font.setColor(Color.GRAY);
        if (net != null) {
            drawCentered(font, "Servidor: " + net.getServerName() + " (" + net.getServerAddress() + ")"
                + pingText(net), WIDTH / 2f, 228);
        }

        font.setColor(Color.SKY);
        font.draw(batch, "Equipo Azul", 50, 198);
        font.setColor(Color.SALMON);
        font.draw(batch, "Equipo Rojo", 260, 198);

        if (snap != null) {
            float yBlue = 175, yRed = 175;
            for (WorldSnapshot.PlayerState p : snap.players) {
                boolean blue = p.team == Team.BLUE;
                float x = blue ? 50 : 260;
                String label = p.name + (p.id == net.getPlayerId() ? " (vos)" : "");
                font.setColor(p.id == net.getPlayerId() ? Color.YELLOW : Color.WHITE);
                font.draw(batch, label, x, blue ? yBlue : yRed);
                font.setColor(p.ready ? Color.GREEN : Color.GRAY);
                font.draw(batch, p.ready ? "LISTO" : "...", x + 130, blue ? yBlue : yRed);
                if (blue) yBlue -= 20; else yRed -= 20;
            }

            font.setColor(Color.WHITE);
            drawCentered(font, statusText(snap), WIDTH / 2f, 82);
        }

        boolean ready = net != null && net.isReady();
        drawButtonLabel(readyButton, ready ? "No estoy listo" : "Listo");
        drawButtonLabel(leaveButton, "Salir");
        font.setColor(Color.GRAY);
        drawCentered(font, "ENTER: listo   ESC: salir", WIDTH / 2f, 18);
    }

    private String statusText(WorldSnapshot snap) {
        if (snap.phase == MatchPhase.COUNTDOWN) {
            return "La partida comienza en " + (int) Math.ceil(snap.phaseTimer) + "...";
        }
        if (snap.players.size() < GameConstants.MIN_PLAYERS) {
            return "Esperando jugadores (minimo " + GameConstants.MIN_PLAYERS + ")";
        }
        return "Esperando que todos esten listos";
    }

    static String pingText(ClientNetwork net) {
        float ping = net.getPingMs();
        return ping < 0 ? "" : "  -  " + Math.round(ping) + " ms";
    }
}
