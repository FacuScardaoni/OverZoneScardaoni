package com.OverZone.Scardaoni.pantallas;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.JuegoOverZone;
import com.OverZone.Scardaoni.interfaz.KitUi;
import com.OverZone.Scardaoni.compartido.FasePartida;

public class PantallaControles extends PantallaMenuBase {
    private final Screen pantallaAnterior;
    private final Rectangle botonVolver = new Rectangle(190, 22, 100, 26);

    private static final String[][] CONTROLES = {
        {"Moverse",              "W  A  S  D"},
        {"Apuntar",              "Mouse"},
        {"Disparar",             "Click izquierdo  /  botón rojo"},
        {"Habilidad especial",   "Click derecho  /  Espacio  /  botón amarillo"},
        {"Pausa",                "ESC"},
        {"Silenciar sonido",     "M"},
        {"Subir / bajar volumen","+  /  -"},
        {"Elegir personaje (sala)", "1 Volt  2 Brutus  3 Iris  4 Phantom"},
        {"Cambiar modo (sala)",  "M  (solo el anfitrión)"}
    };

    public PantallaControles(JuegoOverZone juego, Screen pantallaAnterior) {
        super(juego, 1.6f);
        this.pantallaAnterior = pantallaAnterior;
    }

    @Override
    protected InputProcessor crearProcesadorEntrada() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int codigoTecla) {
                if (codigoTecla == Input.Keys.ESCAPE) {
                    volver();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
                if (boton == Input.Buttons.LEFT && clickeado(botonVolver, pantallaX, pantallaY)) {
                    volver();
                    return true;
                }
                return false;
            }
        };
    }

    /** Vuelve a la pantalla anterior (que sigue viva) y libera esta. */
    private void volver() {
        juego.reemplazarPantalla(pantallaAnterior);
    }

    @Override
    protected boolean actualizar(float delta) {
        // Si se abrió durante una partida en red, se sigue vigilando la
        // conexión: si se cae o termina la partida, se sale de acá.
        if (juego.getRed() != null && juego.seguirServidor(FasePartida.JUGANDO)) {
            pantallaAnterior.dispose(); // la pausa (y con ella la partida) ya no se usa
            return true;
        }
        return false;
    }

    @Override
    protected void dibujarFormas(Vector3 mouse) {
        dibujarPanel(50, 52, 380, 178, KitUi.CIAN);
        dibujarBoton(botonVolver, mouse, 0.25f, 0.5f, 0.85f);
    }

    @Override
    protected void dibujarTexto(Vector3 mouse) {
        dibujarTitulo("Controles", ANCHO / 2f, 256, Color.WHITE);

        float y = 216;
        for (String[] fila : CONTROLES) {
            fuenteChica.setColor(KitUi.CIAN);
            fuenteChica.draw(batch, fila[0].toUpperCase(), 66, y);
            fuenteChica.setColor(Color.WHITE);
            fuenteChica.draw(batch, fila[1], 200, y);
            y -= 16;
        }
        fuenteChica.setColor(Color.LIGHT_GRAY);
        dibujarCentrado(fuenteChica, "Botones del HUD: arrastrá para apuntar y soltá para disparar.", ANCHO / 2f, 64);
        dibujarTextoBoton(botonVolver, "Volver");
    }
}
