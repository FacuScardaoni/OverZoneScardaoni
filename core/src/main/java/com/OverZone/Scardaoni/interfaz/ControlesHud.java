package com.OverZone.Scardaoni.interfaz;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ControlesHud extends InputAdapter {
    // Tamaño de los botones y su distancia al borde derecho e inferior de la
    // pantalla (en unidades del HUD). La posición se calcula según el ancho
    // del HUD, así quedan siempre pegados a la esquina aunque cambie la ventana.
    public static final float RADIO_ATAQUE = 30f, RADIO_SUPER = 21f;
    private static final float ATAQUE_DESDE_DERECHA = 46f, ATAQUE_Y = 50f;
    private static final float SUPER_DESDE_DERECHA = 106f, SUPER_Y = 28f;

    /** Arrastre mínimo para considerar que se está apuntando (si no, es un toque). */
    public static final float ZONA_MUERTA = 6f;
    /** Cuánto se puede alejar la "perilla" del centro del botón. */
    public static final float ARRASTRE_MAX = 32f;
    private static final float PULSO_DISPARO = 0.12f;

    private final Viewport vistaHud;

    private int punteroAtaque = -1, punteroSuper = -1;
    private final Vector2 arrastreAtaque = new Vector2(), arrastreSuper = new Vector2();

    private float pulsoDisparo = 0f;
    private final Vector2 direccionDisparo = new Vector2();
    private boolean disparoAutomatico;

    private boolean superLanzada = false;
    private final Vector2 direccionSuper = new Vector2();
    private boolean superAutomatica;

    public ControlesHud(Viewport vistaHud) {
        this.vistaHud = vistaHud;
    }

    private Vector2 aHud(int pantallaX, int pantallaY) {
        Vector3 v = vistaHud.unproject(new Vector3(pantallaX, pantallaY, 0));
        return new Vector2(v.x, v.y);
    }

    public float ataqueX() { return vistaHud.getWorldWidth() - ATAQUE_DESDE_DERECHA; }
    public float ataqueY() { return ATAQUE_Y; }
    public float superX() { return vistaHud.getWorldWidth() - SUPER_DESDE_DERECHA; }
    public float superY() { return SUPER_Y; }

    public boolean sobreAtaque(float x, float y) {
        return Vector2.dst(x, y, ataqueX(), ataqueY()) <= RADIO_ATAQUE + 4;
    }

    public boolean sobreSuper(float x, float y) {
        return Vector2.dst(x, y, superX(), superY()) <= RADIO_SUPER + 4;
    }

    @Override
    public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
        if (boton != Input.Buttons.LEFT) return false;
        Vector2 p = aHud(pantallaX, pantallaY);
        if (sobreAtaque(p.x, p.y)) {
            punteroAtaque = puntero;
            arrastreAtaque.setZero();
            return true;
        }
        if (sobreSuper(p.x, p.y)) {
            punteroSuper = puntero;
            arrastreSuper.setZero();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int pantallaX, int pantallaY, int puntero) {
        Vector2 p = aHud(pantallaX, pantallaY);
        if (puntero == punteroAtaque) {
            arrastreAtaque.set(p.x - ataqueX(), p.y - ataqueY()).limit(ARRASTRE_MAX);
            return true;
        }
        if (puntero == punteroSuper) {
            arrastreSuper.set(p.x - superX(), p.y - superY()).limit(ARRASTRE_MAX);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int pantallaX, int pantallaY, int puntero, int boton) {
        if (puntero == punteroAtaque) {
            punteroAtaque = -1;
            disparoAutomatico = arrastreAtaque.len() < ZONA_MUERTA;
            if (!disparoAutomatico) direccionDisparo.set(arrastreAtaque).nor();
            pulsoDisparo = PULSO_DISPARO;
            arrastreAtaque.setZero();
            return true;
        }
        if (puntero == punteroSuper) {
            punteroSuper = -1;
            superAutomatica = arrastreSuper.len() < ZONA_MUERTA;
            if (!superAutomatica) direccionSuper.set(arrastreSuper).nor();
            superLanzada = true;
            arrastreSuper.setZero();
            return true;
        }
        return false;
    }

    public void actualizar(float delta) {
        if (pulsoDisparo > 0) pulsoDisparo -= delta;
    }

    /** Suelta todo (por ejemplo, al abrir el menú de pausa). */
    public void reiniciar() {
        punteroAtaque = punteroSuper = -1;
        arrastreAtaque.setZero();
        arrastreSuper.setZero();
        pulsoDisparo = 0;
        superLanzada = false;
    }

    // ------------------------------------------------------------------
    // Consultas para la pantalla de juego y el HUD
    // ------------------------------------------------------------------

    public boolean ataqueApretado() { return punteroAtaque >= 0; }
    public boolean superApretada() { return punteroSuper >= 0; }

    /** Se está arrastrando el botón rojo lo suficiente como para apuntar. */
    public boolean apuntandoAtaque() { return ataqueApretado() && arrastreAtaque.len() >= ZONA_MUERTA; }
    public boolean apuntandoSuper() { return superApretada() && arrastreSuper.len() >= ZONA_MUERTA; }

    public Vector2 getArrastreAtaque() { return arrastreAtaque; }
    public Vector2 getArrastreSuper() { return arrastreSuper; }

    /** Hay un disparo en curso (se acaba de soltar el botón rojo). */
    public boolean estaDisparando() { return pulsoDisparo > 0; }
    public boolean esDisparoAutomatico() { return disparoAutomatico; }
    public Vector2 getDireccionDisparo() { return direccionDisparo; }

    /** Devuelve true una sola vez después de soltar el botón amarillo. */
    public boolean consumirSuper() {
        boolean lanzada = superLanzada;
        superLanzada = false;
        return lanzada;
    }

    public boolean esSuperAutomatica() { return superAutomatica; }
    public Vector2 getDireccionSuper() { return direccionSuper; }
}
