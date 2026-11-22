package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.shared.CharacterType;
import com.OverZone.Scardaoni.shared.GameConstants;
import com.OverZone.Scardaoni.shared.MatchPhase;
import com.OverZone.Scardaoni.shared.Team;
import com.OverZone.Scardaoni.shared.WorldSnapshot;

/**
 * Sala de espera. Muestra los jugadores conectados por equipo, permite elegir
 * personaje (Volt o Brutus) y marcar "listo". Cuando todos están listos
 * (mínimo 2), el servidor inicia la cuenta regresiva y luego la partida.
 *
 * La elección de personaje se envía al servidor en cada PING; el servidor es
 * quien la aplica y la informa de vuelta en el estado.
 */
public class LobbyScreen extends BaseMenuScreen {

    private final Rectangle readyButton = new Rectangle(110, 8, 120, 26);
    private final Rectangle leaveButton = new Rectangle(250, 8, 120, 26);

    /** Tarjetas de elección de personaje, en el mismo orden que CharacterType. */
    private final Rectangle[] characterCards = {
        new Rectangle(20, 58, 210, 94),
        new Rectangle(250, 58, 210, 94)
    };

    private final Texture voltSheet = new Texture(Gdx.files.internal("volt_walk.png"));
    private final Texture brutusSheet = new Texture(Gdx.files.internal("brutus_walk.png"));
    /** Primer cuadro (mirando hacia abajo) de cada personaje, como retrato. */
    private final TextureRegion[] portraits = {
        new TextureRegion(voltSheet, 0, 0, 32, 32),
        new TextureRegion(brutusSheet, 0, 0, 32, 32)
    };

