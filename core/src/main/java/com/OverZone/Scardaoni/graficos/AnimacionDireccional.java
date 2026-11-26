package com.OverZone.Scardaoni.graficos;

import com.OverZone.Scardaoni.compartido.Direccion;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AnimacionDireccional {
    // Las direcciones coinciden con las filas del spritesheet (ver Direccion).
    public static final int ABAJO = Direccion.ABAJO;
    public static final int IZQUIERDA = Direccion.IZQUIERDA;
    public static final int DERECHA = Direccion.DERECHA;
    public static final int ARRIBA = Direccion.ARRIBA;

    private final Texture hoja;
    private final Animation<TextureRegion>[] caminataPorDireccion;
    private final TextureRegion[] quietoPorDireccion;
    private float tiempoEstado = 0f;

    @SuppressWarnings("unchecked")
    public AnimacionDireccional(Texture hoja, int anchoCuadro, int altoCuadro, int columnasCuadro, float duracionCuadro) {
        this.hoja = hoja;
        TextureRegion[][] grilla = TextureRegion.split(hoja, anchoCuadro, altoCuadro);

        caminataPorDireccion = new Animation[4];
        quietoPorDireccion = new TextureRegion[4];
        for (int direccion = 0; direccion < 4 && direccion < grilla.length; direccion++) {
            TextureRegion[] cuadros = new TextureRegion[columnasCuadro];
            System.arraycopy(grilla[direccion], 0, cuadros, 0, Math.min(columnasCuadro, grilla[direccion].length));
            caminataPorDireccion[direccion] = new Animation<>(duracionCuadro, cuadros);
            caminataPorDireccion[direccion].setPlayMode(Animation.PlayMode.LOOP);
            quietoPorDireccion[direccion] = cuadros[0];
        }
    }

    public void actualizar(float delta) {
        tiempoEstado += delta;
    }

    public TextureRegion getCuadro(int direccionMirada, boolean moviendose) {
        direccionMirada = Math.max(0, Math.min(3, direccionMirada));
        if (!moviendose) {
            return quietoPorDireccion[direccionMirada];
        }
        return caminataPorDireccion[direccionMirada].getKeyFrame(tiempoEstado);
    }

    public void dispose() {
        hoja.dispose();
    }
}
