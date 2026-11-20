package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.shared.MatchPhase;
import com.OverZone.Scardaoni.shared.Team;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.shared.WorldSnapshot;

/**
 * Resultados de la partida. Los datos vienen del servidor. Pasados unos
 * segundos, el servidor vuelve a la sala de espera y esta pantalla la sigue,
 * así se puede jugar otra partida sin cerrar el programa.
 */
public class ResultsScreen extends BaseMenuScreen {

    private final Rectangle leaveButton = new Rectangle(170, 20, 140, 28);

    public ResultsScreen(Main game) {
        super(game, 2f);
    }

    @Override
    public void show() {
        super.show();
        game.getAudioManager().pauseMusic();
        if (game.getNetwork() != null) game.getNetwork().setReady(false);
    }

    @Override
    protected InputProcessor createInputProcessor() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    leave();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT && clicked(leaveButton, screenX, screenY)) {
                    leave();
                    return true;
                }
                return false;
            }
        };
    }

    private void leave() {
        game.disconnect();
        game.replaceScreen(new MenuScreen(game));
    }

    @Override
    protected boolean update(float delta) {
        return game.followServer(MatchPhase.RESULTS);
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        shapes.setColor(0.12f, 0.14f, 0.2f, 1f);
        shapes.rect(60, 62, 360, 88);
        drawButton(leaveButton, mouse, 0.30f, 0.30f, 0.35f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        WorldSnapshot snap = net == null ? null : net.getLatestSnapshot();
        if (snap == null) return;

        WorldSnapshot.PlayerState me = snap.findPlayer(net.getPlayerId());
        int myTeam = me == null ? Team.NONE : me.team;

        String title;
        Color color;
        if (snap.winner == Team.NONE) {
            title = "EMPATE";
            color = Color.LIGHT_GRAY;
        } else if (snap.winner == myTeam) {
            title = "VICTORIA";
            color = Color.GOLD;
        } else {
            title = "DERROTA";
            color = Color.SCARLET;
        }
        titleFont.setColor(color);
        drawCentered(titleFont, title, WIDTH / 2f, 250);

        font.setColor(Color.LIGHT_GRAY);
        drawCentered(font, snap.endReason.equals("PLAYERS")
            ? "La partida termino porque un equipo se quedo sin jugadores"
            : "Tiempo agotado", WIDTH / 2f, 212);

        font.setColor(Color.SKY);
        drawCentered(font, "Azul " + snap.scoreBlue, WIDTH / 2f - 60, 188);
        font.setColor(Color.WHITE);
        drawCentered(font, "-", WIDTH / 2f, 188);
        font.setColor(Color.SALMON);
        drawCentered(font, snap.scoreRed + " Rojo", WIDTH / 2f + 60, 188);

        font.setColor(Color.GRAY);
        font.draw(batch, "Jugador", 75, 143);
        font.draw(batch, "Equipo", 220, 143);
        font.draw(batch, "Elim.", 300, 143);
        font.draw(batch, "Muertes", 350, 143);
        float y = 125;
        for (WorldSnapshot.PlayerState p : snap.players) {
            font.setColor(p.id == net.getPlayerId() ? Color.YELLOW : Color.WHITE);
            font.draw(batch, p.name, 75, y);
            font.draw(batch, Team.name(p.team), 220, y);
            font.draw(batch, String.valueOf(p.kills), 310, y);
            font.draw(batch, String.valueOf(p.deaths), 370, y);
            y -= 16;
        }

        font.setColor(Color.GRAY);
        drawCentered(font, "Volviendo a la sala en " + (int) Math.ceil(snap.phaseTimer) + " s", WIDTH / 2f, 60);
        drawButtonLabel(leaveButton, "Salir al menu");
    }
}
