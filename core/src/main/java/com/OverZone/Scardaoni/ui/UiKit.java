package com.OverZone.Scardaoni.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.OverZone.Scardaoni.shared.CharacterType;
import com.OverZone.Scardaoni.shared.GameMode;
import com.OverZone.Scardaoni.shared.Team;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

/**
 * Recursos de la interfaz que comparten todas las pantallas: fuentes,
 * logo, botones del HUD y la textura de brillo.
 *
 * Se crean una sola vez en Main y se liberan al cerrar el juego, en lugar de
 * que cada pantalla cargue los suyos.
 *
 * Las fuentes se generan al arrancar a partir de archivos .ttf con la
 * extensión FreeType de LibGDX. Se generan al doble del tamaño necesario y se
 * dibujan a la mitad (escala 0,5) para que se vean nítidas aunque se agrande
 * la ventana. Fuentes: Russo One (títulos) y Chakra Petch (textos), ambas con
 * licencia libre OFL (ver assets/fonts).
 */
public class UiKit implements Disposable {

    /** Caracteres que se incluyen en las fuentes: los básicos más los del castellano. */
    private static final String CHARACTERS = FreeTypeFontGenerator.DEFAULT_CHARS + "áéíóúÁÉÍÓÚñÑüÜ¿¡°·–";
    private static final Color BORDER = new Color(0.02f, 0.03f, 0.07f, 1f);

    // --- Paleta del juego ---
    public static final Color CYAN = new Color(0.13f, 0.88f, 1f, 1f);
    public static final Color ORANGE = new Color(1f, 0.45f, 0.08f, 1f);
    public static final Color YELLOW = new Color(1f, 0.86f, 0.2f, 1f);
    public static final Color RED = new Color(1f, 0.25f, 0.3f, 1f);
    public static final Color BLUE_TEAM = new Color(0.40f, 0.65f, 1f, 1f);
    public static final Color RED_TEAM = new Color(1f, 0.42f, 0.42f, 1f);
    public static final Color GREEN = new Color(0.35f, 1f, 0.55f, 1f);
    public static final Color CRIMSON = new Color(1f, 0.22f, 0.36f, 1f);

    /**
     * Colores de los jugadores en Todos contra todos (el "team" de cada uno es
     * su número de color). Los dos primeros coinciden con azul y rojo.
     */
    public static final Color[] FFA_COLORS = {
        BLUE_TEAM, RED_TEAM, new Color(0.40f, 0.95f, 0.50f, 1f), new Color(0.85f, 0.50f, 1f, 1f)
    };

    /** Títulos enormes (VICTORIA, ELIMINADO). */
    public final BitmapFont huge;
    /** Títulos de pantalla. */
    public final BitmapFont title;
    /** Texto normal y botones. */
    public final BitmapFont body;
    /** Texto chico. */
    public final BitmapFont small;
    /** Nombres sobre los jugadores, con borde para leerse sobre el mapa. */
    public final BitmapFont label;

    public final Texture glow;
    public final Texture logo;
    public final Texture attackButton;
    /** Botón amarillo de súper de cada personaje, en el orden de CharacterType. */
    private final Texture[] superButtons;

    public UiKit() {
        FreeTypeFontGenerator display = new FreeTypeFontGenerator(Gdx.files.internal("fonts/RussoOne-Regular.ttf"));
        FreeTypeFontGenerator bold = new FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-Bold.ttf"));
        FreeTypeFontGenerator semi = new FreeTypeFontGenerator(Gdx.files.internal("fonts/ChakraPetch-SemiBold.ttf"));

        huge = make(display, 84, 3, 0.5f);
        title = make(display, 46, 2, 0.5f);
        body = make(semi, 26, 0, 0.5f);
        small = make(semi, 21, 0, 0.5f);
        label = make(bold, 22, 2, 0.42f);

        // Ya generadas las fuentes, los generadores no se necesitan más.
        display.dispose();
        bold.dispose();
        semi.dispose();

        glow = linear("ui/glow.png");
        logo = linear("ui/logo.png");
        attackButton = linear("ui/btn_attack.png");
        superButtons = new Texture[] {
            linear("ui/btn_super_volt.png"),
            linear("ui/btn_super_brutus.png"),
            linear("ui/btn_super_iris.png"),
            linear("ui/btn_super_phantom.png")
        };
    }

    public Texture superButton(CharacterType type) {
        return superButtons[type.id()];
    }

    /** Color que identifica a cada personaje en la interfaz. */
    public static Color characterColor(CharacterType type) {
        switch (type) {
            case BRUTUS: return ORANGE;
            case IRIS: return GREEN;
            case PHANTOM: return CRIMSON;
            default: return CYAN;
        }
    }

    /**
     * Color de un jugador según el modo: en Equipos, azul o rojo; en Todos
     * contra todos, el color propio de cada uno.
     */
    public static Color playerColor(GameMode mode, int team) {
        if (mode == GameMode.TEAMS) return team == Team.BLUE ? BLUE_TEAM : RED_TEAM;
        return FFA_COLORS[Math.floorMod(team, FFA_COLORS.length)];
    }

    private static BitmapFont make(FreeTypeFontGenerator generator, int size, float border, float scale) {
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = size;
        p.characters = CHARACTERS;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        if (border > 0) {
            p.borderWidth = border;
            p.borderColor = BORDER;
        }
        BitmapFont font = generator.generateFont(p);
        font.getData().setScale(scale);
        // Con la fuente escalada, posiciones con decimales evitan que "tiemble".
        font.setUseIntegerPositions(false);
        return font;
    }

    /** Textura con filtro suave (las imágenes de la interfaz se agrandan y achican). */
    private static Texture linear(String file) {
        Texture t = new Texture(Gdx.files.internal(file));
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    @Override
    public void dispose() {
        huge.dispose();
        title.dispose();
        body.dispose();
        small.dispose();
        label.dispose();
        glow.dispose();
        logo.dispose();
        attackButton.dispose();
        for (Texture t : superButtons) t.dispose();
    }
}
