package com.OverZone.Scardaoni.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.OverZone.Scardaoni.JuegoOverZone;

public class LanzadorEscritorio {
    public static void main(String[] argumentos) {
        if (StartupHelper.startNewJvmIfRequired()) return;
        crearAplicacion();
    }

    private static Lwjgl3Application crearAplicacion() {
        return new Lwjgl3Application(new JuegoOverZone(), getConfiguracion());
    }

    private static Lwjgl3ApplicationConfiguration getConfiguracion() {
        Lwjgl3ApplicationConfiguration configuracion = new Lwjgl3ApplicationConfiguration();
        configuracion.setTitle("OverZone");
        configuracion.useVsync(true);
        configuracion.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        configuracion.setWindowedMode(640, 480);
        configuracion.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuracion;
    }
}
