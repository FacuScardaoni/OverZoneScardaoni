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
import com.OverZone.Scardaoni.ui.UiKit;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.shared.CharacterType;
import com.OverZone.Scardaoni.shared.GameConstants;
import com.OverZone.Scardaoni.shared.GameMode;
import com.OverZone.Scardaoni.shared.MatchPhase;
import com.OverZone.Scardaoni.shared.Protocol;
import com.OverZone.Scardaoni.shared.Team;
import com.OverZone.Scardaoni.shared.WorldSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Sala de espera.
 *
 *  - Arriba a la izquierda: los jugadores conectados (hasta 4), con su
 *    personaje, su color o equipo y si están listos.
 *  - Arriba a la derecha: el modo de juego. Solo el anfitrión (el primero
 *    que se conectó, marcado con una estrella) puede cambiarlo. "Equipos 2v2"
 *    se habilita únicamente con 4 jugadores en la sala.
 *  - Abajo: las cuatro tarjetas de personaje (teclas 1 a 4).
 *
 * El personaje y el modo elegidos viajan al servidor en cada PING. El servidor
 * decide si los acepta y lo informa de vuelta en el estado: la pantalla
 * siempre muestra lo que dice el servidor, no lo que pidió el cliente.
 */
public class LobbyScreen extends BaseMenuScreen {

    private final Rectangle readyButton = new Rectangle(110, 8, 120, 24);
    private final Rectangle leaveButton = new Rectangle(250, 8, 120, 24);

    private final Rectangle ffaButton = new Rectangle(258, 176, 200, 22);
    private final Rectangle teamsButton = new Rectangle(258, 150, 200, 22);

    /** Tarjetas de elección de personaje, en el mismo orden que CharacterType. */
    private final Rectangle[] characterCards = new Rectangle[CharacterType.values().length];

    /** Cuánto se espera la confirmación del servidor antes de avisar que no se pudo. */
    private static final float MODE_CONFIRM_TIMEOUT = 1.5f;
    /** Cuánto queda en pantalla un aviso. */
    private static final float NOTICE_DURATION = 3f;

    /** Modo que pidió este cliente y todavía no confirmó el servidor (null si no hay). */
    private GameMode pendingMode;
    private float pendingTimer;
    /** Último modo visto en el estado, para avisar cuando el anfitrión lo cambia. */
    private GameMode lastSeenMode;

    /** Aviso corto en la parte de abajo (por qué no se pudo cambiar el modo, etc.). */
    private String notice;
    private Color noticeColor = Color.WHITE;
    private float noticeTimer;
    private float blinkTimer;

    private final Texture[] sheets;
    /** Primer cuadro (mirando hacia abajo) de cada personaje, como retrato. */
    private final TextureRegion[] portraits;

