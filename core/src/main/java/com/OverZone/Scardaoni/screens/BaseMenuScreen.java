package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.ui.MenuBackground;
import com.OverZone.Scardaoni.ui.UiDraw;
import com.OverZone.Scardaoni.ui.UiKit;

/**
 * Clase base ABSTRACTA de las pantallas de menú (menú principal, controles,
 * pausa, conexión, sala, resultados y mensajes).
 *
 * Todas comparten el mismo estilo visual: fondo animado, botones con las
 * esquinas cortadas y brillo de neón, paneles translúcidos y las fuentes del
 * juego. Ese código está acá una sola vez y cada pantalla hereda de esta.
 *
 * render() define el ORDEN de los pasos y cada pantalla completa los pasos
 * que le son propios (update, drawShapes, drawText). Como la clase no sabe
 * qué dibujar, esos métodos son abstractos.
 */
public abstract class BaseMenuScreen implements Screen {

    protected static final float WIDTH = 480f;
    protected static final float HEIGHT = 270f;
    private static final float CUT = 7f;

    protected final Main game;
    protected final UiKit ui;
    protected final OrthographicCamera camera = new OrthographicCamera();
    protected final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);
    protected final SpriteBatch batch = new SpriteBatch();
    protected final ShapeRenderer shapes = new ShapeRenderer();
    protected final MenuBackground backdrop = new MenuBackground();

    // Fuentes compartidas (las crea y libera Main, a través de UiKit)
    protected final BitmapFont hugeFont;
    protected final BitmapFont titleFont;
    protected final BitmapFont font;
    protected final BitmapFont smallFont;

    protected final GlyphLayout layout = new GlyphLayout();
    /** Cuánto "encendido" está cada botón (0 a 1), para animar el paso del mouse. */
    private final ObjectFloatMap<Rectangle> hover = new ObjectFloatMap<>();
    protected float time = 0f;
    private boolean disposed = false;

    /** @param titleScale se conserva por compatibilidad; los títulos usan la fuente del juego. */
    protected BaseMenuScreen(Main game, float titleScale) {
        this.game = game;
        this.ui = game.getUi();
        this.hugeFont = ui.huge;
        this.titleFont = ui.title;
        this.font = ui.body;
        this.smallFont = ui.small;
        camera.position.set(WIDTH / 2f, HEIGHT / 2f, 0);
        // Permite cambiar entre relleno y contorno sin cerrar el ShapeRenderer
        // (lo usan los bordes de los botones y el fondo animado).
        shapes.setAutoShapeType(true);
    }

    // ------------------------------------------------------------------
    // Pasos que completa cada pantalla
    // ------------------------------------------------------------------

    /** Quién escucha el teclado y el mouse mientras esta pantalla está activa. */
    protected abstract InputProcessor createInputProcessor();

    /**
     * Lógica de la pantalla en este frame. Devuelve true si cambió de
     * pantalla: en ese caso no se dibuja nada más.
     */
    protected boolean update(float delta) {
        return false;
    }

    /** Dibuja paneles, botones y barras (entre shapes.begin y shapes.end, modo Filled). */
    protected abstract void drawShapes(Vector3 mouse);

    /** Dibuja textos e imágenes (entre batch.begin y batch.end). */
    protected abstract void drawText(Vector3 mouse);

    // ------------------------------------------------------------------
    // Ciclo de vida común
    // ------------------------------------------------------------------

    @Override
    public void show() {
        Gdx.input.setInputProcessor(createInputProcessor());
    }

    @Override
    public final void render(float delta) {
        if (update(delta)) return;
        time += delta;
        backdrop.update(delta);

        Gdx.gl.glClearColor(0.02f, 0.03f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        Vector3 mouse = toWorld(Gdx.input.getX(), Gdx.input.getY());

        // 1. Fondo animado
        shapes.setProjectionMatrix(camera.combined);
        backdrop.drawShapes(shapes);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        backdrop.drawGlows(batch, ui.glow);
        batch.end();

        // 2. Paneles y botones de la pantalla
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawShapes(mouse);
        shapes.end();

        // 3. Textos e imágenes
        batch.begin();
        drawText(mouse);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        batch.dispose();
        shapes.dispose();
        // Las fuentes no se liberan acá: son compartidas y las libera Main.
    }

    // ------------------------------------------------------------------
    // Ayudas para dibujar
    // ------------------------------------------------------------------

    /** Convierte una posición de la ventana (píxeles) a coordenadas de la pantalla. */
    protected Vector3 toWorld(int screenX, int screenY) {
        return viewport.unproject(new Vector3(screenX, screenY, 0));
    }

    protected boolean clicked(Rectangle button, int screenX, int screenY) {
        Vector3 world = toWorld(screenX, screenY);
        return button.contains(world.x, world.y);
    }

    /** Botón de neón con el color de acento indicado. */
    protected void drawButton(Rectangle rect, Vector3 mouse, float r, float g, float b) {
        drawButton(rect, mouse, r, g, b, false);
    }

    /**
     * Botón de neón: panel oscuro con esquinas cortadas, borde y barra del color
     * de acento, y un brillo que se enciende y el botón se desliza un poco
     * cuando el mouse pasa por encima (o si está seleccionado con el teclado).
     */
    protected void drawButton(Rectangle rect, Vector3 mouse, float r, float g, float b, boolean selected) {
        boolean active = selected || rect.contains(mouse.x, mouse.y);
        float h = hover.get(rect, 0f);
        h = MathUtils.clamp(h + (active ? 0.12f : -0.08f), 0f, 1f);   // animación suave
        hover.put(rect, h);

        float slide = 3f * h;
        float x = rect.x + slide;
        Color accent = new Color(r, g, b, 1f);
        // Acento más vivo: los colores que vienen de las pantallas suelen ser apagados.
        float max = Math.max(r, Math.max(g, b));
        if (max > 0) accent.set(r / max, g / max, b / max, 1f);

        if (h > 0) UiDraw.chamferGlow(shapes, x, rect.y, rect.width, rect.height, CUT, accent, h);

        shapes.setColor(0.04f + accent.r * 0.18f * (0.4f + h), 0.05f + accent.g * 0.18f * (0.4f + h),
            0.09f + accent.b * 0.18f * (0.4f + h), 0.92f);
        UiDraw.chamfer(shapes, x, rect.y, rect.width, rect.height, CUT);

        // Barra de color a la izquierda
        shapes.setColor(accent);
        shapes.rect(x + 2, rect.y + 3, 2.5f, rect.height - 6);

        shapes.setColor(accent.r, accent.g, accent.b, 0.55f + 0.45f * h);
        UiDraw.chamferOutline(shapes, x, rect.y, rect.width, rect.height, CUT);
    }

    /** Escribe el texto de un botón centrado dentro de él (en mayúsculas). */
    protected void drawButtonLabel(Rectangle rect, String text) {
        float slide = 3f * hover.get(rect, 0f);
        String upper = text.toUpperCase();
        font.setColor(Color.WHITE);
        layout.setText(font, upper);
        font.draw(batch, upper, rect.x + slide + (rect.width - layout.width) / 2f,
            rect.y + (rect.height + layout.height) / 2f);
    }

    /** Panel translúcido con esquinas cortadas y borde de color. */
    protected void drawPanel(float x, float y, float w, float h, Color accent) {
        shapes.setColor(0.03f, 0.05f, 0.1f, 0.78f);
        UiDraw.chamfer(shapes, x, y, w, h, CUT + 3);
        shapes.setColor(accent.r, accent.g, accent.b, 0.25f);
        shapes.rect(x + 6, y + h - 2, w * 0.4f, 1.5f);     // línea de detalle arriba
        shapes.setColor(accent.r, accent.g, accent.b, 0.7f);
        UiDraw.chamferOutline(shapes, x, y, w, h, CUT + 3);
    }

    /**
     * Título con efecto de "aberración cromática": copias desplazadas en
     * naranja y cian detrás del texto principal.
     */
    protected void drawTitle(String text, float x, float y, Color color) {
        drawTitle(titleFont, text, x, y, color);
    }

    protected void drawTitle(BitmapFont f, String text, float x, float y, Color color) {
        String upper = text.toUpperCase();
        layout.setText(f, upper);
        float left = x - layout.width / 2f;
        float jitter = 0.6f * MathUtils.sin(time * 9f);
        f.setColor(1f, 0.45f, 0.08f, 0.85f);
        f.draw(batch, upper, left + 1.4f + jitter, y - 1.2f);
        f.setColor(0.13f, 0.88f, 1f, 0.85f);
        f.draw(batch, upper, left - 1.4f - jitter, y + 1.2f);
        f.setColor(color);
        f.draw(batch, upper, left, y);
    }

    /**
     * Escribe un texto en (x, y) achicando la letra si no entra en el ancho
     * indicado. Sirve para nombres largos dentro de tarjetas angostas.
     */
    protected void drawFitted(BitmapFont f, String text, float x, float y, float maxWidth) {
        float scaleX = f.getData().scaleX, scaleY = f.getData().scaleY;
        layout.setText(f, text);
        if (layout.width > maxWidth) {
            float k = maxWidth / layout.width;
            f.getData().setScale(scaleX * k, scaleY * k);
        }
        f.draw(batch, text, x, y);
        f.getData().setScale(scaleX, scaleY);
    }

    /** Escribe un texto centrado horizontalmente en x. */
    protected void drawCentered(BitmapFont f, String text, float x, float y) {
        layout.setText(f, text);
        f.draw(batch, text, x - layout.width / 2f, y);
    }
}
