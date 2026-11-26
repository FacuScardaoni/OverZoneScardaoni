package com.OverZone.Scardaoni.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.JuegoOverZone;
import com.OverZone.Scardaoni.audio.GestorAudio;
import com.OverZone.Scardaoni.compartido.FasePartida;
import com.OverZone.Scardaoni.compartido.Equipo;
import com.OverZone.Scardaoni.graficos.AnimacionDireccional;
import com.OverZone.Scardaoni.entrada.ProcesadorEntrada;
import com.OverZone.Scardaoni.red.RedCliente;
import com.OverZone.Scardaoni.compartido.EstadoMundo;
import com.OverZone.Scardaoni.interfaz.Hud;
import com.OverZone.Scardaoni.interfaz.KitUi;
import com.OverZone.Scardaoni.interfaz.ControlesHud;
import com.OverZone.Scardaoni.compartido.Arena;
import com.OverZone.Scardaoni.compartido.TipoPersonaje;
import com.OverZone.Scardaoni.compartido.Direccion;
import com.OverZone.Scardaoni.compartido.ConstantesJuego;
import com.OverZone.Scardaoni.compartido.ModoJuego;
import com.OverZone.Scardaoni.compartido.Obstaculo;

import com.OverZone.Scardaoni.compartido.TipoPotenciador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PantallaJuego implements Screen {
    /** Si un jugador se movió más que esto entre dos estados, reapareció: no se interpola. */
    private static final float DISTANCIA_TELETRANSPORTE = 80f;

    /** Segundos por cuadro de caminata de cada personaje (en el orden de TipoPersonaje). */
    private static final float[] TIEMPO_CUADRO_CAMINATA = {0.10f, 0.16f, 0.12f, 0.09f};
    /** Opacidad de Phantom camuflado: casi invisible para los rivales, translúcido para su equipo. */
    private static final float OPACIDAD_CAMUFLAJE_RIVAL = 0.10f;
    private static final float OPACIDAD_CAMUFLAJE_ALIADO = 0.45f;
    /** Tono que toma Volt mientras usa el Impulso Over. */
    private static final Color TINTE_IMPULSO = new Color(0.55f, 1f, 1f, 0.85f);

    /** Medidas de las zonas de aparición pintadas en el piso (coinciden con el servidor). */
    private static final float ANCHO_ZONA_APARICION = 72f, ALTO_ZONA_APARICION = 168f, MARGEN_ZONA_APARICION = 90f;

    private final JuegoOverZone juego;
    private final GestorAudio audio;

    private final OrthographicCamera camaraMundo;
    private final Viewport vistaMundo;
    private final SpriteBatch batch;
    private final Hud hud;
    private final ProcesadorEntrada entrada;
    /** El mapa es fijo e igual en el servidor y en el cliente: se usa solo para dibujarlo. */
    private final Arena arena;
    private final BitmapFont fuenteEtiqueta;
    /** Botones de ataque y súper del HUD (joysticks). */
    private final ControlesHud controlesHud;
    /** Primero los botones del HUD; si el click no es sobre un botón, le llega al teclado/mouse normal. */
    private final InputMultiplexer entradas;
    /** Para dibujar la línea de mira en el mapa. */
    private final ShapeRenderer formasMundo = new ShapeRenderer();
    private float ultimoDelta = 0f;

    /** Distancia del punto de mira que se envía cuando se apunta con los botones. */
    private static final float DISTANCIA_APUNTADO = 150f;
    /** Hasta qué distancia busca enemigos el autoapuntado. */
    private static final float ALCANCE_AUTOAPUNTADO = 280f;
    private final GlyphLayout medidor = new GlyphLayout();

    // --- Recursos gráficos ---
    /** Una animación por personaje, ordenadas como TipoPersonaje (Volt, Brutus, Iris, Phantom). */
    private AnimacionDireccional[] animaciones;
    private Texture[] texturasProyectil;
    private Texture texturaPiso, texturaPared, texturaParedVertical, texturaCobertura;
    private Texture decoradoCentro, aparicionAzul, aparicionRoja, anilloEquipo, texturaEscudo, texturaZonaRegeneracion;
    private TextureRegion regionPiso, regionEscudo;

    private float tiempoAnimacion = 0f;

    private Texture basePotenciador;
    /** Power-Ups del estado anterior, para darse cuenta cuándo alguien agarra uno. */
    private final Map<Integer, EstadoMundo.EstadoPotenciador> ultimosPotenciadores = new HashMap<>();
    /** Textos que suben y se desvanecen sobre quien agarró un Power-Up ("¡TURBO!"). */
    private final List<TextoFlotante> textosFlotantes = new ArrayList<>();

    private static class TextoFlotante {
        final String texto;
        final Color color;
        final float x;
        float y;
        float vidaRestante = 1.2f;

        TextoFlotante(String texto, Color color, float x, float y) {
            this.texto = texto;
            this.color = color;
            this.x = x;
            this.y = y;
        }
    }
    private boolean liberada = false;

    // --- Para deducir sonidos a partir de los estados ---
    private long ultimaSecuenciaProcesada = -1;
    private Set<Integer> proyectilesConocidos = new HashSet<>();
    private float ultimaVida = -1f;
    /** Eliminaciones de cada jugador en el último estado (id -> eliminaciones). */
    private Map<Integer, Integer> ultimasEliminaciones = new HashMap<>();
    private boolean usabaHabilidad = false;

    public PantallaJuego(JuegoOverZone juego) {
        this.juego = juego;
        this.audio = juego.getGestorAudio();

        camaraMundo = new OrthographicCamera();
        vistaMundo = new ExtendViewport(480, 270, camaraMundo);
        batch = new SpriteBatch();
        hud = new Hud(juego.getInterfaz());
        entrada = new ProcesadorEntrada();
        arena = new Arena();
        fuenteEtiqueta = juego.getInterfaz().etiqueta;
        controlesHud = new ControlesHud(hud.getVista());
        entradas = new InputMultiplexer(controlesHud, entrada);
        formasMundo.setAutoShapeType(true);

        cargarRecursos();
    }

    private void cargarRecursos() {
        // Cada personaje tiene su hoja de sprites "<nombre>_walk.png" y su
        // proyectil "projectile_<nombre>.png" (por ejemplo, iris_walk.png).
        TipoPersonaje[] tipos = TipoPersonaje.values();
        animaciones = new AnimacionDireccional[tipos.length];
        texturasProyectil = new Texture[tipos.length];
        for (TipoPersonaje tipo : tipos) {
            String nombre = tipo.name().toLowerCase();
            Texture hoja = new Texture(Gdx.files.internal(nombre + "_walk.png"));
            animaciones[tipo.id()] = new AnimacionDireccional(hoja, 32, 32, 4, TIEMPO_CUADRO_CAMINATA[tipo.id()]);
            texturasProyectil[tipo.id()] = new Texture(Gdx.files.internal("projectile_" + nombre + ".png"));
        }
        basePotenciador = new Texture(Gdx.files.internal("powerup_pad.png"));
        basePotenciador.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        texturaZonaRegeneracion = new Texture(Gdx.files.internal("regen_zone.png"));
        texturaZonaRegeneracion.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        texturaPiso = repetida("floor_tile.png");
        regionPiso = new TextureRegion(texturaPiso, 0, 0, (int) Arena.ANCHO, (int) Arena.ALTO);
        texturaPared = repetida("wall_tile.png");
        texturaParedVertical = repetida("wall_tile_v.png");
        texturaCobertura = new Texture(Gdx.files.internal("cover_block.png"));

        decoradoCentro = new Texture(Gdx.files.internal("core_decal.png"));
        aparicionAzul = new Texture(Gdx.files.internal("spawn_blue.png"));
        aparicionRoja = new Texture(Gdx.files.internal("spawn_red.png"));
        anilloEquipo = new Texture(Gdx.files.internal("team_ring.png"));
        texturaEscudo = new Texture(Gdx.files.internal("shield.png"));
        regionEscudo = new TextureRegion(texturaEscudo);
    }

    /** Carga una textura que se repite (como baldosas) al dibujar un área más grande. */
    private static Texture repetida(String archivo) {
        Texture t = new Texture(Gdx.files.internal(archivo));
        t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return t;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(entradas);
        audio.reproducirMusica();
    }

    // ------------------------------------------------------------------
    // Bucle del cliente
    // ------------------------------------------------------------------

    @Override
    public void render(float delta) {
        // Si la partida terminó o se perdió la conexión, se cambia de pantalla.
        if (juego.seguirServidor(FasePartida.JUGANDO)) return;

        RedCliente conexion = juego.getRed();
        tiempoAnimacion += delta;
        for (AnimacionDireccional animacion : animaciones) animacion.actualizar(delta);

        if (entrada.silencioFueRecienApretado()) audio.mutearDesmutear();
        if (entrada.subirVolumenFueRecienApretado()) audio.subirVolumen();
        if (entrada.bajarVolumenFueRecienApretado()) audio.bajarVolumen();

        if (entrada.pausaFueRecienApretada()) {
            entrada.reiniciarMomentaneas();
            entrada.soltarTodasLasTeclas();
            controlesHud.reiniciar();
            juego.setScreen(new PantallaPausa(juego, this));
            return;
        }

        ultimoDelta = delta;
        controlesHud.actualizar(delta);
        RedCliente.ParEstados par = conexion.getEstados();
        if (par == null) return;

        // 1. Enviar entradas al servidor: movimiento, punto de mira, disparo y habilidad.
        enviarEntradas(conexion, par.actual.buscarJugador(conexion.getJugadorId()), par.actual);
        entrada.reiniciarMomentaneas();

        // 2. Interpolación entre los dos últimos estados recibidos.
        float alfa = alfaInterpolacion(par);
        int miId = conexion.getJugadorId();

        sonarEventos(par.actual, miId);
        detectarRecogidas(par.actual, miId, delta);

        // 3. Cámara sobre mi jugador y dibujo.
        EstadoMundo.EstadoJugador yo = par.actual.buscarJugador(miId);
        if (yo != null) {
            actualizarCamara(interpolarX(par, yo, alfa), interpolarY(par, yo, alfa));
        }
        dibujar(par, alfa, yo, conexion.getPingMs());
    }

    // ------------------------------------------------------------------
    // Entradas: teclado, mouse y botones del HUD
    // ------------------------------------------------------------------

    private void enviarEntradas(RedCliente conexion, EstadoMundo.EstadoJugador yo, EstadoMundo estadoActual) {
        Vector3 mouse = vistaMundo.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        float apuntarX = mouse.x, apuntarY = mouse.y;
        boolean disparar = entrada.disparoEstaApretado();
        boolean habilidad = entrada.habilidadFueRecienApretada();

        if (yo != null) {
            if (controlesHud.apuntandoAtaque()) {
                Vector2 d = new Vector2(controlesHud.getArrastreAtaque()).nor();
                apuntarX = yo.x + d.x * DISTANCIA_APUNTADO;
                apuntarY = yo.y + d.y * DISTANCIA_APUNTADO;
            } else if (controlesHud.estaDisparando()) {
                disparar = true;
                Vector2 objetivo = controlesHud.esDisparoAutomatico() ? objetivoAutoapuntado(yo, estadoActual)
                    : new Vector2(yo.x + controlesHud.getDireccionDisparo().x * DISTANCIA_APUNTADO,
                                  yo.y + controlesHud.getDireccionDisparo().y * DISTANCIA_APUNTADO);
                apuntarX = objetivo.x;
                apuntarY = objetivo.y;
            }
            if (controlesHud.apuntandoSuper()) {
                Vector2 d = new Vector2(controlesHud.getArrastreSuper()).nor();
                apuntarX = yo.x + d.x * DISTANCIA_APUNTADO;
                apuntarY = yo.y + d.y * DISTANCIA_APUNTADO;
            }
            if (controlesHud.consumirSuper()) {
                habilidad = true;
                if (!controlesHud.esSuperAutomatica()) {
                    apuntarX = yo.x + controlesHud.getDireccionSuper().x * DISTANCIA_APUNTADO;
                    apuntarY = yo.y + controlesHud.getDireccionSuper().y * DISTANCIA_APUNTADO;
                } else {
                    Vector2 objetivo = objetivoAutoapuntado(yo, estadoActual);
                    apuntarX = objetivo.x;
                    apuntarY = objetivo.y;
                }
            }
        }
        conexion.enviarEntrada(entrada.getMoverX(), entrada.getMoverY(), apuntarX, apuntarY, disparar, habilidad);
    }

    /** Enemigo vivo más cercano dentro del alcance; si no hay, hacia donde mira el jugador. */
    private Vector2 objetivoAutoapuntado(EstadoMundo.EstadoJugador yo, EstadoMundo estadoActual) {
        EstadoMundo.EstadoJugador mejor = null;
        float mejorDistancia = ALCANCE_AUTOAPUNTADO;
        for (EstadoMundo.EstadoJugador p : estadoActual.jugadores) {
            // Solo rivales vivos; a un Phantom camuflado no se lo puede "ver".
            if (!p.vivo || !estadoActual.sonRivales(yo, p) || estaCamuflado(p)) continue;
            float d = Vector2.dst(yo.x, yo.y, p.x, p.y);
            if (d < mejorDistancia) {
                mejorDistancia = d;
                mejor = p;
            }
        }
        if (mejor != null) return new Vector2(mejor.x, mejor.y);
        switch (yo.mirando) {
            case Direccion.IZQUIERDA: return new Vector2(yo.x - DISTANCIA_APUNTADO, yo.y);
            case Direccion.DERECHA: return new Vector2(yo.x + DISTANCIA_APUNTADO, yo.y);
            case Direccion.ARRIBA: return new Vector2(yo.x, yo.y + DISTANCIA_APUNTADO);
            default: return new Vector2(yo.x, yo.y - DISTANCIA_APUNTADO);
        }
    }

    private void dibujarIndicadorApuntado(EstadoMundo.EstadoJugador yo, RedCliente.ParEstados par, float alfa) {
        boolean ataque = controlesHud.apuntandoAtaque();
        boolean habilidad = controlesHud.apuntandoSuper();
        if (yo == null || !yo.vivo || (!ataque && !habilidad)) return;

        float x = interpolarX(par, yo, alfa), y = interpolarY(par, yo, alfa);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        formasMundo.setProjectionMatrix(camaraMundo.combined);
        formasMundo.begin(ShapeRenderer.ShapeType.Filled);
        if (ataque) {
            Vector2 d = new Vector2(controlesHud.getArrastreAtaque()).nor();
            // Línea punteada de mira
            for (float t = 14; t < 170; t += 12) {
                float a = 0.85f * (1f - t / 190f);
                formasMundo.setColor(1f, 0.3f, 0.3f, a);
                formasMundo.rectLine(x + d.x * t, y + d.y * t, x + d.x * (t + 7), y + d.y * (t + 7), 2.4f);
            }
            formasMundo.setColor(1f, 0.35f, 0.35f, 0.9f);
            formasMundo.circle(x + d.x * 176, y + d.y * 176, 3.5f, 16);
        }
        if (habilidad) {
            Vector2 d = new Vector2(controlesHud.getArrastreSuper()).nor();
            TipoPersonaje tipo = TipoPersonaje.desdeId(yo.personaje);
            if (tipo == TipoPersonaje.BRUTUS) {
                // Cono del escudo: la zona de la que frena los disparos
                float angulo = d.angleDeg();
                formasMundo.setColor(1f, 0.85f, 0.2f, 0.28f);
                formasMundo.arc(x, y, 44, angulo - 66, 132, 24);
            } else if (tipo == TipoPersonaje.IRIS) {
                // Campo de Regeneración: se crea donde está parada Iris
                formasMundo.setColor(0.35f, 1f, 0.55f, 0.22f);
                formasMundo.circle(x, y, TipoPersonaje.RADIO_REGENERACION, 40);
            } else if (tipo == TipoPersonaje.PHANTOM) {
                // Camuflaje: un aro que se cierra sobre Phantom
                formasMundo.setColor(1f, 0.25f, 0.35f, 0.35f);
                formasMundo.set(ShapeRenderer.ShapeType.Line);
                formasMundo.circle(x, y, 20, 32);
                formasMundo.set(ShapeRenderer.ShapeType.Filled);
            } else {
                // Flecha del impulso: aproximadamente la distancia que recorre
                float largo = tipo.velocidad * 3.2f * tipo.duracionHabilidad;
                formasMundo.setColor(1f, 0.88f, 0.25f, 0.6f);
                formasMundo.rectLine(x, y, x + d.x * largo, y + d.y * largo, 6f);
                Vector2 costado = new Vector2(-d.y, d.x);
                float tx = x + d.x * (largo + 10), ty = y + d.y * (largo + 10);
                formasMundo.triangle(tx, ty,
                    x + d.x * largo + costado.x * 8, y + d.y * largo + costado.y * 8,
                    x + d.x * largo - costado.x * 8, y + d.y * largo - costado.y * 8);
            }
        }
        formasMundo.end();
    }

    // ------------------------------------------------------------------
    // Interpolación
    // ------------------------------------------------------------------

    private float alfaInterpolacion(RedCliente.ParEstados par) {
        long intervalo = par.actual.recibidoNanos - par.anterior.recibidoNanos;
        if (intervalo <= 0) return 1f;
        float transcurrido = System.nanoTime() - par.actual.recibidoNanos;
        return MathUtils.clamp(transcurrido / intervalo, 0f, 1f);
    }

    private float interpolarX(RedCliente.ParEstados par, EstadoMundo.EstadoJugador p, float alfa) {
        EstadoMundo.EstadoJugador antes = par.anterior.buscarJugador(p.id);
        if (!puedeInterpolar(antes, p)) return p.x;
        return MathUtils.lerp(antes.x, p.x, alfa);
    }

    private float interpolarY(RedCliente.ParEstados par, EstadoMundo.EstadoJugador p, float alfa) {
        EstadoMundo.EstadoJugador antes = par.anterior.buscarJugador(p.id);
        if (!puedeInterpolar(antes, p)) return p.y;
        return MathUtils.lerp(antes.y, p.y, alfa);
    }

    private boolean puedeInterpolar(EstadoMundo.EstadoJugador antes, EstadoMundo.EstadoJugador ahora) {
        if (antes == null || !antes.vivo) return false;
        return Math.abs(antes.x - ahora.x) < DISTANCIA_TELETRANSPORTE && Math.abs(antes.y - ahora.y) < DISTANCIA_TELETRANSPORTE;
    }

    // ------------------------------------------------------------------
    // Sonidos deducidos del estado
    // ------------------------------------------------------------------

    private void sonarEventos(EstadoMundo estadoActual, int miId) {
        if (estadoActual.secuencia == ultimaSecuenciaProcesada) return; // este estado ya se procesó
        boolean primero = ultimaSecuenciaProcesada < 0;
        ultimaSecuenciaProcesada = estadoActual.secuencia;

        Set<Integer> actual = new HashSet<>();
        boolean disparoNuevo = false;
        for (EstadoMundo.EstadoProyectil p : estadoActual.proyectiles) {
            actual.add(p.id);
            if (!proyectilesConocidos.contains(p.id)) disparoNuevo = true;
        }
        proyectilesConocidos = actual;
        if (disparoNuevo && !primero) audio.sonarDisparo();

        EstadoMundo.EstadoJugador yo = estadoActual.buscarJugador(miId);
        if (yo != null) {
            if (ultimaVida >= 0 && yo.vida < ultimaVida) audio.sonarGolpe();
            ultimaVida = yo.vida;
            if (yo.habilidadActiva && !usabaHabilidad) {
                switch (TipoPersonaje.desdeId(yo.personaje)) {
                    case BRUTUS: audio.sonarEscudo(); break;
                    case IRIS: audio.sonarCuracion(); break;
                    case PHANTOM: audio.sonarCamuflaje(); break;
                    default: audio.sonarImpulso(); break;
                }
            }
            usabaHabilidad = yo.habilidadActiva;
        }

        // Eliminaciones de cada jugador: si a alguno le subió, hubo una eliminación.
        // (Antes se sumaban las de todos: si alguien se iba justo cuando otro
        // eliminaba, la suma no cambiaba y el sonido no salía.)
        boolean alguienElimino = false;
        Map<Integer, Integer> eliminaciones = new HashMap<>();
        for (EstadoMundo.EstadoJugador p : estadoActual.jugadores) {
            eliminaciones.put(p.id, p.eliminaciones);
            Integer antes = ultimasEliminaciones.get(p.id);
            if (antes != null && p.eliminaciones > antes) alguienElimino = true;
        }
        if (alguienElimino && !primero) audio.sonarEliminacion();
        ultimasEliminaciones = eliminaciones;
    }

    // ------------------------------------------------------------------
    // Cámara y dibujo
    // ------------------------------------------------------------------

    private void actualizarCamara(float objetivoX, float objetivoY) {
        float medioAncho = camaraMundo.viewportWidth * camaraMundo.zoom / 2f;
        float medioAlto = camaraMundo.viewportHeight * camaraMundo.zoom / 2f;

        float camaraX = (medioAncho * 2 >= Arena.ANCHO)
            ? Arena.ANCHO / 2f
            : MathUtils.clamp(objetivoX, medioAncho, Arena.ANCHO - medioAncho);
        float camaraY = (medioAlto * 2 >= Arena.ALTO)
            ? Arena.ALTO / 2f
            : MathUtils.clamp(objetivoY, medioAlto, Arena.ALTO - medioAlto);

        camaraMundo.position.set(camaraX, camaraY, 0);
        camaraMundo.update();
    }

    private void dibujar(RedCliente.ParEstados par, float alfa, EstadoMundo.EstadoJugador yo, float pingMs) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        EstadoMundo estadoActual = par.actual;
        ModoJuego modo = estadoActual.getModo();

        vistaMundo.apply();
        batch.setProjectionMatrix(camaraMundo.combined);
        batch.begin();

        dibujarMapa(modo);
        dibujarZonas(estadoActual, modo);
        dibujarPotenciadoresMapa(estadoActual);

        // Jugadores: anillo del color del equipo (o del jugador en Todos contra
        // todos), sprite del personaje y efectos de la habilidad activa.
        for (EstadoMundo.EstadoJugador p : estadoActual.jugadores) {
            if (!p.vivo) continue;
            float x = interpolarX(par, p, alfa);
            float y = interpolarY(par, p, alfa);
            TipoPersonaje tipo = TipoPersonaje.desdeId(p.personaje);
            float opacidad = opacidadPara(p, yo, estadoActual);
            boolean rivalOculto = opacidad < OPACIDAD_CAMUFLAJE_ALIADO;

            if (!rivalOculto && p.tieneAlgunPotenciador()) {
                // Aura del Power-Up activo (si tiene varios, va alternando el color)
                Color aura = colorPotenciadorActivo(p);
                float pulso = 0.55f + 0.3f * MathUtils.sin(tiempoAnimacion * 7f);
                batch.setColor(aura.r, aura.g, aura.b, pulso * opacidad);
                batch.draw(anilloEquipo, x - 22, y - 22, 44, 22);
            }

            if (!rivalOculto) {
                Color anillo = KitUi.colorJugador(modo, p.equipo);
                batch.setColor(anillo.r, anillo.g, anillo.b, opacidad);
                batch.draw(anilloEquipo, x - 16, y - 18, 32, 16);
            }

            boolean visible = !p.invulnerable || ((int) (tiempoAnimacion * 12) % 2 == 0);
            if (visible) {
                if (p.habilidadActiva && tipo == TipoPersonaje.VOLT) batch.setColor(TINTE_IMPULSO);
                else batch.setColor(1f, 1f, 1f, opacidad);
                TextureRegion cuadro = animaciones[tipo.id()].getCuadro(p.mirando, p.moviendose);
                float tamanio = ConstantesJuego.TAMANIO_DIBUJO_JUGADOR;
                // El sprite se dibuja un poco más arriba que el centro, para
                // que los pies queden sobre el anillo.
                batch.draw(cuadro, x - tamanio / 2f, y - tamanio / 2f + 4, tamanio, tamanio);
            }
            batch.setColor(Color.WHITE);

            if (p.habilidadActiva && tipo == TipoPersonaje.BRUTUS) {
                // El dibujo del escudo apunta a la derecha; se rota según el ángulo.
                batch.draw(regionEscudo, x - 24, y - 24, 24, 24, 48, 48, 1f, 1f, p.anguloEscudo);
            }
        }

        // Proyectiles: imagen según el personaje que disparó, teñida con el color del equipo.
        for (EstadoMundo.EstadoProyectil p : estadoActual.proyectiles) {
            EstadoMundo.EstadoProyectil antes = par.anterior.buscarProyectil(p.id);
            float x = antes == null ? p.x : MathUtils.lerp(antes.x, p.x, alfa);
            float y = antes == null ? p.y : MathUtils.lerp(antes.y, p.y, alfa);
            TipoPersonaje tipo = TipoPersonaje.desdeId(p.personaje);
            float tamanio = tipo.tamanioProyectil * 1.5f;   // el brillo es más grande que el área de choque
            batch.setColor(KitUi.colorJugador(modo, p.equipo));
            batch.draw(texturasProyectil[tipo.id()], x - tamanio / 2f, y - tamanio / 2f, tamanio, tamanio);
        }
        batch.setColor(Color.WHITE);

        // Nombres sobre cada jugador (un Phantom camuflado rival no muestra el suyo).
        for (EstadoMundo.EstadoJugador p : estadoActual.jugadores) {
            if (!p.vivo || opacidadPara(p, yo, estadoActual) < OPACIDAD_CAMUFLAJE_ALIADO) continue;
            boolean soyYo = yo != null && p.id == yo.id;
            fuenteEtiqueta.setColor(soyYo ? Color.YELLOW : KitUi.colorJugador(modo, p.equipo));
            String etiqueta = soyYo ? "Vos" : p.nombre;
            medidor.setText(fuenteEtiqueta, etiqueta);
            fuenteEtiqueta.draw(batch, etiqueta, interpolarX(par, p, alfa) - medidor.width / 2f,
                interpolarY(par, p, alfa) + ConstantesJuego.TAMANIO_DIBUJO_JUGADOR / 2f + 12);
        }

        // Textos de Power-Ups agarrados
        for (TextoFlotante t : textosFlotantes) {
            float a = MathUtils.clamp(t.vidaRestante / 0.5f, 0f, 1f);
            fuenteEtiqueta.setColor(t.color.r, t.color.g, t.color.b, a);
            medidor.setText(fuenteEtiqueta, t.texto);
            fuenteEtiqueta.draw(batch, t.texto, t.x - medidor.width / 2f, t.y);
        }
        fuenteEtiqueta.setColor(Color.WHITE);

        batch.end();

        hud.getVista().apply();
        dibujarIndicadorApuntado(yo, par, alfa);

        hud.render(batch, yo, estadoActual, audio.estaSilenciado(), audio.getVolumen(), pingMs, controlesHud, ultimoDelta);
    }

    private static boolean estaCamuflado(EstadoMundo.EstadoJugador p) {
        return p.habilidadActiva && TipoPersonaje.desdeId(p.personaje) == TipoPersonaje.PHANTOM;
    }

    private float opacidadPara(EstadoMundo.EstadoJugador p, EstadoMundo.EstadoJugador yo, EstadoMundo estadoActual) {
        if (!estaCamuflado(p)) return 1f;
        boolean amigo = yo == null || p.id == yo.id || !estadoActual.sonRivales(yo, p);
        return amigo ? OPACIDAD_CAMUFLAJE_ALIADO : OPACIDAD_CAMUFLAJE_RIVAL;
    }

    /** Color del aura: el de un Power-Up activo; si hay varios, cambia cada medio segundo. */
    private Color colorPotenciadorActivo(EstadoMundo.EstadoJugador p) {
        List<TipoPotenciador> activo = new ArrayList<>();
        for (TipoPotenciador t : TipoPotenciador.values()) {
            if (p.potenciador(t) > 0f) activo.add(t);
        }
        return KitUi.colorPotenciador(activo.get((int) (tiempoAnimacion * 2f) % activo.size()));
    }

    /** Power-Ups en el mapa: flotan arriba y abajo sobre una base que late. */
    private void dibujarPotenciadoresMapa(EstadoMundo estadoActual) {
        for (EstadoMundo.EstadoPotenciador pot : estadoActual.potenciadoresMapa) {
            Color c = KitUi.colorPotenciador(pot.getTipo());
            float pulso = 0.6f + 0.4f * MathUtils.sin(tiempoAnimacion * 5f + pot.id);
            batch.setColor(c.r, c.g, c.b, 0.75f * pulso);
            batch.draw(basePotenciador, pot.x - 20, pot.y - 20, 40, 40);
            float vaiven = MathUtils.sin(tiempoAnimacion * 3f + pot.id) * 2.5f;
            batch.setColor(Color.WHITE);
            batch.draw(juego.getInterfaz().iconoPotenciador(pot.getTipo()), pot.x - 12, pot.y - 12 + vaiven + 3, 24, 24);
        }
        batch.setColor(Color.WHITE);
    }

    private void detectarRecogidas(EstadoMundo estadoActual, int miId, float delta) {
        if (estadoActual.fase == FasePartida.JUGANDO) {
            for (EstadoMundo.EstadoPotenciador vieja : ultimosPotenciadores.values()) {
                if (estadoActual.buscarPotenciador(vieja.id) != null) continue;
                EstadoMundo.EstadoJugador recolector = null;
                float mejor = (TipoPotenciador.RADIO_RECOGIDA + 14f) * (TipoPotenciador.RADIO_RECOGIDA + 14f);
                for (EstadoMundo.EstadoJugador p : estadoActual.jugadores) {
                    if (!p.vivo) continue;
                    float dx = p.x - vieja.x, dy = p.y - vieja.y;
                    float d2 = dx * dx + dy * dy;
                    if (d2 <= mejor) {
                        mejor = d2;
                        recolector = p;
                    }
                }
                if (recolector == null) continue;
                TipoPotenciador tipo = vieja.getTipo();
                if (recolector.id == miId) audio.sonarPotenciador();
                textosFlotantes.add(new TextoFlotante("¡" + tipo.nombreVisible.toUpperCase() + "!",
                    KitUi.colorPotenciador(tipo), recolector.x, recolector.y + ConstantesJuego.TAMANIO_DIBUJO_JUGADOR / 2f + 26));
            }
        }
        ultimosPotenciadores.clear();
        for (EstadoMundo.EstadoPotenciador pot : estadoActual.potenciadoresMapa) ultimosPotenciadores.put(pot.id, pot);

        Iterator<TextoFlotante> it = textosFlotantes.iterator();
        while (it.hasNext()) {
            TextoFlotante t = it.next();
            t.vidaRestante -= delta;
            t.y += 22f * delta;
            if (t.vidaRestante <= 0f) it.remove();
        }
    }

    /** Campos de Regeneración de Iris: círculo verde que late y se apaga al terminar. */
    private void dibujarZonas(EstadoMundo estadoActual, ModoJuego modo) {
        for (EstadoMundo.EstadoZona z : estadoActual.zonas) {
            float desvanecer = MathUtils.clamp(z.restante / 0.6f, 0f, 1f);       // se apaga en el final
            float pulso = 0.75f + 0.25f * MathUtils.sin(tiempoAnimacion * 5f);
            Color c = modo == ModoJuego.EQUIPOS ? KitUi.colorJugador(modo, z.equipo) : KitUi.VERDE;
            // Mezcla del verde de curación con el color del equipo dueño
            batch.setColor(0.6f * KitUi.VERDE.r + 0.4f * c.r, 0.6f * KitUi.VERDE.g + 0.4f * c.g,
                0.6f * KitUi.VERDE.b + 0.4f * c.b, 0.85f * desvanecer * pulso);
            float tamanio = z.radio * 2f;
            batch.draw(texturaZonaRegeneracion, z.x - z.radio, z.y - z.radio, tamanio, tamanio);
        }
        batch.setColor(Color.WHITE);
    }

    /** Piso, marcas en el piso, paredes y bloques de cobertura. */
    private void dibujarMapa(ModoJuego modo) {
        batch.draw(regionPiso, 0, 0, Arena.ANCHO, Arena.ALTO);

        // Lugares donde aparecen los Power-Ups (marcados tenues aunque estén vacíos).
        batch.setColor(1f, 1f, 1f, 0.18f);
        for (float[] posicion : Arena.APARICIONES_POTENCIADORES) {
            batch.draw(basePotenciador, posicion[0] - 20, posicion[1] - 20, 40, 40);
        }
        batch.setColor(Color.WHITE);

        // Las zonas de aparición de cada equipo solo tienen sentido en Equipos.
        if (modo == ModoJuego.EQUIPOS) {
            float zonaY = Arena.ALTO / 2f - ALTO_ZONA_APARICION / 2f;
            batch.draw(aparicionAzul, MARGEN_ZONA_APARICION - ANCHO_ZONA_APARICION / 2f, zonaY);
            batch.draw(aparicionRoja, Arena.ANCHO - MARGEN_ZONA_APARICION - ANCHO_ZONA_APARICION / 2f, zonaY);
        }
        batch.draw(decoradoCentro, Arena.ANCHO / 2f - decoradoCentro.getWidth() / 2f, Arena.ALTO / 2f - decoradoCentro.getHeight() / 2f);

        for (Obstaculo obstaculo : arena.getObstaculos()) {
            Rectangle b = obstaculo.getLimites();
            if (b.width == texturaCobertura.getWidth() && b.height == texturaCobertura.getHeight()) {
                batch.draw(texturaCobertura, b.x, b.y, b.width, b.height);
            } else {
                // Las paredes se arman repitiendo la baldosa a lo largo:
                // horizontal o vertical según la forma de la pared.
                Texture baldosa = b.width >= b.height ? texturaPared : texturaParedVertical;
                batch.draw(baldosa, b.x, b.y, b.width, b.height, 0, 0, (int) b.width, (int) b.height, false, false);
            }
        }
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    public void resize(int ancho, int alto) {
        vistaMundo.update(ancho, alto);
        hud.resize(ancho, alto);
    }

    /** Lo llama PantallaPausa al volver a la partida. */
    public void reanudarJuego() {
        Gdx.input.setInputProcessor(entradas);
        entrada.soltarTodasLasTeclas();
        entrada.reiniciarMomentaneas();
        controlesHud.reiniciar();
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        // Guarda: puede liberarse desde la pausa o al terminar la partida.
        if (liberada) return;
        liberada = true;

        batch.dispose();
        hud.dispose();
        formasMundo.dispose();
        for (AnimacionDireccional animacion : animaciones) animacion.dispose(); // libera las hojas de sprites
        for (Texture t : texturasProyectil) t.dispose();
        texturaPiso.dispose();
        texturaPared.dispose();
        texturaParedVertical.dispose();
        texturaCobertura.dispose();
        decoradoCentro.dispose();
        aparicionAzul.dispose();
        aparicionRoja.dispose();
        anilloEquipo.dispose();
        texturaEscudo.dispose();
        texturaZonaRegeneracion.dispose();
        basePotenciador.dispose();
    }
}
