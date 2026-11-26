package com.OverZone.Scardaoni;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.OverZone.Scardaoni.audio.GestorAudio;
import com.OverZone.Scardaoni.interfaz.KitUi;
import com.OverZone.Scardaoni.compartido.FasePartida;
import com.OverZone.Scardaoni.red.RedCliente;
import com.OverZone.Scardaoni.compartido.EstadoMundo;
import com.OverZone.Scardaoni.pantallas.PantallaJuego;
import com.OverZone.Scardaoni.pantallas.PantallaSala;
import com.OverZone.Scardaoni.pantallas.PantallaMenu;
import com.OverZone.Scardaoni.pantallas.PantallaMensaje;
import com.OverZone.Scardaoni.pantallas.PantallaResultados;

public class JuegoOverZone extends Game {
    private GestorAudio gestorAudio;
    private KitUi interfaz;
    private RedCliente red;

    @Override
    public void create() {
        gestorAudio = new GestorAudio();
        gestorAudio.cargar();
        interfaz = new KitUi();
        setScreen(new PantallaMenu(this));
    }

    public GestorAudio getGestorAudio() {
        return gestorAudio;
    }

    /** Fuentes e imágenes de la interfaz, compartidas por todas las pantallas. */
    public KitUi getInterfaz() {
        return interfaz;
    }

    // ------------------------------------------------------------------
    // Conexión
    // ------------------------------------------------------------------

    /** Abre una conexión nueva y empieza a buscar el servidor. */
    public void conectar() {
        desconectar();
        red = new RedCliente(System.getProperty("user.name", "Jugador"));
        red.iniciar();
    }

    /** Cierra la conexión actual, si hay una, avisándole al servidor. */
    public void desconectar() {
        if (red != null) {
            red.detener();
            red = null;
        }
    }

    public RedCliente getRed() {
        return red;
    }

    public boolean seguirServidor(FasePartida... aceptadas) {
        RedCliente conexion = red;
        if (conexion == null) {
            reemplazarPantalla(new PantallaMenu(this));
            return true;
        }
        if (conexion.getEstadoConexion() == RedCliente.EstadoConexion.FALLIDO) {
            String mensaje = conexion.getMensajeFalla();
            desconectar();
            reemplazarPantalla(new PantallaMensaje(this, "Conexion perdida", mensaje));
            return true;
        }
        EstadoMundo estado = conexion.getUltimoEstado();
        if (estado == null) return false; // todavía no llegó el primer estado

        for (FasePartida fase : aceptadas) {
            if (estado.fase == fase) return false;
        }
        reemplazarPantalla(pantallaParaFase(estado.fase));
        return true;
    }

    private Screen pantallaParaFase(FasePartida fase) {
        switch (fase) {
            case JUGANDO: return new PantallaJuego(this);
            case RESULTADOS: return new PantallaResultados(this);
            default: return new PantallaSala(this);
        }
    }

    public void reemplazarPantalla(Screen siguiente) {
        Screen vieja = getScreen();
        setScreen(siguiente);
        if (vieja != null && vieja != siguiente) vieja.dispose();
    }

    @Override
    public void dispose() {
        // Al cerrar la ventana: avisar al servidor, liberar la pantalla y el audio.
        desconectar();
        super.dispose();
        if (getScreen() != null) getScreen().dispose();
        gestorAudio.dispose();
        interfaz.dispose();
    }
}
