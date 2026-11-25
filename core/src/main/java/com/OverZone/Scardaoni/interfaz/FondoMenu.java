package com.OverZone.Scardaoni.interfaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Fondo animado de los menús: degradé oscuro, una grilla en perspectiva que
 * avanza hacia la pantalla, líneas de velocidad, chispas que suben y
 * explosiones de energía que aparecen cada tanto.
 *
 * Todo se calcula con el tiempo (update) y se dibuja con formas simples, sin
 * imágenes salvo la textura de brillo.
 */
public class FondoMenu {

    private static final float ANCHO_FONDO = 480f, ALTO_FONDO = 270f;
    private static final float HORIZONTE = 96f;
    private static final int BRASAS = 70;
    private static final int MAX_CHISPAS = 120;

    private static final Color ARRIBA_FONDO = new Color(0.02f, 0.03f, 0.07f, 1f);
    private static final Color ABAJO_FONDO = new Color(0.09f, 0.03f, 0.09f, 1f);

    private float tiempo = 0f;

    // Chispas que suben lentamente
    private final float[] brasaX = new float[BRASAS], brasaY = new float[BRASAS];
    private final float[] brasaVx = new float[BRASAS], brasaVy = new float[BRASAS], brasaTamanio = new float[BRASAS];
    private final boolean[] brasaNaranja = new boolean[BRASAS];

    // Explosión actual (una por vez) y sus esquirlas
    private float explosionX, explosionY, edadExplosion = 99f;
    private float proximaExplosion = 0.8f;
    private boolean explosionCian;
    private final float[] sx = new float[MAX_CHISPAS], sy = new float[MAX_CHISPAS];
    private final float[] chispaVx = new float[MAX_CHISPAS], chispaVy = new float[MAX_CHISPAS], chispaVida = new float[MAX_CHISPAS];

    public FondoMenu() {
        for (int i = 0; i < BRASAS; i++) reiniciarBrasa(i, true);
    }

    private void reiniciarBrasa(int i, boolean enCualquierLado) {
        brasaX[i] = MathUtils.random(0f, ANCHO_FONDO);
        brasaY[i] = enCualquierLado ? MathUtils.random(0f, ALTO_FONDO) : MathUtils.random(-10f, 0f);
        brasaVx[i] = MathUtils.random(-6f, 6f);
        brasaVy[i] = MathUtils.random(10f, 34f);
        brasaTamanio[i] = MathUtils.random(0.8f, 2.4f);
        brasaNaranja[i] = MathUtils.randomBoolean(0.75f);
    }

    public void actualizar(float delta) {
        tiempo += delta;

        for (int i = 0; i < BRASAS; i++) {
            brasaX[i] += (brasaVx[i] + MathUtils.sin(tiempo * 2 + i) * 4f) * delta;
            brasaY[i] += brasaVy[i] * delta;
            if (brasaY[i] > ALTO_FONDO + 5) reiniciarBrasa(i, false);
        }

        edadExplosion += delta;
        proximaExplosion -= delta;
        if (proximaExplosion <= 0) {
            proximaExplosion = MathUtils.random(1.4f, 2.8f);
            edadExplosion = 0f;
            explosionX = MathUtils.random(40f, ANCHO_FONDO - 40f);
            explosionY = MathUtils.random(HORIZONTE + 30f, ALTO_FONDO - 30f);
            explosionCian = MathUtils.randomBoolean(0.35f);
            int aparecidas = 0;
            for (int i = 0; i < MAX_CHISPAS && aparecidas < 26; i++) {
                if (chispaVida[i] > 0) continue;
                float ang = MathUtils.random(0f, MathUtils.PI2);
                float velocidad = MathUtils.random(40f, 130f);
                sx[i] = explosionX;
                sy[i] = explosionY;
                chispaVx[i] = MathUtils.cos(ang) * velocidad;
                chispaVy[i] = MathUtils.sin(ang) * velocidad;
                chispaVida[i] = MathUtils.random(0.5f, 1.1f);
                aparecidas++;
            }
        }
        for (int i = 0; i < MAX_CHISPAS; i++) {
            if (chispaVida[i] <= 0) continue;
            chispaVida[i] -= delta;
            sx[i] += chispaVx[i] * delta;
            sy[i] += chispaVy[i] * delta;
            chispaVx[i] *= 0.97f;
            chispaVy[i] = chispaVy[i] * 0.97f - 60f * delta;   // un poco de gravedad
        }
    }

