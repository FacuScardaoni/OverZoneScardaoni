package com.OverZone.Scardaoni.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Botones de ataque y súper del HUD, al estilo de los juegos de celular.
 *
 * Funcionan como pequeños joysticks:
 *  - Se presiona sobre el botón y se ARRASTRA para apuntar (en el mapa se ve
 *    una línea de mira).
 *  - Al SOLTAR, se dispara (botón rojo) o se usa la habilidad (botón amarillo)
 *    en esa dirección.
 *  - Si se toca y se suelta sin arrastrar, el disparo apunta solo al enemigo
 *    más cercano (autoapuntado).
 *
 * Esta clase solo traduce el mouse en "intenciones" (dirección y momento del
 * disparo). Igual que el resto del cliente, no decide nada del juego: esas
 * intenciones se envían al servidor como entradas normales.
 */
public class HudControls extends InputAdapter {

    // Tamaño de los botones y su distancia al borde derecho e inferior de la
    // pantalla (en unidades del HUD). La posición se calcula según el ancho
    // del HUD, así quedan siempre pegados a la esquina aunque cambie la ventana.
    public static final float ATTACK_R = 30f, SUPER_R = 21f;
    private static final float ATTACK_FROM_RIGHT = 46f, ATTACK_Y = 50f;
    private static final float SUPER_FROM_RIGHT = 106f, SUPER_Y = 28f;

    /** Arrastre mínimo para considerar que se está apuntando (si no, es un toque). */
    public static final float DEADZONE = 6f;
    /** Cuánto se puede alejar la "perilla" del centro del botón. */
    public static final float MAX_DRAG = 32f;
    /**
     * Durante cuánto tiempo se mantiene apretado el disparo después de soltar.
     * Es menor que el tiempo entre disparos de los dos personajes, así que
     * produce un único disparo; y como dura varios frames, aunque se pierda
     * algún paquete en la red, el servidor igual recibe el pedido.
     */
    private static final float SHOT_PULSE = 0.12f;

    private final Viewport hudViewport;

    private int attackPointer = -1, superPointer = -1;
    private final Vector2 attackDrag = new Vector2(), superDrag = new Vector2();

    private float shotPulse = 0f;
    private final Vector2 shotDir = new Vector2();
    private boolean shotAuto;

    private boolean superFired = false;
    private final Vector2 superDir = new Vector2();
    private boolean superAuto;

    public HudControls(Viewport hudViewport) {
        this.hudViewport = hudViewport;
    }

    private Vector2 toHud(int screenX, int screenY) {
        Vector3 v = hudViewport.unproject(new Vector3(screenX, screenY, 0));
        return new Vector2(v.x, v.y);
    }

    public float attackX() { return hudViewport.getWorldWidth() - ATTACK_FROM_RIGHT; }
    public float attackY() { return ATTACK_Y; }
    public float superX() { return hudViewport.getWorldWidth() - SUPER_FROM_RIGHT; }
    public float superY() { return SUPER_Y; }

    public boolean overAttack(float x, float y) {
        return Vector2.dst(x, y, attackX(), attackY()) <= ATTACK_R + 4;
    }

    public boolean overSuper(float x, float y) {
        return Vector2.dst(x, y, superX(), superY()) <= SUPER_R + 4;
    }

    // ------------------------------------------------------------------
    // Eventos del mouse. Devuelven true si el evento era para un botón, así
    // no le llega a PlayerInputProcessor (y no dispara hacia el mouse).
    // ------------------------------------------------------------------

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != Input.Buttons.LEFT) return false;
        Vector2 p = toHud(screenX, screenY);
        if (overAttack(p.x, p.y)) {
            attackPointer = pointer;
            attackDrag.setZero();
            return true;
        }
        if (overSuper(p.x, p.y)) {
            superPointer = pointer;
            superDrag.setZero();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        Vector2 p = toHud(screenX, screenY);
        if (pointer == attackPointer) {
            attackDrag.set(p.x - attackX(), p.y - attackY()).limit(MAX_DRAG);
            return true;
        }
        if (pointer == superPointer) {
            superDrag.set(p.x - superX(), p.y - superY()).limit(MAX_DRAG);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == attackPointer) {
            attackPointer = -1;
            shotAuto = attackDrag.len() < DEADZONE;
            if (!shotAuto) shotDir.set(attackDrag).nor();
            shotPulse = SHOT_PULSE;
            attackDrag.setZero();
            return true;
        }
        if (pointer == superPointer) {
            superPointer = -1;
            superAuto = superDrag.len() < DEADZONE;
            if (!superAuto) superDir.set(superDrag).nor();
            superFired = true;
            superDrag.setZero();
            return true;
        }
        return false;
    }

    public void update(float delta) {
        if (shotPulse > 0) shotPulse -= delta;
    }

    /** Suelta todo (por ejemplo, al abrir el menú de pausa). */
    public void reset() {
        attackPointer = superPointer = -1;
        attackDrag.setZero();
        superDrag.setZero();
        shotPulse = 0;
        superFired = false;
    }

    // ------------------------------------------------------------------
    // Consultas para la pantalla de juego y el HUD
    // ------------------------------------------------------------------

    public boolean isAttackHeld() { return attackPointer >= 0; }
    public boolean isSuperHeld() { return superPointer >= 0; }

    /** Se está arrastrando el botón rojo lo suficiente como para apuntar. */
    public boolean isAimingAttack() { return isAttackHeld() && attackDrag.len() >= DEADZONE; }
    public boolean isAimingSuper() { return isSuperHeld() && superDrag.len() >= DEADZONE; }

    public Vector2 getAttackDrag() { return attackDrag; }
    public Vector2 getSuperDrag() { return superDrag; }

    /** Hay un disparo en curso (se acaba de soltar el botón rojo). */
    public boolean isShooting() { return shotPulse > 0; }
    public boolean isShotAuto() { return shotAuto; }
    public Vector2 getShotDir() { return shotDir; }

    /** Devuelve true una sola vez después de soltar el botón amarillo. */
    public boolean consumeSuper() {
        boolean fired = superFired;
        superFired = false;
        return fired;
    }

    public boolean isSuperAuto() { return superAuto; }
    public Vector2 getSuperDir() { return superDir; }
}
