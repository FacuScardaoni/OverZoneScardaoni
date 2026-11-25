package com.OverZone.Scardaoni.entrada;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

/**
 * Clase dedicada exclusivamente a procesar las entradas del jugador
 * (Consigna punto 1: Manejo de entradas y control de los elementos).
 *
 * Se registra en el multiplexor de Gdx.input y traduce teclado + mouse
 * en un estado consultable por el resto del juego (Jugador, PantallaJuego, etc).
 * No contiene lógica de juego: solo captura y expone el estado de entrada.
 */
public class ProcesadorEntrada extends InputAdapter {

    // Movimiento (WASD -> 8 direcciones combinando ejes)
    private boolean arriba, abajo, izquierda, derecha;

    // Mouse
    private float mouseMundoX, mouseMundoY; // se actualiza desde afuera (PantallaJuego conoce la cámara)
    private boolean disparoApretado;      // click izquierdo mantenido
    private boolean disparoRecienApretado;  // flanco de subida, para disparo semi-automático si se desea
    private boolean habilidadRecienApretada; // click derecho -> habilidad especial

    // Otros
    private boolean pausaRecienApretada;   // ESC
    private boolean silencioRecienApretado;    // M
    private boolean subirVolumenRecienApretado;   // '+'
    private boolean bajarVolumenRecienApretado; // '-'

    @Override
    public boolean keyDown(int codigoTecla) {
        switch (codigoTecla) {
            case Input.Keys.W: arriba = true; break;
            case Input.Keys.S: abajo = true; break;
            case Input.Keys.A: izquierda = true; break;
            case Input.Keys.D: derecha = true; break;
            case Input.Keys.SPACE: habilidadRecienApretada = true; break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean keyUp(int codigoTecla) {
        switch (codigoTecla) {
            case Input.Keys.W: arriba = false; break;
            case Input.Keys.S: abajo = false; break;
            case Input.Keys.A: izquierda = false; break;
            case Input.Keys.D: derecha = false; break;
            case Input.Keys.ESCAPE: pausaRecienApretada = true; break;
            case Input.Keys.M: silencioRecienApretado = true; break;
            case Input.Keys.PLUS:
            case Input.Keys.EQUALS: subirVolumenRecienApretado = true; break;
            case Input.Keys.MINUS: bajarVolumenRecienApretado = true; break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
        if (boton == Input.Buttons.LEFT) {
            disparoApretado = true;
            disparoRecienApretado = true;
        } else if (boton == Input.Buttons.RIGHT) {
            habilidadRecienApretada = true;
        }
        return true;
    }

    @Override
    public boolean touchUp(int pantallaX, int pantallaY, int puntero, int boton) {
        if (boton == Input.Buttons.LEFT) {
            disparoApretado = false;
        }
        return true;
    }


    public void reiniciarMomentaneas() {
        disparoRecienApretado = false;
        habilidadRecienApretada = false;
        pausaRecienApretada = false;
        silencioRecienApretado = false;
        subirVolumenRecienApretado = false;
        bajarVolumenRecienApretado = false;
    }

    /**
     * Limpia el estado de las teclas de movimiento. Se usa al cambiar de
     * pantalla (por ejemplo al entrar o salir de la pausa), ya que el evento
     * keyUp puede ocurrir mientras otra pantalla tiene el foco, dejando al
     * personaje moviéndose solo al regresar.
     */
    public void soltarTodasLasTeclas() {
        arriba = abajo = izquierda = derecha = false;
        disparoApretado = false;
    }

    public void setPosicionMouseMundo(float x, float y) {
        this.mouseMundoX = x;
        this.mouseMundoY = y;
    }

    public float getMoverX() {
        float x = 0;
        if (izquierda) x -= 1;
        if (derecha) x += 1;
        return x;
    }

    public float getMoverY() {
        float y = 0;
        if (arriba) y += 1;
        if (abajo) y -= 1;
        return y;
    }

    public boolean seEstaMoviendo() {
        return arriba || abajo || izquierda || derecha;
    }

    public float getMouseMundoX() { return mouseMundoX; }
    public float getMouseMundoY() { return mouseMundoY; }

    public boolean disparoEstaApretado() { return disparoApretado; }
    public boolean disparoFueRecienApretado() { return disparoRecienApretado; }
    public boolean habilidadFueRecienApretada() { return habilidadRecienApretada; }
    public boolean pausaFueRecienApretada() { return pausaRecienApretada; }
    public boolean silencioFueRecienApretado() { return silencioRecienApretado; }
    public boolean subirVolumenFueRecienApretado() { return subirVolumenRecienApretado; }
    public boolean bajarVolumenFueRecienApretado() { return bajarVolumenRecienApretado; }
}
