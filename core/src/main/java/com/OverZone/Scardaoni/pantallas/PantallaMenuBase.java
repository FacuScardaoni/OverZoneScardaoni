package com.OverZone.Scardaoni.pantallas;

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
import com.OverZone.Scardaoni.JuegoOverZone;
import com.OverZone.Scardaoni.interfaz.FondoMenu;
import com.OverZone.Scardaoni.interfaz.DibujoUi;
import com.OverZone.Scardaoni.interfaz.KitUi;

/**
 * Clase base ABSTRACTA de las pantallas de menú (menú principal, controles,
 * pausa, conexión, sala, resultados y mensajes).
 *
 * Todas comparten el mismo estilo visual: fondo animado, botones con las
 * esquinas cortadas y brillo de neón, paneles translúcidos y las fuentes del
 * juego. Ese código está acá una sola vez y cada pantalla hereda de esta.
 *
 * render() define el ORDEN de los pasos y cada pantalla completa los pasos
 * que le son propios (update, dibujarFormas, dibujarTexto). Como la clase no sabe
 * qué dibujar, esos métodos son abstractos.
 */
public abstract class PantallaMenuBase implements Screen {

    protected static final float ANCHO = 480f;
    protected static final float ALTO = 270f;
    private static final float CORTE = 7f;

    protected final JuegoOverZone juego;
    protected final KitUi interfaz;
    protected final OrthographicCamera camara = new OrthographicCamera();
    protected final Viewport vista = new FitViewport(ANCHO, ALTO, camara);
    protected final SpriteBatch batch = new SpriteBatch();
    protected final ShapeRenderer formas = new ShapeRenderer();
    protected final FondoMenu fondo = new FondoMenu();

    // Fuentes compartidas (las crea y libera JuegoOverZone, a través de KitUi)
    protected final BitmapFont fuenteEnorme;
    protected final BitmapFont fuenteTitulo;
    protected final BitmapFont fuente;
    protected final BitmapFont fuenteChica;

    protected final GlyphLayout medidor = new GlyphLayout();
    /** Cuánto "encendido" está cada botón (0 a 1), para animar el paso del mouse. */
    private final ObjectFloatMap<Rectangle> resaltado = new ObjectFloatMap<>();
    protected float tiempo = 0f;
    private boolean liberada = false;

    /** @param escalaTitulo se conserva por compatibilidad; los títulos usan la fuente del juego. */
    protected PantallaMenuBase(JuegoOverZone juego, float escalaTitulo) {
        this.juego = juego;
        this.interfaz = juego.getInterfaz();
        this.fuenteEnorme = interfaz.enorme;
        this.fuenteTitulo = interfaz.titulo;
        this.fuente = interfaz.cuerpo;
        this.fuenteChica = interfaz.chica;
        camara.position.set(ANCHO / 2f, ALTO / 2f, 0);
        // Permite cambiar entre relleno y contorno sin cerrar el ShapeRenderer
        // (lo usan los bordes de los botones y el fondo animado).
        formas.setAutoShapeType(true);
    }

    // ------------------------------------------------------------------
    // Pasos que completa cada pantalla
    // ------------------------------------------------------------------

    /** Quién escucha el teclado y el mouse mientras esta pantalla está activa. */
    protected abstract InputProcessor crearProcesadorEntrada();

    /**
     * Lógica de la pantalla en este frame. Devuelve true si cambió de
     * pantalla: en ese caso no se dibuja nada más.
     */
    protected boolean actualizar(float delta) {
        return false;
    }

    /** Dibuja paneles, botones y barras (entre shapes.begin y shapes.end, modo Filled). */
    protected abstract void dibujarFormas(Vector3 mouse);

    /** Dibuja textos e imágenes (entre batch.begin y batch.end). */
    protected abstract void dibujarTexto(Vector3 mouse);

    // ------------------------------------------------------------------
    // Ciclo de vida común
    // ------------------------------------------------------------------

    @Override
    public void show() {
        Gdx.input.setInputProcessor(crearProcesadorEntrada());
    }

