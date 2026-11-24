package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.ui.UiKit;
import com.OverZone.Scardaoni.shared.CharacterType;
import com.OverZone.Scardaoni.shared.GameMode;
import com.OverZone.Scardaoni.shared.MatchPhase;
import com.OverZone.Scardaoni.shared.Team;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.shared.WorldSnapshot;

import java.util.ArrayList;
import java.util.List;

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
        drawPanel(60, 64, 360, 92, UiKit.CYAN);
        drawButton(leaveButton, mouse, 0.30f, 0.30f, 0.35f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        WorldSnapshot snap = net == null ? null : net.getLatestSnapshot();
        if (snap == null) return;

        WorldSnapshot.PlayerState me = snap.findPlayer(net.getPlayerId());
        boolean ffa = snap.getMode() == GameMode.FREE_FOR_ALL;

        // En Equipos, "winner" es el equipo ganador; en Todos contra todos, el id del jugador.
        String title;
        Color color;
        if (snap.winner == Team.NONE) {
            title = "EMPATE";
            color = Color.LIGHT_GRAY;
        } else if (ffa) {
            WorldSnapshot.PlayerState w = snap.findPlayer(snap.winner);
            if (me != null && snap.winner == me.id) {
                title = "¡GANASTE!";
                color = Color.GOLD;
            } else {
                title = "GANÓ " + (w == null ? "?" : w.name.toUpperCase());
                color = Color.SCARLET;
            }
        } else if (me != null && snap.winner == me.team) {
            title = "VICTORIA";
            color = Color.GOLD;
        } else {
            title = "DERROTA";
            color = Color.SCARLET;
        }
        layout.setText(hugeFont, title);
        drawTitle(layout.width > WIDTH - 30 ? titleFont : hugeFont, title, WIDTH / 2f, 258, color);

        font.setColor(Color.LIGHT_GRAY);
        String reason = snap.endReason.equals("PLAYERS")
            ? (ffa ? "No quedaron rivales" : "Un equipo se quedo sin jugadores")
            : "Tiempo agotado";
        drawCentered(font, snap.getMode().displayName + "  -  " + reason, WIDTH / 2f, 206);

        if (!ffa) {
            font.setColor(Color.SKY);
            drawCentered(font, "Azul " + snap.scoreBlue, WIDTH / 2f - 60, 182);
            font.setColor(Color.WHITE);
            drawCentered(font, "-", WIDTH / 2f, 182);
            font.setColor(Color.SALMON);
            drawCentered(font, snap.scoreRed + " Rojo", WIDTH / 2f + 60, 182);
        } else {
            font.setColor(Color.WHITE);
            drawCentered(font, "Ranking por eliminaciones", WIDTH / 2f, 182);
        }

        // Tabla: en Todos contra todos, ordenada por eliminaciones
        List<WorldSnapshot.PlayerState> rows = new ArrayList<>(snap.players);
        if (ffa) {
            rows.sort((x, y) -> x.kills != y.kills ? Integer.compare(y.kills, x.kills)
                : Integer.compare(x.deaths, y.deaths));
        } else {
            rows.sort((x, y) -> Integer.compare(x.team, y.team));
        }
        font.setColor(Color.GRAY);
        font.draw(batch, "Jugador", 70, 145);
        font.draw(batch, "Personaje", 160, 145);
        font.draw(batch, ffa ? "Puesto" : "Equipo", 235, 145);
        font.draw(batch, "Elim.", 300, 145);
        font.draw(batch, "Muertes", 350, 145);
        float y = 128;
        int place = 1;
        for (WorldSnapshot.PlayerState p : rows) {
            font.setColor(p.id == net.getPlayerId() ? Color.YELLOW : Color.WHITE);
            drawFitted(font, p.name, 70, y, 86);
            font.setColor(UiKit.characterColor(CharacterType.fromId(p.character)));
            font.draw(batch, CharacterType.fromId(p.character).displayName, 160, y);
            font.setColor(UiKit.playerColor(snap.getMode(), p.team));
            font.draw(batch, ffa ? "#" + place : Team.name(p.team), 235, y);
            font.setColor(Color.WHITE);
            font.draw(batch, String.valueOf(p.kills), 310, y);
            font.draw(batch, String.valueOf(p.deaths), 370, y);
            y -= 15;
            place++;
        }

        font.setColor(Color.GRAY);
        drawCentered(font, "Volviendo a la sala en " + (int) Math.ceil(snap.phaseTimer) + " s", WIDTH / 2f, 60);
        drawButtonLabel(leaveButton, "Salir al menu");
    }
}
