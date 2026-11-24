package com.OverZone.Scardaoni;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.OverZone.Scardaoni.audio.AudioManager;
import com.OverZone.Scardaoni.ui.UiKit;
import com.OverZone.Scardaoni.shared.MatchPhase;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.shared.WorldSnapshot;
import com.OverZone.Scardaoni.screens.GameScreen;
import com.OverZone.Scardaoni.screens.LobbyScreen;
import com.OverZone.Scardaoni.screens.MenuScreen;
import com.OverZone.Scardaoni.screens.MessageScreen;
import com.OverZone.Scardaoni.screens.ResultsScreen;

/**
 * Clase principal del CLIENTE. Administra las pantallas, el audio y la
 * conexión con el servidor.
 *
 * Mientras hay conexión, las pantallas no deciden solas a cuál pasar: siguen
 * la etapa de la partida que informa el servidor (método followServer).
 */
public class Main extends Game {

    private AudioManager audioManager;
    private UiKit ui;
    private ClientNetwork network;

    @Override
    public void create() {
        audioManager = new AudioManager();
        audioManager.load();
        ui = new UiKit();
        setScreen(new MenuScreen(this));
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    /** Fuentes e imágenes de la interfaz, compartidas por todas las pantallas. */
    public UiKit getUi() {
        return ui;
    }

    // ------------------------------------------------------------------
    // Conexión
    // ------------------------------------------------------------------

    /** Abre una conexión nueva y empieza a buscar el servidor. */
    public void connect() {
        disconnect();
        network = new ClientNetwork(System.getProperty("user.name", "Jugador"));
        network.start();
    }

    /** Cierra la conexión actual, si hay una, avisándole al servidor. */
    public void disconnect() {
        if (network != null) {
            network.stop();
            network = null;
        }
    }

    public ClientNetwork getNetwork() {
        return network;
    }

    /**
     * Revisa la conexión y la etapa de la partida. Si la pantalla actual ya no
     * corresponde, cambia a la que corresponde y devuelve true; en ese caso la
     * pantalla que lo llamó debe terminar su render sin dibujar nada más.
     *
     * @param accepted etapas en las que la pantalla actual puede seguir.
     */
    public boolean followServer(MatchPhase... accepted) {
        ClientNetwork net = network;
        if (net == null) {
            replaceScreen(new MenuScreen(this));
            return true;
        }
        if (net.getStatus() == ClientNetwork.Status.FAILED) {
            String message = net.getFailureMessage();
            disconnect();
            replaceScreen(new MessageScreen(this, "Conexion perdida", message));
            return true;
        }
        WorldSnapshot snapshot = net.getLatestSnapshot();
        if (snapshot == null) return false; // todavía no llegó el primer estado

        for (MatchPhase phase : accepted) {
            if (snapshot.phase == phase) return false;
        }
        replaceScreen(screenForPhase(snapshot.phase));
        return true;
    }

    private Screen screenForPhase(MatchPhase phase) {
        switch (phase) {
            case PLAYING: return new GameScreen(this);
            case RESULTS: return new ResultsScreen(this);
            default: return new LobbyScreen(this);
        }
    }

    /**
     * Cambia de pantalla y libera la anterior. Game.setScreen no libera la
     * pantalla anterior; si no se hiciera, cada pantalla vieja quedaría
     * ocupando memoria. Se usa setScreen solo cuando se va a volver a la
     * pantalla anterior (por ejemplo, de la pausa al juego).
     */
    public void replaceScreen(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null && old != next) old.dispose();
    }

    @Override
    public void dispose() {
        // Al cerrar la ventana: avisar al servidor, liberar la pantalla y el audio.
        disconnect();
        super.dispose();
        if (getScreen() != null) getScreen().dispose();
        audioManager.dispose();
        ui.dispose();
    }
}
