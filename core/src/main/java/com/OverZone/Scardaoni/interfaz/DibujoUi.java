package com.OverZone.Scardaoni.interfaz;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Funciones de dibujo del estilo visual del juego: paneles con las esquinas
 * cortadas en diagonal (típicos de las interfaces futuristas) y brillos.
 */
public final class DibujoUi {

    private DibujoUi() { }

    /**
     * Rectángulo relleno con la esquina superior izquierda y la inferior
     * derecha cortadas en diagonal. Se arma con triángulos desde el centro.
     * Requiere ShapeRenderer en modo Filled.
     */
    public static void chaflan(ShapeRenderer s, float x, float y, float w, float h, float corte) {
        float[] v = vertices(x, y, w, h, corte);
        float cx = x + w / 2f, cy = y + h / 2f;
        for (int i = 0; i < v.length; i += 2) {
            int j = (i + 2) % v.length;
            s.triangle(cx, cy, v[i], v[i + 1], v[j], v[j + 1]);
        }
    }

    /** Contorno del mismo rectángulo biselado. Cambia a modo Line y vuelve a Filled. */
    public static void contornoChaflan(ShapeRenderer s, float x, float y, float w, float h, float corte) {
        s.set(ShapeRenderer.ShapeType.Line);
        float[] v = vertices(x, y, w, h, corte);
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
    public static void brilloChaflan(ShapeRenderer s, float x, float y, float w, float h, float corte,
                                   Color color, float intensidad) {
        for (int i = 3; i >= 1; i--) {
            float crecer = i * 2.2f;
            s.setColor(color.r, color.g, color.b, 0.07f * intensidad * (4 - i));
            chaflan(s, x - crecer, y - crecer, w + crecer * 2, h + crecer * 2, corte + crecer * 0.6f);
        }
    }

    private static float[] vertices(float x, float y, float w, float h, float corte) {
        return new float[] {
            x, y,
            x + w - corte, y,
            x + w, y + corte,
            x + w, y + h,
            x + corte, y + h,
            x, y + h - corte
        };
    }

    /**
     * Dibuja la textura de brillo centrada en (x, y) con mezcla "aditiva":
     * los colores se suman y aclaran lo de abajo, como una luz.
     */
    public static void brillo(SpriteBatch batch, Texture brillo, float x, float y, float tamanio, Color color, float alfa) {
        int origen = batch.getBlendSrcFunc(), destino = batch.getBlendDstFunc();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(color.r, color.g, color.b, alfa);
        batch.draw(brillo, x - tamanio / 2f, y - tamanio / 2f, tamanio, tamanio);
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(origen, destino);
    }
}
