package com.OverZone.Scardaoni.interfaz;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.compartido.TipoPersonaje;
import com.OverZone.Scardaoni.compartido.ModoJuego;
import com.OverZone.Scardaoni.compartido.TipoPotenciador;
import com.OverZone.Scardaoni.compartido.Equipo;
import com.OverZone.Scardaoni.compartido.EstadoMundo;

public class Hud {
    public static final float ANCHO_VIRTUAL = 480f;
    public static final float ALTO_VIRTUAL = 270f;

    private final KitUi interfaz;
    private final OrthographicCamera camara;
    private final Viewport vista;
    private final ShapeRenderer formas = new ShapeRenderer();
    private final GlyphLayout medidor = new GlyphLayout();
    private float tiempo = 0f;

    public Hud(KitUi interfaz) {
        this.interfaz = interfaz;
        camara = new OrthographicCamera();
        // ExtendViewport: como mínimo 480x270, y si la ventana tiene otra forma
        // se agranda. Los elementos se ubican respecto de los bordes reales
        // (arriba, derecha), así siempre quedan en las esquinas de la ventana.
        vista = new ExtendViewport(ANCHO_VIRTUAL, ALTO_VIRTUAL, camara);
        camara.position.set(ANCHO_VIRTUAL / 2f, ALTO_VIRTUAL / 2f, 0);
        // Permite cambiar entre relleno y contorno sin cerrar el ShapeRenderer.
        formas.setAutoShapeType(true);
    }

    public void resize(int ancho, int alto) {
        vista.update(ancho, alto, true);
    }

