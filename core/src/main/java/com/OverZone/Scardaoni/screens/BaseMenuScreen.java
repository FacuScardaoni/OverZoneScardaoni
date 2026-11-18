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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.Main;

/**
 * Clase base ABSTRACTA de las pantallas de menú (menú principal, controles,
 * pausa, conexión, sala, resultados y mensajes).
 *
 * Todas necesitaban lo mismo: cámara fija de 480x270, pincel, formas,
 * fuentes, botones y liberar recursos. Antes ese código estaba copiado en
 * cada pantalla; ahora está acá una sola vez y cada pantalla hereda de esta.
 *
 * render() define el ORDEN de los pasos y cada pantalla completa los pasos
 * que le son propios (update, drawShapes, drawText). Como la clase no sabe
 * qué dibujar, esos métodos son abstractos.
 */
public abstract class BaseMenuScreen implements Screen {

    protected static final float WIDTH = 480f;
    protected static final float HEIGHT = 270f;

    protected final Main game;
    protected final OrthographicCamera camera = new OrthographicCamera();
    protected final Viewport viewport = new FitViewport(WIDTH, HEIGHT, camera);
    protected final SpriteBatch batch = new SpriteBatch();
    protected final ShapeRenderer shapes = new ShapeRenderer();
    protected final BitmapFont titleFont = new BitmapFont();
    protected final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    protected final Color background = new Color(0.05f, 0.06f, 0.1f, 1f);
    private boolean disposed = false;

    protected BaseMenuScreen(Main game, float titleScale) {
        this.game = game;
        camera.position.set(WIDTH / 2f, HEIGHT / 2f, 0);
        titleFont.getData().setScale(titleScale);
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

    /** Dibuja rectángulos, botones y barras (entre shapes.begin y shapes.end). */
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

        Gdx.gl.glClearColor(background.r, background.g, background.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        Vector3 mouse = toWorld(Gdx.input.getX(), Gdx.input.getY());

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawShapes(mouse);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
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
        titleFont.dispose();
        font.dispose();
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

    /** Botón rectangular que se aclara cuando el mouse pasa por encima. */
    protected void drawButton(Rectangle rect, Vector3 mouse, float r, float g, float b) {
        boolean hover = rect.contains(mouse.x, mouse.y);
        float boost = hover ? 0.15f : 0f;
        shapes.setColor(r + boost, g + boost, b + boost, 1f);
        shapes.rect(rect.x, rect.y, rect.width, rect.height);
    }

    /** Escribe el texto de un botón centrado dentro de él. */
    protected void drawButtonLabel(Rectangle rect, String text) {
        font.setColor(Color.WHITE);
        layout.setText(font, text);
        font.draw(batch, text, rect.x + (rect.width - layout.width) / 2f, rect.y + (rect.height + layout.height) / 2f);
    }

    /** Escribe un texto centrado horizontalmente en x. */
    protected void drawCentered(BitmapFont f, String text, float x, float y) {
        layout.setText(f, text);
        f.draw(batch, text, x - layout.width / 2f, y);
    }
}
