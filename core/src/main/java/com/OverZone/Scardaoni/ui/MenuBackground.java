package com.OverZone.Scardaoni.ui;

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
public class MenuBackground {

    private static final float W = 480f, H = 270f;
    private static final float HORIZON = 96f;
    private static final int EMBERS = 70;
    private static final int MAX_SPARKS = 120;

    private static final Color TOP = new Color(0.02f, 0.03f, 0.07f, 1f);
    private static final Color BOTTOM = new Color(0.09f, 0.03f, 0.09f, 1f);

    private float time = 0f;

    // Chispas que suben lentamente
    private final float[] ex = new float[EMBERS], ey = new float[EMBERS];
    private final float[] evx = new float[EMBERS], evy = new float[EMBERS], esize = new float[EMBERS];
    private final boolean[] eorange = new boolean[EMBERS];

    // Explosión actual (una por vez) y sus esquirlas
    private float boomX, boomY, boomAge = 99f;
    private float nextBoom = 0.8f;
    private boolean boomCyan;
    private final float[] sx = new float[MAX_SPARKS], sy = new float[MAX_SPARKS];
    private final float[] svx = new float[MAX_SPARKS], svy = new float[MAX_SPARKS], slife = new float[MAX_SPARKS];

    public MenuBackground() {
        for (int i = 0; i < EMBERS; i++) resetEmber(i, true);
    }

    private void resetEmber(int i, boolean anywhere) {
        ex[i] = MathUtils.random(0f, W);
        ey[i] = anywhere ? MathUtils.random(0f, H) : MathUtils.random(-10f, 0f);
        evx[i] = MathUtils.random(-6f, 6f);
        evy[i] = MathUtils.random(10f, 34f);
        esize[i] = MathUtils.random(0.8f, 2.4f);
        eorange[i] = MathUtils.randomBoolean(0.75f);
    }

    public void update(float delta) {
        time += delta;

        for (int i = 0; i < EMBERS; i++) {
            ex[i] += (evx[i] + MathUtils.sin(time * 2 + i) * 4f) * delta;
            ey[i] += evy[i] * delta;
            if (ey[i] > H + 5) resetEmber(i, false);
        }

        boomAge += delta;
        nextBoom -= delta;
        if (nextBoom <= 0) {
            nextBoom = MathUtils.random(1.4f, 2.8f);
            boomAge = 0f;
            boomX = MathUtils.random(40f, W - 40f);
            boomY = MathUtils.random(HORIZON + 30f, H - 30f);
            boomCyan = MathUtils.randomBoolean(0.35f);
            int spawned = 0;
            for (int i = 0; i < MAX_SPARKS && spawned < 26; i++) {
                if (slife[i] > 0) continue;
                float ang = MathUtils.random(0f, MathUtils.PI2);
                float speed = MathUtils.random(40f, 130f);
                sx[i] = boomX;
                sy[i] = boomY;
                svx[i] = MathUtils.cos(ang) * speed;
                svy[i] = MathUtils.sin(ang) * speed;
                slife[i] = MathUtils.random(0.5f, 1.1f);
                spawned++;
            }
        }
        for (int i = 0; i < MAX_SPARKS; i++) {
            if (slife[i] <= 0) continue;
            slife[i] -= delta;
            sx[i] += svx[i] * delta;
            sy[i] += svy[i] * delta;
            svx[i] *= 0.97f;
            svy[i] = svy[i] * 0.97f - 60f * delta;   // un poco de gravedad
        }
    }