    public void render(SpriteBatch batch, EstadoMundo.EstadoJugador yo, EstadoMundo estadoActual,
                       boolean silenciado, float volumen, float pingMs, ControlesHud controles, float delta) {
        tiempo += delta;
        camara.update();
        float tope = vista.getWorldHeight();          // borde superior
        float medioX = vista.getWorldWidth() / 2f;     // centro horizontal
        float ax = controles.ataqueX(), ay = controles.ataqueY();
        float sx = controles.superX(), sy = controles.superY();
        Vector3 m = vista.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        TipoPersonaje personaje = yo == null ? TipoPersonaje.VOLT : TipoPersonaje.desdeId(yo.personaje);
        float proporcionVida = yo == null ? 0f : MathUtils.clamp(yo.vida / personaje.vidaMaxima, 0f, 1f);
        float superLista = yo == null ? 0f : 1f - yo.recargaHabilidad;
        boolean superCargada = superLista >= 0.999f;
        boolean superActiva = yo != null && yo.habilidadActiva;
        Color acento = KitUi.colorPersonaje(personaje);

        boolean todosContraTodos = estadoActual.getModo() == ModoJuego.TODOS_CONTRA_TODOS;
        EstadoMundo.EstadoJugador rival = todosContraTodos ? mejorRival(estadoActual, yo) : null;
        int puntosIzquierda, puntosDerecha;
        Color colorIzquierda, colorDerecha;
        if (todosContraTodos) {
            puntosIzquierda = yo == null ? 0 : yo.eliminaciones;
            puntosDerecha = rival == null ? 0 : rival.eliminaciones;
            colorIzquierda = yo == null ? Color.GRAY : KitUi.colorJugador(ModoJuego.TODOS_CONTRA_TODOS, yo.equipo);
            colorDerecha = rival == null ? Color.GRAY : KitUi.colorJugador(ModoJuego.TODOS_CONTRA_TODOS, rival.equipo);
        } else {
            puntosIzquierda = estadoActual.puntosAzul;
            puntosDerecha = estadoActual.puntosRojo;
            colorIzquierda = KitUi.EQUIPO_AZUL;
            colorDerecha = KitUi.EQUIPO_ROJO;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        formas.setProjectionMatrix(camara.combined);
        formas.begin(ShapeRenderer.ShapeType.Filled);

        // --- Panel del jugador (arriba a la izquierda) ---
        panel(6, tope - 56, 140, 50, acento);
        float barraX = 12, barraY = tope - 34, barraAncho = 128, barraAlto = 9;
        formas.setColor(0.08f, 0.09f, 0.13f, 1f);
        formas.rect(barraX, barraY, barraAncho, barraAlto);
        Color vidaTexto = proporcionVida > 0.5f ? new Color(0.3f, 0.95f, 0.45f, 1f)
            : proporcionVida > 0.25f ? new Color(1f, 0.8f, 0.2f, 1f) : new Color(1f, 0.25f, 0.25f, 1f);
        formas.setColor(vidaTexto);
        formas.rect(barraX, barraY, barraAncho * proporcionVida, barraAlto);
        formas.setColor(1f, 1f, 1f, 0.25f);
        formas.rect(barraX, barraY + barraAlto - 2.5f, barraAncho * proporcionVida, 2.5f);   // brillo superior
        // Marcas cada 20 puntos de vida
        formas.setColor(0.02f, 0.03f, 0.07f, 0.8f);
        for (float marcaVida = 20; marcaVida < personaje.vidaMaxima; marcaVida += 20) {
            formas.rect(barraX + barraAncho * marcaVida / personaje.vidaMaxima, barraY, 1f, barraAlto);
        }
        // Barra de recarga de la habilidad
        formas.setColor(0.08f, 0.09f, 0.13f, 1f);
        formas.rect(barraX, tope - 42, barraAncho, 4);
        formas.setColor(superActiva ? KitUi.NARANJA : (superCargada ? KitUi.AMARILLO : new Color(0.55f, 0.48f, 0.2f, 1f)));
        formas.rect(barraX, tope - 42, barraAncho * superLista, 4);

        // --- Reloj y puntajes (arriba al centro) ---
        panel(medioX - 36, tope - 34, 72, 28, Color.WHITE);
        cuadroPuntos(medioX - 82, tope - 32, colorIzquierda);
        cuadroPuntos(medioX + 42, tope - 32, colorDerecha);

        // --- Botón de súper: anillo de carga ---
        formas.setColor(0.05f, 0.05f, 0.08f, 0.85f);
        formas.circle(sx, sy, ControlesHud.RADIO_SUPER + 4, 40);
        formas.setColor(superActiva ? KitUi.NARANJA : KitUi.AMARILLO);
        if (superLista > 0) {
            formas.arc(sx, sy, ControlesHud.RADIO_SUPER + 4, 90 - 360 * superLista, 360 * superLista, 40);
        }
        // --- Botón de ataque: aro oscuro ---
        formas.setColor(0.05f, 0.05f, 0.08f, 0.85f);
        formas.circle(ax, ay, ControlesHud.RADIO_ATAQUE + 3, 48);
        formas.end();

        batch.setProjectionMatrix(camara.combined);
        batch.begin();

        // Power-Ups activos: un ícono con los segundos que le quedan, debajo del panel
        if (yo != null && yo.vivo) dibujarPotenciadores(batch, yo, tope, tiempo);

        // Brillos de los botones
        boolean sobreAtaque = controles.sobreAtaque(m.x, m.y);
        DibujoUi.brillo(batch, interfaz.brillo, ax, ay, 110,
            KitUi.ROJO, controles.ataqueApretado() ? 0.8f : (sobreAtaque ? 0.55f : 0.3f));
        if (superCargada || superActiva) {
            float pulso = 0.55f + 0.35f * MathUtils.sin(tiempo * 6f);
            DibujoUi.brillo(batch, interfaz.brillo, sx, sy, 95,
                superActiva ? KitUi.NARANJA : KitUi.AMARILLO, pulso);
        }

        // Botón de ataque (se achica un poco al presionarlo)
        float escalaAtaque = controles.ataqueApretado() ? 0.92f : (sobreAtaque ? 1.04f : 1f);
        dibujarBotonCircular(batch, interfaz.botonAtaque, ax, ay,
            ControlesHud.RADIO_ATAQUE * escalaAtaque, Color.WHITE);
        // Botón de súper: gris mientras carga, amarillo brillante cuando está listo
        Texture texturaSuper = interfaz.botonSuper(personaje);
        float escalaSuper = controles.superApretada() ? 0.9f : (controles.sobreSuper(m.x, m.y) ? 1.05f : 1f);
        Color tinteSuper = superCargada || superActiva ? Color.WHITE : new Color(0.45f, 0.45f, 0.5f, 1f);
        dibujarBotonCircular(batch, texturaSuper, sx, sy,
            ControlesHud.RADIO_SUPER * escalaSuper, tinteSuper);

        // "Perilla" del joystick mientras se arrastra
        if (controles.ataqueApretado()) perilla(batch, ax, ay, controles.getArrastreAtaque(), KitUi.ROJO);
        if (controles.superApretada()) perilla(batch, sx, sy, controles.getArrastreSuper(), KitUi.AMARILLO);

        // --- Textos ---
        BitmapFont cuerpo = interfaz.cuerpo, chica = interfaz.chica, titulo = interfaz.titulo;
        chica.setColor(acento);
        chica.draw(batch, personaje.nombreVisible.toUpperCase(), 12, tope - 11);
        cuerpo.setColor(Color.WHITE);
        String textoVida = (yo == null ? 0 : (int) Math.ceil(yo.vida)) + " / " + (int) personaje.vidaMaxima;
        medidor.setText(chica, textoVida);
        chica.setColor(Color.WHITE);
        chica.draw(batch, textoVida, 140 - medidor.width, tope - 11);
        chica.setColor(superActiva ? KitUi.NARANJA : (superCargada ? KitUi.AMARILLO : Color.GRAY));
        chica.draw(batch, personaje.nombreHabilidad + (superCargada && !superActiva ? "  ¡LISTA!" : ""), 12, tope - 45);

        float t = Math.max(0, estadoActual.tiempoFase);
        boolean ultimosSegundos = t <= 10;
        titulo.setColor(ultimosSegundos ? (((int) (tiempo * 4)) % 2 == 0 ? KitUi.ROJO : Color.WHITE) : Color.WHITE);
        centrado(batch, titulo, String.format("%d:%02d", (int) (t / 60), (int) (t % 60)), medioX, tope - 11);

        titulo.setColor(Color.WHITE);
        centrado(batch, titulo, String.valueOf(puntosIzquierda), medioX - 60, tope - 11);
        centrado(batch, titulo, String.valueOf(puntosDerecha), medioX + 64, tope - 11);
        if (yo != null && !todosContraTodos) {
            chica.setColor(yo.equipo == Equipo.AZUL ? KitUi.EQUIPO_AZUL : KitUi.EQUIPO_ROJO);
            centrado(batch, chica, "TU EQUIPO: " + Equipo.nombre(yo.equipo).toUpperCase(), medioX, tope - 40);
        } else if (yo != null) {
            // Puesto: 1 + cantidad de jugadores con más eliminaciones que yo
            int puesto = 1;
            for (EstadoMundo.EstadoJugador p : estadoActual.jugadores) {
                if (p.id != yo.id && p.eliminaciones > yo.eliminaciones) puesto++;
            }
            chica.setColor(puesto == 1 ? KitUi.AMARILLO : Color.LIGHT_GRAY);
            String nombreRival = rival == null ? "-" : rival.nombre.toUpperCase();
            centrado(batch, chica, "VOS " + puesto + "°/" + estadoActual.jugadores.size() + "  |  RIVAL: " + nombreRival,
                medioX, tope - 40);
        }

        chica.setColor(Color.GRAY);
        chica.draw(batch, (pingMs < 0 ? "Ping -" : "Ping " + Math.round(pingMs) + " ms")
            + "   " + (silenciado ? "Sonido OFF" : "Vol " + (int) (volumen * 100) + "%"), 8, 14);
        chica.draw(batch, "ESC: menú", 8, 26);

        // Eliminado: cuenta para reaparecer
        if (yo != null && !yo.vivo) {
            BitmapFont enorme = interfaz.enorme;
            float y = tope / 2f + 26;
            enorme.setColor(1f, 0.45f, 0.08f, 0.8f);
            centrado(batch, enorme, "ELIMINADO", medioX + 1.5f, y - 1.5f);
            enorme.setColor(KitUi.ROJO);
            centrado(batch, enorme, "ELIMINADO", medioX, y);
            cuerpo.setColor(Color.WHITE);
            centrado(batch, cuerpo, "Reaparecés en " + (int) Math.ceil(yo.reapareceEn) + " s", medioX, y - 40);
        }

        batch.end();
    }

    /** Fila de Power-Ups activos. Titilan en el último segundo y medio. */
    private void dibujarPotenciadores(SpriteBatch batch, EstadoMundo.EstadoJugador yo, float tope, float tiempo) {
        float x = 8;
        float y = tope - 74;
        for (TipoPotenciador tipo : TipoPotenciador.values()) {
            float tiempoRestante = yo.potenciador(tipo);
            if (!tipo.esTemporal() || tiempoRestante <= 0f) continue;
            if (tiempoRestante < 1.5f && MathUtils.sin(tiempo * 18f) < 0f) {
                x += 44;
                continue;
            }
            Color c = KitUi.colorPotenciador(tipo);
            DibujoUi.brillo(batch, interfaz.brillo, x + 8, y + 8, 30, c, 0.35f);
            batch.setColor(Color.WHITE);
            batch.draw(interfaz.iconoPotenciador(tipo), x, y, 16, 16);
            interfaz.chica.setColor(c);
            interfaz.chica.draw(batch, String.format(java.util.Locale.ROOT, "%.1f", tiempoRestante), x + 18, y + 12);
            x += 44;
        }
        batch.setColor(Color.WHITE);
    }

    private static EstadoMundo.EstadoJugador mejorRival(EstadoMundo estadoActual, EstadoMundo.EstadoJugador yo) {
        EstadoMundo.EstadoJugador mejor = null;
        for (EstadoMundo.EstadoJugador p : estadoActual.jugadores) {
            if (yo != null && p.id == yo.id) continue;
            if (mejor == null || p.eliminaciones > mejor.eliminaciones || (p.eliminaciones == mejor.eliminaciones && p.muertes < mejor.muertes)) mejor = p;
        }
        return mejor;
    }

    private void panel(float x, float y, float w, float h, Color acento) {
        formas.setColor(0.02f, 0.03f, 0.07f, 0.72f);
        DibujoUi.chaflan(formas, x, y, w, h, 6);
        formas.setColor(acento.r, acento.g, acento.b, 0.75f);
        DibujoUi.contornoChaflan(formas, x, y, w, h, 6);
    }

    private void cuadroPuntos(float x, float y, Color color) {
        formas.setColor(color.r * 0.35f, color.g * 0.35f, color.b * 0.35f, 0.85f);
        DibujoUi.chaflan(formas, x, y, 44, 24, 5);
        formas.setColor(color);
        DibujoUi.contornoChaflan(formas, x, y, 44, 24, 5);
    }

    private static void dibujarBotonCircular(SpriteBatch batch, Texture textura, float cx, float cy, float r, Color tinte) {
        batch.setColor(tinte);
        batch.draw(textura, cx - r, cy - r, r * 2, r * 2);
        batch.setColor(Color.WHITE);
    }

    private void perilla(SpriteBatch batch, float cx, float cy, Vector2 arrastre, Color color) {
        float x = cx + arrastre.x, y = cy + arrastre.y;
        DibujoUi.brillo(batch, interfaz.brillo, x, y, 40, color, 0.9f);
        DibujoUi.brillo(batch, interfaz.brillo, x, y, 16, Color.WHITE, 1f);
    }

    private void centrado(SpriteBatch batch, BitmapFont f, String texto, float x, float y) {
        medidor.setText(f, texto);
        f.draw(batch, texto, x - medidor.width / 2f, y);
    }

    public Viewport getVista() {
        return vista;
    }

    public void dispose() {
        // Las fuentes y texturas son de KitUi; acá solo se libera lo propio.
        formas.dispose();
    }
}
