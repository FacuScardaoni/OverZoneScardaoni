package com.OverZone.Scardaoni.pantallas;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.JuegoOverZone;
import com.OverZone.Scardaoni.interfaz.KitUi;
import com.OverZone.Scardaoni.audio.GestorAudio;
import com.OverZone.Scardaoni.compartido.FasePartida;

/**
 * Menú de pausa durante una partida en red.
 *
 * En un juego en red la partida NO se detiene: sigue corriendo en el
 * servidor. Mientras este menú está abierto, el cliente deja de mandar
 * entradas y el servidor detiene al personaje. La conexión sigue activa.
 */
public class PantallaPausa extends PantallaMenuBase {

    private final PantallaJuego pantallaJuego;
    private final GestorAudio audio;
    /** true si se volvió a la partida: en ese caso no hay que liberarla. */
    private boolean reanudada = false;

    private final Rectangle botonReanudar     = new Rectangle(170, 160, 140, 30);
    private final Rectangle botonControles   = new Rectangle(170, 122, 140, 30);
    private final Rectangle botonDesconectar = new Rectangle(170,  84, 140, 30);
    private final Rectangle botonSilencio       = new Rectangle(150,  10, 72, 20);

    // --- Barra de volumen ---
    private static final float DESLIZADOR_X = 150f;
    private static final float DESLIZADOR_Y = 42f;
    private static final float DESLIZADOR_ANCHO = 200f;
    private static final float DESLIZADOR_ALTO = 6f;
    private static final float RADIO_PERILLA = 7f;
    private final Rectangle zonaDeslizador = new Rectangle(
        DESLIZADOR_X - RADIO_PERILLA, DESLIZADOR_Y - 11, DESLIZADOR_ANCHO + RADIO_PERILLA * 2, 28);
    private boolean arrastrandoVolumen = false;

    public PantallaPausa(JuegoOverZone juego, PantallaJuego pantallaJuego) {
        super(juego, 1.8f);
        this.pantallaJuego = pantallaJuego;
        this.audio = juego.getGestorAudio();
    }