    /** Dibuja las formas del fondo. Abre y cierra el ShapeRenderer por su cuenta. */
    public void drawShapes(ShapeRenderer s) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        s.begin(ShapeRenderer.ShapeType.Filled);
        // Degradé: azul muy oscuro arriba, violeta con un toque naranja abajo
        s.rect(0, 0, W, H, BOTTOM, BOTTOM, TOP, TOP);
        // Resplandor del horizonte
        for (int i = 0; i < 6; i++) {
            s.setColor(1f, 0.35f, 0.1f, 0.05f);
            s.rect(0, HORIZON - 6 + i * 2, W, 14 - i * 2.2f);
        }
        // Chispas
        for (int i = 0; i < EMBERS; i++) {
            float flicker = 0.55f + 0.45f * MathUtils.sin(time * 6 + i * 1.7f);
            if (eorange[i]) s.setColor(1f, 0.55f + 0.3f * flicker, 0.15f, 0.85f * flicker);
            else s.setColor(0.4f, 0.9f, 1f, 0.7f * flicker);
            s.rect(ex[i], ey[i], esize[i], esize[i]);
        }
        // Esquirlas de la explosión
        for (int i = 0; i < MAX_SPARKS; i++) {
            if (slife[i] <= 0) continue;
            float a = Math.min(1f, slife[i] * 1.6f);
            if (boomCyan) s.setColor(0.6f, 0.95f, 1f, a);
            else s.setColor(1f, 0.75f, 0.25f, a);
            s.rect(sx[i] - 1, sy[i] - 1, 2.2f, 2.2f);
        }

        s.set(ShapeRenderer.ShapeType.Line);
        // Grilla en perspectiva: líneas que salen del punto de fuga...
        float vx = W / 2f;
        for (float x = -480; x <= W + 480; x += 40) {
            s.setColor(0.13f, 0.7f, 1f, 0.22f);
            s.line(vx, HORIZON, x, 0);
        }
        // ...y líneas horizontales que avanzan hacia el jugador
        for (int i = 0; i < 9; i++) {
            float z = (i / 9f + time * 0.18f) % 1f;      // 0 = horizonte, 1 = abajo
            float y = HORIZON * (1f - z * z);
            s.setColor(0.13f, 0.7f, 1f, 0.06f + 0.3f * z);
            s.line(0, y, W, y);
        }
        s.setColor(1f, 0.55f, 0.2f, 0.8f);
        s.line(0, HORIZON, W, HORIZON);

        // Líneas de velocidad diagonales
        for (int i = 0; i < 7; i++) {
            float t = (time * (0.35f + i * 0.04f) + i * 0.37f) % 1f;
            float x = W + 60 - t * (W + 200);
            float y = HORIZON + 20 + (i * 37) % (H - HORIZON - 30);
            s.setColor(i % 2 == 0 ? 0.13f : 1f, i % 2 == 0 ? 0.88f : 0.5f, i % 2 == 0 ? 1f : 0.15f, 0.35f);
            s.line(x, y, x + 50, y + 9);
        }

        // Anillo expansivo de la explosión
        if (boomAge < 0.9f) {
            float k = boomAge / 0.9f;
            float a = 1f - k;
            if (boomCyan) s.setColor(0.4f, 0.95f, 1f, a);
            else s.setColor(1f, 0.6f, 0.15f, a);
            s.circle(boomX, boomY, 6 + k * 60, 40);
            s.circle(boomX, boomY, 3 + k * 38, 32);
        }

        // Líneas de barrido (efecto de pantalla)
        s.setColor(0f, 0f, 0f, 0.13f);
        for (float y = 0; y < H; y += 3) s.line(0, y, W, y);
        s.end();
    }

    /** Dibuja los brillos (luces) del fondo. Se llama entre batch.begin y batch.end. */
    public void drawGlows(SpriteBatch batch, Texture glow) {
        // Sol de energía en el horizonte, que late
        float pulse = 0.85f + 0.15f * MathUtils.sin(time * 2.2f);
        UiDraw.glow(batch, glow, W / 2f, HORIZON, 260 * pulse, UiKit.ORANGE, 0.35f);
        UiDraw.glow(batch, glow, W / 2f, HORIZON, 120 * pulse, UiKit.YELLOW, 0.25f);
        // Destello de la explosión
        if (boomAge < 0.5f) {
            float k = boomAge / 0.5f;
            UiDraw.glow(batch, glow, boomX, boomY, 40 + k * 90, boomCyan ? UiKit.CYAN : UiKit.ORANGE, 0.9f * (1 - k));
        }
    }
}