    public LobbyScreen(Main game) {
        super(game, 1.4f);
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
                switch (keycode) {
                    case Input.Keys.ENTER:
                    case Input.Keys.SPACE:
                        toggleReady();
                        return true;
                    case Input.Keys.ESCAPE:
                        leave();
                        return true;
                    case Input.Keys.NUM_1:
                    case Input.Keys.LEFT:
                        choose(CharacterType.VOLT);
                        return true;
                    case Input.Keys.NUM_2:
                    case Input.Keys.RIGHT:
                        choose(CharacterType.BRUTUS);
                        return true;
                    default:
                        return false;
                }
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
                for (CharacterType type : CharacterType.values()) {
                    if (clicked(characterCards[type.id()], screenX, screenY)) {
                        choose(type);
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private void toggleReady() {
        ClientNetwork net = game.getNetwork();
        if (net != null) net.setReady(!net.isReady());
    }

    private void choose(CharacterType type) {
        ClientNetwork net = game.getNetwork();
        if (net != null) net.setCharacter(type);
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
        CharacterType selected = net == null ? CharacterType.VOLT : net.getCharacter();

        // Paneles de los equipos
        shapes.setColor(0.10f, 0.16f, 0.30f, 1f);
        shapes.rect(20, 160, 210, 66);
        shapes.setColor(0.30f, 0.10f, 0.12f, 1f);
        shapes.rect(250, 160, 210, 66);

        // Tarjetas de personaje: la elegida tiene un borde brillante
        for (CharacterType type : CharacterType.values()) {
            Rectangle card = characterCards[type.id()];
            boolean isSelected = type == selected;
            if (isSelected) {
                shapes.setColor(type == CharacterType.VOLT ? 0.15f : 1f, type == CharacterType.VOLT ? 0.85f : 0.55f,
                    type == CharacterType.VOLT ? 1f : 0.12f, 1f);
                shapes.rect(card.x - 2, card.y - 2, card.width + 4, card.height + 4);
            }
            drawButton(card, mouse, 0.11f, 0.13f, 0.18f);
            // Fondo del retrato
            shapes.setColor(0.16f, 0.19f, 0.25f, 1f);
            shapes.rect(card.x + 6, card.y + 14, 68, 68);
        }

        if (ready) drawButton(readyButton, mouse, 0.20f, 0.55f, 0.30f);
        else drawButton(readyButton, mouse, 0.20f, 0.50f, 0.85f);
        drawButton(leaveButton, mouse, 0.45f, 0.30f, 0.30f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        WorldSnapshot snap = net == null ? null : net.getLatestSnapshot();

        titleFont.setColor(Color.WHITE);
        drawCentered(titleFont, "Sala de espera", WIDTH / 2f, 262);
        font.setColor(Color.GRAY);
        if (net != null) {
            drawCentered(font, "Servidor: " + net.getServerName() + " (" + net.getServerAddress() + ")"
                + pingText(net), WIDTH / 2f, 242);
        }

        // Equipos
        font.setColor(Color.SKY);
        font.draw(batch, "Equipo Azul", 28, 222);
        font.setColor(Color.SALMON);
        font.draw(batch, "Equipo Rojo", 258, 222);
        if (snap != null) {
            font.getData().setScale(0.8f); // letra más chica para que entre todo en el panel
            float yBlue = 204, yRed = 204;
            for (WorldSnapshot.PlayerState p : snap.players) {
                boolean blue = p.team == Team.BLUE;
                float x = blue ? 28 : 258;
                float y = blue ? yBlue : yRed;
                boolean isMe = p.id == net.getPlayerId();
                font.setColor(isMe ? Color.YELLOW : Color.WHITE);
                font.draw(batch, p.name + (isMe ? " (vos)" : ""), x, y);
                font.setColor(Color.LIGHT_GRAY);
                font.draw(batch, CharacterType.fromId(p.character).displayName, x + 104, y);
                font.setColor(p.ready ? Color.GREEN : Color.GRAY);
                font.draw(batch, p.ready ? "LISTO" : "...", x + 160, y);
                if (blue) yBlue -= 18; else yRed -= 18;
            }
            font.getData().setScale(1f);
        }

        // Tarjetas de personaje
        for (CharacterType type : CharacterType.values()) {
            Rectangle card = characterCards[type.id()];
            batch.draw(portraits[type.id()], card.x + 8, card.y + 16, 64, 64);
            float tx = card.x + 82;
            font.setColor(Color.WHITE);
            font.draw(batch, (type.id() + 1) + " - " + type.displayName, tx, card.y + card.height - 8);
            font.getData().setScale(0.8f);
            font.setColor(Color.GRAY);
            font.draw(batch, type.role + " / " + type.faction, tx, card.y + card.height - 26);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Vida " + (int) type.maxHealth + "  Vel. " + (int) type.speed, tx, card.y + 50);
            font.draw(batch, "Dano " + (int) type.damage + "  Cad. " + type.fireCooldown + "s", tx, card.y + 36);
            font.setColor(type == CharacterType.VOLT ? Color.CYAN : Color.ORANGE);
            font.draw(batch, type.abilityName, tx, card.y + 20);
            font.getData().setScale(1f);
        }

        font.setColor(Color.WHITE);
        if (snap != null) drawCentered(font, statusText(snap), WIDTH / 2f, 52);

        boolean ready = net != null && net.isReady();
        drawButtonLabel(readyButton, ready ? "No estoy listo" : "Listo (ENTER)");
        drawButtonLabel(leaveButton, "Salir (ESC)");
    }

    private String statusText(WorldSnapshot snap) {
        if (snap.phase == MatchPhase.COUNTDOWN) {
            return "La partida comienza en " + (int) Math.ceil(snap.phaseTimer) + "...";
        }
        if (snap.players.size() < GameConstants.MIN_PLAYERS) {
            return "Esperando jugadores (minimo " + GameConstants.MIN_PLAYERS + ")";
        }
        return "Elegi tu personaje (1 / 2) y marca Listo";
    }

    static String pingText(ClientNetwork net) {
        float ping = net.getPingMs();
        return ping < 0 ? "" : "  -  " + Math.round(ping) + " ms";
    }

    @Override
    public void dispose() {
        super.dispose();
        voltSheet.dispose();
        brutusSheet.dispose();
    }
}
