package com.OverZone.Scardaoni.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.JuegoOverZone;
import com.OverZone.Scardaoni.interfaz.DibujoUi;
import com.OverZone.Scardaoni.interfaz.KitUi;

public class PantallaMenu extends PantallaMenuBase {
    private final Rectangle botonJugar     = new Rectangle(160, 118, 160, 32);
    private final Rectangle botonControles = new Rectangle(172, 80, 136, 26);
    private final Rectangle botonSalirJuego     = new Rectangle(172, 48, 136, 26);
    private final Rectangle[] botones = {botonJugar, botonControles, botonSalirJuego};
    /** Botón elegido con el teclado (0 = Jugar). */
    private int seleccionado = 0;

    private final Texture hojaVolt = new Texture(Gdx.files.internal("volt_walk.png"));
    private final Texture hojaBrutus = new Texture(Gdx.files.internal("brutus_walk.png"));
    private final TextureRegion volt = new TextureRegion(hojaVolt, 0, 64, 32, 32);     // mirando a la derecha
    private final TextureRegion brutus = new TextureRegion(hojaBrutus, 0, 32, 32, 32); // mirando a la izquierda
    private final Texture hojaIris = new Texture(Gdx.files.internal("iris_walk.png"));
    private final Texture hojaPhantom = new Texture(Gdx.files.internal("phantom_walk.png"));
    private final TextureRegion iris = new TextureRegion(hojaIris, 0, 64, 32, 32);
    private final TextureRegion phantom = new TextureRegion(hojaPhantom, 0, 32, 32, 32);

    public PantallaMenu(JuegoOverZone juego) {
        super(juego, 2.2f);
    }

    @Override
    protected InputProcessor crearProcesadorEntrada() {
        return new InputAdapter() {
            @Override
            public boolean keyDown(int codigoTecla) {
                switch (codigoTecla) {
                    case Input.Keys.UP:
                    case Input.Keys.W:
                        seleccionado = (seleccionado + botones.length - 1) % botones.length;
                        return true;
                    case Input.Keys.DOWN:
                    case Input.Keys.S:
                        seleccionado = (seleccionado + 1) % botones.length;
                        return true;
                    case Input.Keys.ENTER:
                    case Input.Keys.SPACE:
                        activar(seleccionado);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean mouseMoved(int pantallaX, int pantallaY) {
                for (int i = 0; i < botones.length; i++) {
                    if (clickeado(botones[i], pantallaX, pantallaY)) seleccionado = i;
                }
                return false;
            }

            @Override
            public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
                if (boton != Input.Buttons.LEFT) return false;
                for (int i = 0; i < botones.length; i++) {
                    if (clickeado(botones[i], pantallaX, pantallaY)) {
                        activar(i);
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private void activar(int indice) {
        switch (indice) {
            case 0:
                juego.reemplazarPantalla(new PantallaConexion(juego));
                break;
            case 1:
                // setScreen (y no reemplazarPantalla) porque se vuelve a este menú.
                juego.setScreen(new PantallaControles(juego, this));
                break;
            default:
                Gdx.app.exit();
        }
    }

    @Override
    protected void dibujarFormas(Vector3 mouse) {
        dibujarBoton(botonJugar, mouse, 1f, 0.45f, 0.08f, seleccionado == 0);
        dibujarBoton(botonControles, mouse, 0.13f, 0.88f, 1f, seleccionado == 1);
        dibujarBoton(botonSalirJuego, mouse, 1f, 0.25f, 0.3f, seleccionado == 2);
    }

    @Override
    protected void dibujarTexto(Vector3 mouse) {
        // Personajes a los costados, sobre una luz de color, flotando suave
        float vaivenV = MathUtils.sin(tiempo * 2.4f) * 2f;
        float vaivenB = MathUtils.sin(tiempo * 2.0f + 1.3f) * 1.5f;
        DibujoUi.brillo(batch, interfaz.brillo, 70, 70, 150, KitUi.CIAN, 0.45f);
        DibujoUi.brillo(batch, interfaz.brillo, 410, 70, 150, KitUi.NARANJA, 0.45f);
        // Iris y Phantom un poco más atrás (más chicos y oscuros)
        batch.setColor(0.7f, 0.7f, 0.75f, 1f);
        batch.draw(iris, 70, 58 - vaivenB, 64, 64);
        batch.draw(phantom, 346, 58 - vaivenV, 64, 64);
        batch.setColor(Color.WHITE);
        batch.draw(volt, 22, 28 + vaivenV, 96, 96);
        batch.draw(brutus, 362, 28 + vaivenB, 96, 96);
        fuenteChica.setColor(KitUi.CIAN);
        dibujarCentrado(fuenteChica, "VOLT · ASALTO", 70, 26);
        fuenteChica.setColor(KitUi.NARANJA);
        dibujarCentrado(fuenteChica, "BRUTUS · TANQUE", 410, 26);

        // Logo con un leve latido
        float pulso = 1f + 0.012f * MathUtils.sin(tiempo * 3f);
        float logoAncho = 310 * pulso, logoAlto = logoAncho * interfaz.logo.getHeight() / interfaz.logo.getWidth();
        batch.draw(interfaz.logo, ANCHO / 2f - logoAncho / 2f, 172 - (logoAlto - 73) / 2f, logoAncho, logoAlto);

        fuenteChica.setColor(0.75f, 0.85f, 1f, 1f);
        dibujarCentrado(fuenteChica, "ARENA MULTIJUGADOR  ·  RED LOCAL UDP", ANCHO / 2f, 172);

        dibujarTextoBoton(botonJugar, "Jugar en red");
        dibujarTextoBoton(botonControles, "Controles");
        dibujarTextoBoton(botonSalirJuego, "Salir");

        fuenteChica.setColor(0.7f, 0.75f, 0.85f, 1f);
        dibujarCentrado(fuenteChica, "Flechas para elegir  ·  ENTER para confirmar", ANCHO / 2f, 16);
    }

    @Override
    public void dispose() {
        super.dispose();
        hojaVolt.dispose();
        hojaBrutus.dispose();
        hojaIris.dispose();
        hojaPhantom.dispose();
    }
}
