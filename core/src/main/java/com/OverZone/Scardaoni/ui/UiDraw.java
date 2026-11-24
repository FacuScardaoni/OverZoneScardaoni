package com.OverZone.Scardaoni.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Funciones de dibujo del estilo visual del juego: paneles con las esquinas
 * cortadas en diagonal (típicos de las interfaces futuristas) y brillos.
 */
public final class UiDraw {

    private UiDraw() { }

    /**
     * Rectángulo relleno con la esquina superior izquierda y la inferior
     * derecha cortadas en diagonal. Se arma con triángulos desde el centro.
     * Requiere ShapeRenderer en modo Filled.
     */
    public static void chamfer(ShapeRenderer s, float x, float y, float w, float h, float cut) {
        float[] v = vertices(x, y, w, h, cut);
        float cx = x + w / 2f, cy = y + h / 2f;
        for (int i = 0; i < v.length; i += 2) {
            int j = (i + 2) % v.length;
            s.triangle(cx, cy, v[i], v[i + 1], v[j], v[j + 1]);
        }
    }

    /** Contorno del mismo rectángulo biselado. Cambia a modo Line y vuelve a Filled. */
    public static void chamferOutline(ShapeRenderer s, float x, float y, float w, float h, float cut) {
        s.set(ShapeRenderer.ShapeType.Line);
        float[] v = vertices(x, y, w, h, cut);
        for (int i = 0; i < v.length; i += 2) {
            int j = (i + 2) % v.length;
            s.line(v[i], v[i + 1], v[j], v[j + 1]);
        }
        s.set(ShapeRenderer.ShapeType.Filled);
    }

    /**
     * Halo de luz alrededor de un rectángulo biselado: varias capas cada vez
     * más grandes y más transparentes.
     */
    public static void chamferGlow(ShapeRenderer s, float x, float y, float w, float h, float cut,
                                   Color color, float intensity) {
        for (int i = 3; i >= 1; i--) {
            float grow = i * 2.2f;
            s.setColor(color.r, color.g, color.b, 0.07f * intensity * (4 - i));
            chamfer(s, x - grow, y - grow, w + grow * 2, h + grow * 2, cut + grow * 0.6f);
        }
    }

    private static float[] vertices(float x, float y, float w, float h, float cut) {
        return new float[] {
            x, y,
            x + w - cut, y,
            x + w, y + cut,
            x + w, y + h,
            x + cut, y + h,
            x, y + h - cut
        };
    }

    /**
     * Dibuja la textura de brillo centrada en (x, y) con mezcla "aditiva":
     * los colores se suman y aclaran lo de abajo, como una luz.
     */
    public static void glow(SpriteBatch batch, Texture glow, float x, float y, float size, Color color, float alpha) {
        int src = batch.getBlendSrcFunc(), dst = batch.getBlendDstFunc();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(glow, x - size / 2f, y - size / 2f, size, size);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(src, dst);
    }
}
