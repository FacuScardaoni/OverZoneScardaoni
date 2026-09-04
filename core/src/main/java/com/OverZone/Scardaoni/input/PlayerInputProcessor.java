package com.OverZone.Scardaoni.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

/**
 * Clase dedicada exclusivamente a procesar las entradas del jugador
 * (Consigna punto 1: Manejo de entradas y control de los elementos).
 *
 * Se registra en el multiplexor de Gdx.input y traduce teclado + mouse
 * en un estado consultable por el resto del juego (Player, GameScreen, etc).
 * No contiene lógica de juego: solo captura y expone el estado de entrada.
 */
public class PlayerInputProcessor extends InputAdapter {

    // Movimiento (WASD -> 8 direcciones combinando ejes)
    private boolean up, down, left, right;

    // Mouse
    private float mouseWorldX, mouseWorldY; // se actualiza desde afuera (GameScreen conoce la cámara)
    private boolean firePressed;      // click izquierdo mantenido
    private boolean fireJustPressed;  // flanco de subida, para disparo semi-automático si se desea
    private boolean abilityJustPressed; // click derecho -> habilidad especial

    // Otros
    private boolean pauseJustPressed;   // ESC
    private boolean muteJustPressed;    // M
    private boolean volumeUpJustPressed;   // '+'
    private boolean volumeDownJustPressed; // '-'

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.W: up = true; break;
            case Input.Keys.S: down = true; break;
            case Input.Keys.A: left = true; break;
            case Input.Keys.D: right = true; break;
            case Input.Keys.SPACE: abilityJustPressed = true; break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.W: up = false; break;
            case Input.Keys.S: down = false; break;
            case Input.Keys.A: left = false; break;
            case Input.Keys.D: right = false; break;
            case Input.Keys.ESCAPE: pauseJustPressed = true; break;
            case Input.Keys.M: muteJustPressed = true; break;
            case Input.Keys.PLUS:
            case Input.Keys.EQUALS: volumeUpJustPressed = true; break;
            case Input.Keys.MINUS: volumeDownJustPressed = true; break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            firePressed = true;
            fireJustPressed = true;
        } else if (button == Input.Buttons.RIGHT) {
            abilityJustPressed = true;
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            firePressed = false;
        }
        return true;
    }


    public void resetTransient() {
        fireJustPressed = false;
        abilityJustPressed = false;
        pauseJustPressed = false;
        muteJustPressed = false;
        volumeUpJustPressed = false;
        volumeDownJustPressed = false;
    }

    /**
     * Limpia el estado de las teclas de movimiento. Se usa al cambiar de
     * pantalla (por ejemplo al entrar o salir de la pausa), ya que el evento
     * keyUp puede ocurrir mientras otra pantalla tiene el foco, dejando al
     * personaje moviéndose solo al regresar.
     */
    public void releaseAllKeys() {
        up = down = left = right = false;
        firePressed = false;
    }

    public void setMouseWorldPosition(float x, float y) {
        this.mouseWorldX = x;
        this.mouseWorldY = y;
    }

    public float getMoveX() {
        float x = 0;
        if (left) x -= 1;
        if (right) x += 1;
        return x;
    }

    public float getMoveY() {
        float y = 0;
        if (up) y += 1;
        if (down) y -= 1;
        return y;
    }

    public boolean isMoving() {
        return up || down || left || right;
    }

    public float getMouseWorldX() { return mouseWorldX; }
    public float getMouseWorldY() { return mouseWorldY; }

    public boolean isFirePressed() { return firePressed; }
    public boolean isFireJustPressed() { return fireJustPressed; }
    public boolean isAbilityJustPressed() { return abilityJustPressed; }
    public boolean isPauseJustPressed() { return pauseJustPressed; }
    public boolean isMuteJustPressed() { return muteJustPressed; }
    public boolean isVolumeUpJustPressed() { return volumeUpJustPressed; }
    public boolean isVolumeDownJustPressed() { return volumeDownJustPressed; }
}