    @Override
    protected InputProcessor crearProcesadorEntrada() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int codigoTecla) {
                switch (codigoTecla) {
                    case Input.Keys.ESCAPE: reanudarJuego(); return true;
                    case Input.Keys.M: audio.mutearDesmutear(); return true;
                    case Input.Keys.PLUS:
                    case Input.Keys.EQUALS: audio.subirVolumen(); return true;
                    case Input.Keys.MINUS: audio.bajarVolumen(); return true;
                    default: return false;
                }
            }

            @Override
            public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
                if (boton != Input.Buttons.LEFT) return false;
                Vector3 mundo = aMundo(pantallaX, pantallaY);

                if (zonaDeslizador.contains(mundo.x, mundo.y)) {
                    arrastrandoVolumen = true;
                    actualizarVolumenDesdeX(mundo.x);
                } else if (botonSilencio.contains(mundo.x, mundo.y)) {
                    audio.mutearDesmutear();
                } else if (botonReanudar.contains(mundo.x, mundo.y)) {
                    reanudarJuego();
                } else if (botonControles.contains(mundo.x, mundo.y)) {
                    juego.setScreen(new PantallaControles(juego, PantallaPausa.this));
                } else if (botonDesconectar.contains(mundo.x, mundo.y)) {
                    juego.desconectar();
                    juego.reemplazarPantalla(new PantallaMenu(juego));
                } else {
                    return false;
                }
                return true;
            }

            @Override
            public boolean touchDragged(int pantallaX, int pantallaY, int puntero) {
                if (!arrastrandoVolumen) return false;
                actualizarVolumenDesdeX(aMundo(pantallaX, pantallaY).x);
                return true;
            }

            @Override
            public boolean touchUp(int pantallaX, int pantallaY, int puntero, int boton) {
                arrastrandoVolumen = false;
                return false;
            }
        };
    }

    /** Convierte una posición horizontal en un volumen de 0 a 1. */
    private void actualizarVolumenDesdeX(float mundoX) {
        float proporcion = (mundoX - DESLIZADOR_X) / DESLIZADOR_ANCHO;
        audio.setVolumenXBarra(MathUtils.clamp(proporcion, 0f, 1f));
    }

    private void reanudarJuego() {
        reanudada = true;
        juego.reemplazarPantalla(pantallaJuego); // libera esta pantalla, no la partida
        pantallaJuego.reanudarJuego();
    }

    @Override
    protected boolean actualizar(float delta) {
        // Si la partida termina o se pierde la conexión con la pausa abierta,
        // se sale de acá (y se libera la partida).
        return juego.seguirServidor(FasePartida.JUGANDO);
    }

    @Override
    protected void dibujarFormas(Vector3 mouse) {
        float volumen = audio.getVolumen();
        boolean silenciado = audio.estaSilenciado();
        float volumenMostrado = silenciado ? 0f : volumen;

        dibujarBoton(botonReanudar, mouse, 0.20f, 0.50f, 0.85f);
        dibujarBoton(botonControles, mouse, 0.25f, 0.40f, 0.55f);
        dibujarBoton(botonDesconectar, mouse, 0.45f, 0.30f, 0.30f);
        dibujarBoton(botonSilencio, mouse, silenciado ? 0.45f : 0.25f, 0.25f, 0.30f);

        dibujarPanel(52, DESLIZADOR_Y - 14, 376, 34, KitUi.CIAN);
        formas.setColor(0.12f, 0.14f, 0.2f, 1f);
        formas.rect(DESLIZADOR_X, DESLIZADOR_Y, DESLIZADOR_ANCHO, DESLIZADOR_ALTO);
        if (silenciado) formas.setColor(0.35f, 0.35f, 0.38f, 1f);
        else formas.setColor(KitUi.CIAN);
        formas.rect(DESLIZADOR_X, DESLIZADOR_Y, DESLIZADOR_ANCHO * volumenMostrado, DESLIZADOR_ALTO);

        boolean deslizadorActivo = arrastrandoVolumen || zonaDeslizador.contains(mouse.x, mouse.y);
        formas.setColor(deslizadorActivo ? 1f : 0.85f, deslizadorActivo ? 1f : 0.85f, 0.9f, 1f);
        formas.circle(DESLIZADOR_X + DESLIZADOR_ANCHO * volumenMostrado, DESLIZADOR_Y + DESLIZADOR_ALTO / 2f, RADIO_PERILLA);
    }

    @Override
    protected void dibujarTexto(Vector3 mouse) {
        float volumen = audio.getVolumen();
        boolean silenciado = audio.estaSilenciado();

        dibujarTitulo("Pausa", ANCHO / 2f, 250, Color.WHITE);
        fuente.setColor(Color.ORANGE);
        dibujarCentrado(fuente, "La partida sigue en curso: tu personaje esta quieto", ANCHO / 2f, 210);

        dibujarTextoBoton(botonReanudar, "Reanudar");
        dibujarTextoBoton(botonControles, "Controles");
        dibujarTextoBoton(botonDesconectar, "Desconectarse");

        fuente.setColor(Color.LIGHT_GRAY);
        fuente.draw(batch, "Volumen", 62, DESLIZADOR_Y + 11);
        fuente.setColor(Color.WHITE);
        fuente.draw(batch, silenciado ? "OFF" : (int) (volumen * 100) + "%", 360, DESLIZADOR_Y + 11);
        dibujarTextoBoton(botonSilencio, silenciado ? "Activar" : "Silenciar");
    }

    @Override
    public void dispose() {
        super.dispose();
        // Si no se volvió a la partida, la partida tampoco se va a usar más.
        if (!reanudada) pantallaJuego.dispose();
    }
}
