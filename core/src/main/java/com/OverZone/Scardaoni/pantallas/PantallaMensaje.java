package com.OverZone.Scardaoni.pantallas;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.JuegoOverZone;

public class PantallaMensaje extends PantallaMenuBase {
    private final String titulo;
    private final String[] lineas;
    private final Rectangle botonMenu = new Rectangle(170, 40, 140, 30);

    public PantallaMensaje(JuegoOverZone juego, String titulo, String mensaje) {
        super(juego, 1.6f);
        this.titulo = titulo;
        this.lineas = mensaje.split("\n");
    }

    @Override
    public void show() {
        super.show();
        juego.getGestorAudio().pausarMusica();
    }

    @Override
    protected InputProcessor crearProcesadorEntrada() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int codigoTecla) {
                if (codigoTecla == Input.Keys.ENTER || codigoTecla == Input.Keys.ESCAPE) {
                    volverAlMenu();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
                if (boton == Input.Buttons.LEFT && clickeado(botonMenu, pantallaX, pantallaY)) {
                    volverAlMenu();
                    return true;
                }
                return false;
            }
        };
    }

    private void volverAlMenu() {
        juego.reemplazarPantalla(new PantallaMenu(juego));
    }

    @Override
    protected void dibujarFormas(Vector3 mouse) {
        dibujarBoton(botonMenu, mouse, 0.25f, 0.40f, 0.55f);
    }

    @Override
    protected void dibujarTexto(Vector3 mouse) {
        dibujarTitulo(titulo, ANCHO / 2f, 222, Color.ORANGE);
        fuente.setColor(Color.WHITE);
        float y = 175;
        for (String linea : lineas) {
            dibujarCentrado(fuente, linea, ANCHO / 2f, y);
            y -= 20;
        }
        dibujarTextoBoton(botonMenu, "Menu principal");
    }
}