    public LobbyScreen(Main game) {
        super(game, 1.4f);
        CharacterType[] types = CharacterType.values();
        sheets = new Texture[types.length];
        portraits = new TextureRegion[types.length];
        for (CharacterType type : types) {
            int i = type.id();
            characterCards[i] = new Rectangle(12 + i * 116, 52, 108, 74);
            sheets[i] = new Texture(Gdx.files.internal(type.name().toLowerCase() + "_walk.png"));
            portraits[i] = new TextureRegion(sheets[i], 0, 0, 32, 32);
        }
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
                    case Input.Keys.NUM_1: choose(CharacterType.VOLT); return true;
                    case Input.Keys.NUM_2: choose(CharacterType.BRUTUS); return true;
                    case Input.Keys.NUM_3: choose(CharacterType.IRIS); return true;
                    case Input.Keys.NUM_4: choose(CharacterType.PHANTOM); return true;
                    case Input.Keys.LEFT: cycleCharacter(-1); return true;
                    case Input.Keys.RIGHT: cycleCharacter(1); return true;
                    case Input.Keys.M:
                        WorldSnapshot snap = latest();
                        if (snap != null) {
                            GameMode shown = pendingMode != null ? pendingMode : snap.getMode();
                            chooseMode(shown == GameMode.TEAMS ? GameMode.FREE_FOR_ALL : GameMode.TEAMS);
                        }
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
                if (clicked(ffaButton, screenX, screenY)) {
                    chooseMode(GameMode.FREE_FOR_ALL);
                    return true;
                }
                if (clicked(teamsButton, screenX, screenY)) {
                    chooseMode(GameMode.TEAMS);
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

    private WorldSnapshot latest() {
        ClientNetwork net = game.getNetwork();
        return net == null ? null : net.getLatestSnapshot();
    }

    private boolean amHost(WorldSnapshot snap) {
        ClientNetwork net = game.getNetwork();
        return net != null && snap != null && snap.hostId == net.getPlayerId();
    }

    private void toggleReady() {
        ClientNetwork net = game.getNetwork();
        if (net != null) net.setReady(!net.isReady());
    }

    private void choose(CharacterType type) {
        ClientNetwork net = game.getNetwork();
        if (net != null) net.setCharacter(type);
    }

    private void cycleCharacter(int step) {
        ClientNetwork net = game.getNetwork();
        if (net == null) return;
        int n = CharacterType.values().length;
        choose(CharacterType.fromId(Math.floorMod(net.getCharacter().id() + step, n)));
    }

    /**
     * Elegir un modo. Antes de mandar el pedido se revisa lo mismo que va a
     * revisar el servidor, para avisarle al jugador en el momento por qué no
     * se puede, en vez de que el botón "no haga nada".
     */
    private void chooseMode(GameMode mode) {
        WorldSnapshot snap = latest();
        if (snap == null) return;
        int count = snap.players.size();
        if (!amHost(snap)) {
            WorldSnapshot.PlayerState host = snap.findPlayer(snap.hostId);
            showNotice("Solo el anfitrión" + (host == null ? "" : " (" + host.name + ")")
                + " puede cambiar el modo", UiKit.ORANGE);
            return;
        }
        if (!mode.allows(count)) {
            showNotice(mode.displayName + " necesita exactamente " + mode.requiredPlayers
                + " jugadores (hay " + count + ")", UiKit.ORANGE);
            return;
        }
        if (mode == snap.getMode() && pendingMode == null) return; // ya está elegido
        game.getNetwork().requestMode(mode);
        pendingMode = mode;
        pendingTimer = MODE_CONFIRM_TIMEOUT;
    }

    private void showNotice(String text, Color color) {
        notice = text;
        noticeColor = color;
        noticeTimer = NOTICE_DURATION;
    }

    private void leave() {
        game.disconnect();
        game.replaceScreen(new MenuScreen(game));
    }

    @Override
    protected boolean update(float delta) {
        blinkTimer += delta;
        if (noticeTimer > 0) noticeTimer -= delta;

        WorldSnapshot snap = latest();
        if (snap != null) {
            GameMode current = snap.getMode();
            // ¿El servidor confirmó el modo que pedí?
            if (pendingMode != null) {
                if (current == pendingMode) {
                    pendingMode = null;
                    showNotice("Modo: " + current.displayName, UiKit.GREEN);
                } else {
                    pendingTimer -= delta;
                    if (pendingTimer <= 0) {
                        pendingMode = null;
                        showNotice("El servidor no aceptó el cambio. Probá de nuevo.", UiKit.ORANGE);
                    }
                }
            } else if (lastSeenMode != null && current != lastSeenMode) {
                // Lo cambió el anfitrión (o el servidor, porque alguien se fue).
                showNotice(amHost(snap) ? "Modo: " + current.displayName
                    : "Ahora se juega: " + current.displayName, UiKit.YELLOW);
            }
            lastSeenMode = current;
        }
        return game.followServer(MatchPhase.LOBBY, MatchPhase.COUNTDOWN);
    }

    /** Jugadores en el orden en que se muestran (en Equipos, primero los azules). */
    private List<WorldSnapshot.PlayerState> orderedPlayers(WorldSnapshot snap) {
        List<WorldSnapshot.PlayerState> list = new ArrayList<>(snap.players);
        if (snap.getMode() == GameMode.TEAMS) list.sort((a, b) -> Integer.compare(a.team, b.team));
        return list;
    }

    @Override
    protected void drawShapes(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        WorldSnapshot snap = latest();
        boolean ready = net != null && net.isReady();
        CharacterType selected = net == null ? CharacterType.VOLT : net.getCharacter();

        // Panel de jugadores y panel de modo
        drawPanel(12, 134, 232, 96, UiKit.CYAN);
        drawPanel(250, 134, 218, 96, UiKit.YELLOW);

        if (snap != null) {
            // Marca de color de cada jugador
            float y = 196;
            for (WorldSnapshot.PlayerState p : orderedPlayers(snap)) {
                Color c = UiKit.playerColor(snap.getMode(), p.team);
                shapes.setColor(c);
                shapes.rect(20, y - 8, 4, 10);
                y -= 15;
            }

            // Botones de modo: el elegido queda encendido; los no disponibles, apagados
            boolean host = amHost(snap);
            int count = snap.players.size();
            for (GameMode mode : GameMode.values()) {
                Rectangle r = mode == GameMode.TEAMS ? teamsButton : ffaButton;
                boolean on = snap.getMode() == mode && pendingMode == null;
                boolean waiting = pendingMode == mode;
                boolean available = host && mode.allows(count);
                Vector3 m = available ? mouse : new Vector3(-100, -100, 0);
                if (waiting) {
                    // Esperando la confirmación del servidor: el botón titila
                    float k = 0.6f + 0.4f * Math.abs(MathUtils.sin(blinkTimer * 8f));
                    drawButton(r, m, k, k * 0.86f, 0.2f, true);
                } else if (on) drawButton(r, m, 1f, 0.86f, 0.2f, true);
                else if (available) drawButton(r, m, 0.3f, 0.7f, 1f);
                else drawButton(r, m, 0.35f, 0.35f, 0.4f);
            }
        }

        // Tarjetas de personaje: la elegida tiene un borde brillante
        for (CharacterType type : CharacterType.values()) {
            Rectangle card = characterCards[type.id()];
            Color accent = UiKit.characterColor(type);
            drawButton(card, mouse, accent.r, accent.g, accent.b, type == selected);
            shapes.setColor(accent.r * 0.15f, accent.g * 0.15f, accent.b * 0.15f, 0.9f);
            shapes.rect(card.x + 8, card.y + 30, 38, 38);
        }

        if (ready) drawButton(readyButton, mouse, 0.20f, 0.55f, 0.30f);
        else drawButton(readyButton, mouse, 0.20f, 0.50f, 0.85f);
        drawButton(leaveButton, mouse, 0.45f, 0.30f, 0.30f);
    }

    @Override
    protected void drawText(Vector3 mouse) {
        ClientNetwork net = game.getNetwork();
        WorldSnapshot snap = latest();

        drawTitle("Sala de espera", WIDTH / 2f, 264, Color.WHITE);
        smallFont.setColor(Color.GRAY);
        if (net != null) {
            drawCentered(smallFont, "Servidor: " + net.getServerName() + " (" + net.getServerAddress() + ")"
                + pingText(net), WIDTH / 2f, 244);
        }

        // --- Jugadores ---
        int count = snap == null ? 0 : snap.players.size();
        font.setColor(Color.WHITE);
        font.draw(batch, "Jugadores " + count + "/" + Protocol.MAX_PLAYERS, 20, 224);
        if (snap != null) {
            float y = 204;
            for (WorldSnapshot.PlayerState p : orderedPlayers(snap)) {
                boolean isMe = p.id == net.getPlayerId();
                boolean host = p.id == snap.hostId;
                smallFont.setColor(isMe ? Color.YELLOW : Color.WHITE);
                drawFitted(smallFont, (host ? "* " : "") + p.name + (isMe ? " (vos)" : ""), 30, y, 82);
                smallFont.setColor(UiKit.characterColor(CharacterType.fromId(p.character)));
                smallFont.draw(batch, CharacterType.fromId(p.character).displayName, 116, y);
                if (snap.getMode() == GameMode.TEAMS) {
                    smallFont.setColor(UiKit.playerColor(GameMode.TEAMS, p.team));
                    smallFont.draw(batch, p.team == Team.BLUE ? "Azul" : "Rojo", 160, y);
                }
                smallFont.setColor(p.ready ? Color.GREEN : Color.GRAY);
                smallFont.draw(batch, p.ready ? "LISTO" : "...", 200, y);
                y -= 15;
            }
            for (int i = count; i < Protocol.MAX_PLAYERS; i++) {
                smallFont.setColor(0.4f, 0.45f, 0.55f, 1f);
                smallFont.draw(batch, "- libre -", 30, y);
                y -= 15;
            }
        }

        // --- Modo de juego ---
        font.setColor(Color.WHITE);
        font.draw(batch, "Modo de juego", 258, 224);
        drawButtonLabel(ffaButton, GameMode.FREE_FOR_ALL.displayName);
        drawButtonLabel(teamsButton, GameMode.TEAMS.displayName);
        if (snap != null) {
            smallFont.setColor(Color.LIGHT_GRAY);
            String hint;
            if (!amHost(snap)) {
                WorldSnapshot.PlayerState host = snap.findPlayer(snap.hostId);
                hint = "Elige el anfitrión" + (host == null ? "" : ": " + host.name);
            } else if (pendingMode != null) {
                smallFont.setColor(UiKit.YELLOW);
                hint = "Cambiando a " + pendingMode.displayName + "...";
            } else if (count < GameMode.TEAMS.requiredPlayers) {
                hint = "SOS EL ANFITRIÓN - Equipos necesita 4 (" + count + "/4)";
            } else {
                smallFont.setColor(UiKit.YELLOW);
                hint = "SOS EL ANFITRIÓN - elegí el modo (tecla M)";
            }
            drawFitted(smallFont, hint, 258, 143, 204);
        }

        // --- Tarjetas de personaje ---
        for (CharacterType type : CharacterType.values()) {
            Rectangle card = characterCards[type.id()];
            Color accent = UiKit.characterColor(type);
            batch.draw(portraits[type.id()], card.x + 8, card.y + 31, 38, 38);
            float tx = card.x + 50;
            float maxW = card.width - 54;
            font.setColor(Color.WHITE);
            drawFitted(font, (type.id() + 1) + " " + type.displayName, tx, card.y + card.height - 7, maxW);
            smallFont.setColor(Color.GRAY);
            drawFitted(smallFont, type.role, tx, card.y + card.height - 22, maxW);
            smallFont.setColor(Color.LIGHT_GRAY);
            drawFitted(smallFont, "Vida " + (int) type.maxHealth, tx, card.y + card.height - 35, maxW);
            drawFitted(smallFont, "Daño " + (int) type.damage + "  Vel " + (int) type.speed,
                card.x + 9, card.y + 24, card.width - 14);
            smallFont.setColor(accent);
            drawFitted(smallFont, type.abilityName, card.x + 9, card.y + 12, card.width - 14);
        }

        font.setColor(Color.WHITE);
        if (noticeTimer > 0 && notice != null) {
            // Los avisos reemplazan por un momento al texto de estado
            smallFont.setColor(noticeColor);
            drawCentered(smallFont, notice, WIDTH / 2f, 46);
        } else if (snap != null) {
            drawCentered(smallFont, statusText(snap), WIDTH / 2f, 46);
        }

        boolean ready = net != null && net.isReady();
        drawButtonLabel(readyButton, ready ? "No estoy listo" : "Listo (ENTER)");
        drawButtonLabel(leaveButton, "Salir (ESC)");
    }

    private String statusText(WorldSnapshot snap) {
        smallFont.setColor(Color.WHITE);
        if (snap.phase == MatchPhase.COUNTDOWN) {
            smallFont.setColor(UiKit.YELLOW);
            return "La partida comienza en " + (int) Math.ceil(snap.phaseTimer) + "...";
        }
        if (snap.players.size() < GameConstants.MIN_PLAYERS) {
            return "Esperando jugadores (minimo " + GameConstants.MIN_PLAYERS + ")";
        }
        return "Elegi tu personaje (1 a 4) y marca Listo";
    }

    static String pingText(ClientNetwork net) {
        float ping = net.getPingMs();
        return ping < 0 ? "" : "  -  " + Math.round(ping) + " ms";
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Texture t : sheets) t.dispose();
    }
}