    /** Dibuja las formas del fondo. Abre y cierra el ShapeRenderer por su cuenta. */
    public void dibujarFormas(ShapeRenderer s) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        s.begin(ShapeRenderer.ShapeType.Filled);
        // Degradé: azul muy oscuro arriba, violeta con un toque naranja abajo
        s.rect(0, 0, ANCHO_FONDO, ALTO_FONDO, ABAJO_FONDO, ABAJO_FONDO, ARRIBA_FONDO, ARRIBA_FONDO);
        // Resplandor del horizonte
        for (int i = 0; i < 6; i++) {
            s.setColor(1f, 0.35f, 0.1f, 0.05f);
            s.rect(0, HORIZONTE - 6 + i * 2, ANCHO_FONDO, 14 - i * 2.2f);
        }
        // Chispas
        for (int i = 0; i < BRASAS; i++) {
            float parpadeo = 0.55f + 0.45f * MathUtils.sin(tiempo * 6 + i * 1.7f);
            if (brasaNaranja[i]) s.setColor(1f, 0.55f + 0.3f * parpadeo, 0.15f, 0.85f * parpadeo);
            else s.setColor(0.4f, 0.9f, 1f, 0.7f * parpadeo);
            s.rect(brasaX[i], brasaY[i], brasaTamanio[i], brasaTamanio[i]);
        }
        // Esquirlas de la explosión
        for (int i = 0; i < MAX_CHISPAS; i++) {
            if (chispaVida[i] <= 0) continue;
            float a = Math.min(1f, chispaVida[i] * 1.6f);
            if (explosionCian) s.setColor(0.6f, 0.95f, 1f, a);
            else s.setColor(1f, 0.75f, 0.25f, a);
            s.rect(sx[i] - 1, sy[i] - 1, 2.2f, 2.2f);
        }

        s.set(ShapeRenderer.ShapeType.Line);
        // Grilla en perspectiva: líneas que salen del punto de fuga...
        float vx = ANCHO_FONDO / 2f;
        for (float x = -480; x <= ANCHO_FONDO + 480; x += 40) {
            s.setColor(0.13f, 0.7f, 1f, 0.22f);
            s.line(vx, HORIZONTE, x, 0);
        }
        // ...y líneas horizontales que avanzan hacia el jugador
        for (int i = 0; i < 9; i++) {
            float z = (i / 9f + tiempo * 0.18f) % 1f;      // 0 = horizonte, 1 = abajo
            float y = HORIZONTE * (1f - z * z);
            s.setColor(0.13f, 0.7f, 1f, 0.06f + 0.3f * z);
            s.line(0, y, ANCHO_FONDO, y);
        }
        s.setColor(1f, 0.55f, 0.2f, 0.8f);
        s.line(0, HORIZONTE, ANCHO_FONDO, HORIZONTE);

        // Líneas de velocidad diagonales
        for (int i = 0; i < 7; i++) {
            float t = (tiempo * (0.35f + i * 0.04f) + i * 0.37f) % 1f;
            float x = ANCHO_FONDO + 60 - t * (ANCHO_FONDO + 200);
            float y = HORIZONTE + 20 + (i * 37) % (ALTO_FONDO - HORIZONTE - 30);
            s.setColor(i % 2 == 0 ? 0.13f : 1f, i % 2 == 0 ? 0.88f : 0.5f, i % 2 == 0 ? 1f : 0.15f, 0.35f);
            s.line(x, y, x + 50, y + 9);
        }

        // Anillo expansivo de la explosión
        if (edadExplosion < 0.9f) {
            float k = edadExplosion / 0.9f;
            float a = 1f - k;
            if (explosionCian) s.setColor(0.4f, 0.95f, 1f, a);
            else s.setColor(1f, 0.6f, 0.15f, a);
            s.circle(explosionX, explosionY, 6 + k * 60, 40);
            s.circle(explosionX, explosionY, 3 + k * 38, 32);
        }

        // Líneas de barrido (efecto de pantalla)
        s.setColor(0f, 0f, 0f, 0.13f);
        for (float y = 0; y < ALTO_FONDO; y += 3) s.line(0, y, ANCHO_FONDO, y);
        s.end();
    }

    /** Dibuja los brillos (luces) del fondo. Se llama entre batch.begin y batch.end. */
    public void dibujarBrillos(SpriteBatch batch, Texture brillo) {
        // Sol de energía en el horizonte, que late
        float pulso = 0.85f + 0.15f * MathUtils.sin(tiempo * 2.2f);
        DibujoUi.brillo(batch, brillo, ANCHO_FONDO / 2f, HORIZONTE, 260 * pulso, KitUi.NARANJA, 0.35f);
        DibujoUi.brillo(batch, brillo, ANCHO_FONDO / 2f, HORIZONTE, 120 * pulso, KitUi.AMARILLO, 0.25f);
        // Destello de la explosión
        if (edadExplosion < 0.5f) {
            float k = edadExplosion / 0.5f;
            DibujoUi.brillo(batch, brillo, explosionX, explosionY, 40 + k * 90, explosionCian ? KitUi.CIAN : KitUi.NARANJA, 0.9f * (1 - k));
        }
    }
}
