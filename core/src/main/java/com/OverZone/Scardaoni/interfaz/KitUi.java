package com.OverZone.Scardaoni.interfaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.OverZone.Scardaoni.compartido.TipoPersonaje;
import com.OverZone.Scardaoni.compartido.ModoJuego;
import com.OverZone.Scardaoni.compartido.TipoPotenciador;
import com.OverZone.Scardaoni.compartido.Equipo;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

public class KitUi implements Disposable {
    /** Caracteres que se incluyen en las fuentes: los básicos más los del castellano. */
    private static final String CARACTERES = FreeTypeFontGenerator.DEFAULT_CHARS + "áéíóúÁÉÍÓÚñÑüÜ¿¡°·–";
    private static final Color BORDE = new Color(0.02f, 0.03f, 0.07f, 1f);

    // --- Paleta del juego ---
    public static final Color CIAN = new Color(0.13f, 0.88f, 1f, 1f);
    public static final Color NARANJA = new Color(1f, 0.45f, 0.08f, 1f);
    public static final Color AMARILLO = new Color(1f, 0.86f, 0.2f, 1f);
    public static final Color ROJO = new Color(1f, 0.25f, 0.3f, 1f);
    public static final Color EQUIPO_AZUL = new Color(0.40f, 0.65f, 1f, 1f);
    public static final Color EQUIPO_ROJO = new Color(1f, 0.42f, 0.42f, 1f);
    public static final Color VERDE = new Color(0.35f, 1f, 0.55f, 1f);
    public static final Color CARMESI = new Color(1f, 0.22f, 0.36f, 1f);
    public static final Color COLOR_BOTIQUIN = new Color(0.35f, 1f, 0.55f, 1f);
    public static final Color COLOR_TURBO = new Color(1f, 0.84f, 0.24f, 1f);
    public static final Color COLOR_SOBRECARGA = new Color(1f, 0.32f, 0.2f, 1f);
    public static final Color COLOR_BLINDAJE = new Color(0.32f, 0.82f, 1f, 1f);

    public static final Color[] COLORES_TODOS_CONTRA_TODOS = {
        EQUIPO_AZUL, EQUIPO_ROJO, new Color(0.40f, 0.95f, 0.50f, 1f), new Color(0.85f, 0.50f, 1f, 1f)
    };

    /** Títulos enormes (VICTORIA, ELIMINADO). */
    public final BitmapFont enorme;
    /** Títulos de pantalla. */
    public final BitmapFont titulo;
    /** Texto normal y botones. */
    public final BitmapFont cuerpo;
    /** Texto chico. */
    public final BitmapFont chica;
    /** Nombres sobre los jugadores, con borde para leerse sobre el mapa. */
    public final BitmapFont etiqueta;

    public final Texture brillo;
    public final Texture logo;
    public final Texture botonAtaque;
    /** Ícono de cada Power-Up, en el orden de TipoPotenciador. */
    private final Texture[] iconosPotenciadores;

    /** Botón amarillo de súper de cada personaje, en el orden de TipoPersonaje. */
    private final Texture[] botonesSuper;

    public KitUi() {
        FreeTypeFontGenerator titular = new FreeTypeFontGenerator(Gdx.files.internal("fonts/RussoOne-Regular.ttf"));
        FreeTypeFontGenerator negrita = new FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-Bold.ttf"));
        FreeTypeFontGenerator semiNegrita = new FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-SemiBold.ttf"));

        enorme = crear(titular, 84, 3, 0.5f);
        titulo = crear(titular, 46, 2, 0.5f);
        cuerpo = crear(semiNegrita, 26, 0, 0.5f);
        chica = crear(semiNegrita, 21, 0, 0.5f);
        etiqueta = crear(negrita, 22, 2, 0.42f);

        // Ya generadas las fuentes, los generadores no se necesitan más.
        titular.dispose();
        negrita.dispose();
        semiNegrita.dispose();

        brillo = lineal("ui/glow.png");
        logo = lineal("ui/logo.png");
        botonAtaque = lineal("ui/btn_attack.png");
        TipoPotenciador[] tiposPotenciador = TipoPotenciador.values();
        iconosPotenciadores = new Texture[tiposPotenciador.length];
        for (TipoPotenciador t : tiposPotenciador) {
            iconosPotenciadores[t.id()] = lineal("powerup_" + t.name().toLowerCase() + ".png");
        }
        botonesSuper = new Texture[] {
            lineal("ui/btn_super_volt.png"),
            lineal("ui/btn_super_brutus.png"),
            lineal("ui/btn_super_iris.png"),
            lineal("ui/btn_super_phantom.png")
        };
    }

    public Texture iconoPotenciador(TipoPotenciador tipo) {
        return iconosPotenciadores[tipo.id()];
    }

    /** Color de cada Power-Up (aura del jugador, textos y HUD). */
    public static Color colorPotenciador(TipoPotenciador tipo) {
        switch (tipo) {
            case BOTIQUIN: return COLOR_BOTIQUIN;
            case TURBO: return COLOR_TURBO;
            case SOBRECARGA: return COLOR_SOBRECARGA;
            default: return COLOR_BLINDAJE;
        }
    }

    public Texture botonSuper(TipoPersonaje tipo) {
        return botonesSuper[tipo.id()];
    }

    /** Color que identifica a cada personaje en la interfaz. */
    public static Color colorPersonaje(TipoPersonaje tipo) {
        switch (tipo) {
            case BRUTUS: return NARANJA;
            case IRIS: return VERDE;
            case PHANTOM: return CARMESI;
            default: return CIAN;
        }
    }

    public static Color colorJugador(ModoJuego modo, int equipo) {
        if (modo == ModoJuego.EQUIPOS) return equipo == Equipo.AZUL ? EQUIPO_AZUL : EQUIPO_ROJO;
        return COLORES_TODOS_CONTRA_TODOS[Math.floorMod(equipo, COLORES_TODOS_CONTRA_TODOS.length)];
    }

    private static BitmapFont crear(FreeTypeFontGenerator generador, int tamanio, float borde, float escala) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = tamanio;
        p.characters = CARACTERES;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        if (borde > 0) {
            p.borderWidth = borde;
            p.borderColor = BORDE;
        }
        BitmapFont fuente = generador.generateFont(p);
        fuente.getData().setScale(escala);
        // Con la fuente escalada, posiciones con decimales evitan que "tiemble".
        fuente.setUseIntegerPositions(false);
        return fuente;
    }

    /** Textura con filtro suave (las imágenes de la interfaz se agrandan y achican). */
    private static Texture lineal(String archivo) {
        Texture t = new Texture(Gdx.files.internal(archivo));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    @Override
    public void dispose() {
        enorme.dispose();
        titulo.dispose();
        cuerpo.dispose();
        chica.dispose();
        etiqueta.dispose();
        brillo.dispose();
        logo.dispose();
        botonAtaque.dispose();
        for (Texture t : botonesSuper) t.dispose();
        for (Texture t : iconosPotenciadores) t.dispose();
    }
}