    @Override
    public final void render(float delta) {
        if (actualizar(delta)) return;
        tiempo += delta;
        fondo.actualizar(delta);

        Gdx.gl.glClearColor(0.02f, 0.03f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camara.update();
        Vector3 mouse = aMundo(Gdx.input.getX(), Gdx.input.getY());

        // 1. Fondo animado
        formas.setProjectionMatrix(camara.combined);
        fondo.dibujarFormas(formas);
        batch.setProjectionMatrix(camara.combined);
        batch.begin();
        fondo.dibujarBrillos(batch, interfaz.brillo);
        batch.end();

        // 2. Paneles y botones de la pantalla
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        formas.begin(ShapeRenderer.ShapeType.Filled);
        dibujarFormas(mouse);
        formas.end();

        // 3. Textos e imágenes
        batch.begin();
        dibujarTexto(mouse);
        batch.end();
    }

    @Override
    public void resize(int ancho, int alto) {
        vista.update(ancho, alto, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (liberada) return;
        liberada = true;
        batch.dispose();
        formas.dispose();
        // Las fuentes no se liberan acá: son compartidas y las libera JuegoOverZone.
    }

    // ------------------------------------------------------------------
    // Ayudas para dibujar
    // ------------------------------------------------------------------

    /** Convierte una posición de la ventana (píxeles) a coordenadas de la pantalla. */
    protected Vector3 aMundo(int pantallaX, int pantallaY) {
        return vista.unproject(new Vector3(pantallaX, pantallaY, 0));
    }

    protected boolean clickeado(Rectangle boton, int pantallaX, int pantallaY) {
        Vector3 mundo = aMundo(pantallaX, pantallaY);
        return boton.contains(mundo.x, mundo.y);
    }

    /** Botón de neón con el color de acento indicado. */
    protected void dibujarBoton(Rectangle rect, Vector3 mouse, float r, float g, float b) {
        dibujarBoton(rect, mouse, r, g, b, false);
    }

    /**
     * Botón de neón: panel oscuro con esquinas cortadas, borde y barra del color
     * de acento, y un brillo que se enciende y el botón se desliza un poco
     * cuando el mouse pasa por encima (o si está seleccionado con el teclado).
     */
    protected void dibujarBoton(Rectangle rect, Vector3 mouse, float r, float g, float b, boolean seleccionado) {
        boolean activo = seleccionado || rect.contains(mouse.x, mouse.y);
        float h = resaltado.get(rect, 0f);
        h = MathUtils.clamp(h + (activo ? 0.12f : -0.08f), 0f, 1f);   // animación suave
        resaltado.put(rect, h);

        float deslizamiento = 3f * h;
        float x = rect.x + deslizamiento;
        Color acento = new Color(r, g, b, 1f);
        // Acento más vivo: los colores que vienen de las pantallas suelen ser apagados.
        float maximo = Math.max(r, Math.max(g, b));
        if (maximo > 0) acento.set(r / maximo, g / maximo, b / maximo, 1f);

        if (h > 0) DibujoUi.brilloChaflan(formas, x, rect.y, rect.width, rect.height, CORTE, acento, h);

        formas.setColor(0.04f + acento.r * 0.18f * (0.4f + h), 0.05f + acento.g * 0.18f * (0.4f + h),
            0.09f + acento.b * 0.18f * (0.4f + h), 0.92f);
        DibujoUi.chaflan(formas, x, rect.y, rect.width, rect.height, CORTE);

        // Barra de color a la izquierda
        formas.setColor(acento);
        formas.rect(x + 2, rect.y + 3, 2.5f, rect.height - 6);

        formas.setColor(acento.r, acento.g, acento.b, 0.55f + 0.45f * h);
        DibujoUi.contornoChaflan(formas, x, rect.y, rect.width, rect.height, CORTE);
    }

    /** Escribe el texto de un botón centrado dentro de él (en mayúsculas). */
    protected void dibujarTextoBoton(Rectangle rect, String texto) {
        float deslizamiento = 3f * resaltado.get(rect, 0f);
        String mayusculas = texto.toUpperCase();
        fuente.setColor(Color.WHITE);
        medidor.setText(fuente, mayusculas);
        fuente.draw(batch, mayusculas, rect.x + deslizamiento + (rect.width - medidor.width) / 2f,
            rect.y + (rect.height + medidor.height) / 2f);
    }

    /** Panel translúcido con esquinas cortadas y borde de color. */
    protected void dibujarPanel(float x, float y, float w, float h, Color acento) {
        formas.setColor(0.03f, 0.05f, 0.1f, 0.78f);
        DibujoUi.chaflan(formas, x, y, w, h, CORTE + 3);
        formas.setColor(acento.r, acento.g, acento.b, 0.25f);
        formas.rect(x + 6, y + h - 2, w * 0.4f, 1.5f);     // línea de detalle arriba
        formas.setColor(acento.r, acento.g, acento.b, 0.7f);
        DibujoUi.contornoChaflan(formas, x, y, w, h, CORTE + 3);
    }

    /**
     * Título con efecto de "aberración cromática": copias desplazadas en
     * naranja y cian detrás del texto principal.
     */
    protected void dibujarTitulo(String texto, float x, float y, Color color) {
        dibujarTitulo(fuenteTitulo, texto, x, y, color);
    }

    protected void dibujarTitulo(BitmapFont f, String texto, float x, float y, Color color) {
        String mayusculas = texto.toUpperCase();
        medidor.setText(f, mayusculas);
        float izquierda = x - medidor.width / 2f;
        float temblor = 0.6f * MathUtils.sin(tiempo * 9f);
        f.setColor(1f, 0.45f, 0.08f, 0.85f);
        f.draw(batch, mayusculas, izquierda + 1.4f + temblor, y - 1.2f);
        f.setColor(0.13f, 0.88f, 1f, 0.85f);
        f.draw(batch, mayusculas, izquierda - 1.4f - temblor, y + 1.2f);
        f.setColor(color);
        f.draw(batch, mayusculas, izquierda, y);
    }

    /**
     * Escribe un texto en (x, y) achicando la letra si no entra en el ancho
     * indicado. Sirve para nombres largos dentro de tarjetas angostas.
     */
    protected void dibujarAjustado(BitmapFont f, String texto, float x, float y, float anchoMaximo) {
        float escalaX = f.getData().scaleX, escalaY = f.getData().scaleY;
        medidor.setText(f, texto);
        if (medidor.width > anchoMaximo) {
            float k = anchoMaximo / medidor.width;
            f.getData().setScale(escalaX * k, escalaY * k);
        }
        f.draw(batch, texto, x, y);
        f.getData().setScale(escalaX, escalaY);
    }

    /** Escribe un texto centrado horizontalmente en x. */
    protected void dibujarCentrado(BitmapFont f, String texto, float x, float y) {
        medidor.setText(f, texto);
        f.draw(batch, texto, x - medidor.width / 2f, y);
    }
}
