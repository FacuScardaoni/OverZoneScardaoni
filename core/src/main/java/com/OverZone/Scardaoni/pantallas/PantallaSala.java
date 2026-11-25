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
import com.OverZone.Scardaoni.interfaz.KitUi;
import com.OverZone.Scardaoni.red.RedCliente;
import com.OverZone.Scardaoni.compartido.TipoPersonaje;
import com.OverZone.Scardaoni.compartido.ConstantesJuego;
import com.OverZone.Scardaoni.compartido.ModoJuego;
import com.OverZone.Scardaoni.compartido.FasePartida;
import com.OverZone.Scardaoni.compartido.Protocolo;
import com.OverZone.Scardaoni.compartido.Equipo;
import com.OverZone.Scardaoni.compartido.EstadoMundo;

import java.util.ArrayList;
import java.util.List;

/**
 * Sala de espera.
 *
 *  - Arriba a la izquierda: los jugadores conectados (hasta 4), con su
 *    personaje, su color o equipo y si están listos.
 *  - Arriba a la derecha: el modo de juego. Solo el anfitrión (el primero
 *    que se conectó, marcado con una estrella) puede cambiarlo. "Equipos 2v2"
 *    se habilita únicamente con 4 jugadores en la sala.
 *  - Abajo: las cuatro tarjetas de personaje (teclas 1 a 4).
 *
 * El personaje y el modo elegidos viajan al servidor en cada PING. El servidor
 * decide si los acepta y lo informa de vuelta en el estado: la pantalla
 * siempre muestra lo que dice el servidor, no lo que pidió el cliente.
 */
public class PantallaSala extends PantallaMenuBase {

    private final Rectangle botonListo = new Rectangle(110, 8, 120, 24);
    private final Rectangle botonSalir = new Rectangle(250, 8, 120, 24);

    private final Rectangle botonTodosContraTodos = new Rectangle(258, 176, 200, 22);
    private final Rectangle botonEquipos = new Rectangle(258, 150, 200, 22);

    /** Tarjetas de elección de personaje, en el mismo orden que TipoPersonaje. */
    private final Rectangle[] tarjetasPersonaje = new Rectangle[TipoPersonaje.values().length];

    /** Cuánto se espera la confirmación del servidor antes de avisar que no se pudo. */
    private static final float ESPERA_CONFIRMACION_MODO = 1.5f;
    /** Cuánto queda en pantalla un aviso. */
    private static final float DURACION_AVISO = 3f;

    /** Modo que pidió este cliente y todavía no confirmó el servidor (null si no hay). */
    private ModoJuego modoPendiente;
    private float tiempoPendiente;
    /** Último modo visto en el estado, para avisar cuando el anfitrión lo cambia. */
    private ModoJuego ultimoModoVisto;

    /** Aviso corto en la parte de abajo (por qué no se pudo cambiar el modo, etc.). */
    private String aviso;
    private Color colorAviso = Color.WHITE;
    private float tiempoAviso;
    private float tiempoParpadeo;

    private final Texture[] hojas;
    /** Primer cuadro (mirando hacia abajo) de cada personaje, como retrato. */
    private final TextureRegion[] retratos;

    public PantallaSala(JuegoOverZone juego) {
        super(juego, 1.4f);
        TipoPersonaje[] tipos = TipoPersonaje.values();
        hojas = new Texture[tipos.length];
        retratos = new TextureRegion[tipos.length];
        for (TipoPersonaje tipo : tipos) {
            int i = tipo.id();
            tarjetasPersonaje[i] = new Rectangle(12 + i * 116, 52, 108, 74);
            hojas[i] = new Texture(Gdx.files.internal(tipo.name().toLowerCase() + "_walk.png"));
            retratos[i] = new TextureRegion(hojas[i], 0, 0, 32, 32);
        }
    }

    @Override
    public void show() {
        super.show();
        // Al entrar a la sala, nadie está listo hasta que lo marque.
        if (juego.getRed() != null) juego.getRed().setListo(false);
    }

    @Override
    protected InputProcessor crearProcesadorEntrada() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int codigoTecla) {
                switch (codigoTecla) {
                    case Input.Keys.ENTER:
                    case Input.Keys.SPACE:
                        alternarListo();
                        return true;
                    case Input.Keys.ESCAPE:
                        salir();
                        return true;
                    case Input.Keys.NUM_1: elegir(TipoPersonaje.VOLT); return true;
                    case Input.Keys.NUM_2: elegir(TipoPersonaje.BRUTUS); return true;
                    case Input.Keys.NUM_3: elegir(TipoPersonaje.IRIS); return true;
                    case Input.Keys.NUM_4: elegir(TipoPersonaje.PHANTOM); return true;
                    case Input.Keys.LEFT: rotarPersonaje(-1); return true;
                    case Input.Keys.RIGHT: rotarPersonaje(1); return true;
                    case Input.Keys.M:
                        EstadoMundo estadoActual = ultimo();
                        if (estadoActual != null) {
                            ModoJuego mostrado = modoPendiente != null ? modoPendiente : estadoActual.getModo();
                            elegirModo(mostrado == ModoJuego.EQUIPOS ? ModoJuego.TODOS_CONTRA_TODOS : ModoJuego.EQUIPOS);
                        }
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
                if (boton != Input.Buttons.LEFT) return false;
                if (clickeado(botonListo, pantallaX, pantallaY)) {
                    alternarListo();
                    return true;
                }
                if (clickeado(botonSalir, pantallaX, pantallaY)) {
                    salir();
                    return true;
                }
                if (clickeado(botonTodosContraTodos, pantallaX, pantallaY)) {
                    elegirModo(ModoJuego.TODOS_CONTRA_TODOS);
                    return true;
                }
                if (clickeado(botonEquipos, pantallaX, pantallaY)) {
                    elegirModo(ModoJuego.EQUIPOS);
                    return true;
                }
                for (TipoPersonaje tipo : TipoPersonaje.values()) {
                    if (clickeado(tarjetasPersonaje[tipo.id()], pantallaX, pantallaY)) {
                        elegir(tipo);
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private EstadoMundo ultimo() {
        RedCliente conexion = juego.getRed();
        return conexion == null ? null : conexion.getUltimoEstado();
    }

    private boolean soyAnfitrion(EstadoMundo estadoActual) {
        RedCliente conexion = juego.getRed();
        return conexion != null && estadoActual != null && estadoActual.anfitrionId == conexion.getJugadorId();
    }

    private void alternarListo() {
        RedCliente conexion = juego.getRed();
        if (conexion != null) conexion.setListo(!conexion.estaListo());
    }

    private void elegir(TipoPersonaje tipo) {
        RedCliente conexion = juego.getRed();
        if (conexion != null) conexion.setPersonaje(tipo);
    }

    private void rotarPersonaje(int paso) {
        RedCliente conexion = juego.getRed();
        if (conexion == null) return;
        int n = TipoPersonaje.values().length;
        elegir(TipoPersonaje.desdeId(Math.floorMod(conexion.getPersonaje().id() + paso, n)));
    }

    /**
     * Elegir un modo. Antes de mandar el pedido se revisa lo mismo que va a
     * revisar el servidor, para avisarle al jugador en el momento por qué no
     * se puede, en vez de que el botón "no haga nada".
     */
    private void elegirModo(ModoJuego modo) {
        EstadoMundo estadoActual = ultimo();
        if (estadoActual == null) return;
        int cantidad = estadoActual.jugadores.size();
        if (!soyAnfitrion(estadoActual)) {
            EstadoMundo.EstadoJugador anfitrion = estadoActual.buscarJugador(estadoActual.anfitrionId);
            mostrarAviso("Solo el anfitrión" + (anfitrion == null ? "" : " (" + anfitrion.nombre + ")")
                + " puede cambiar el modo", KitUi.NARANJA);
            return;
        }
        if (!modo.permite(cantidad)) {
            mostrarAviso(modo.nombreVisible + " necesita exactamente " + modo.jugadoresNecesarios
                + " jugadores (hay " + cantidad + ")", KitUi.NARANJA);
            return;
        }
        if (modo == estadoActual.getModo() && modoPendiente == null) return; // ya está elegido
        juego.getRed().pedirModo(modo);
        modoPendiente = modo;
        tiempoPendiente = ESPERA_CONFIRMACION_MODO;
    }

    private void mostrarAviso(String texto, Color color) {
        aviso = texto;
        colorAviso = color;
        tiempoAviso = DURACION_AVISO;
    }

    private void salir() {
        juego.desconectar();
        juego.reemplazarPantalla(new PantallaMenu(juego));
    }

    @Override
    protected boolean actualizar(float delta) {
        tiempoParpadeo += delta;
        if (tiempoAviso > 0) tiempoAviso -= delta;

        EstadoMundo estadoActual = ultimo();
        if (estadoActual != null) {
            ModoJuego actual = estadoActual.getModo();
            // ¿El servidor confirmó el modo que pedí?
            if (modoPendiente != null) {
                if (actual == modoPendiente) {
                    modoPendiente = null;
                    mostrarAviso("Modo: " + actual.nombreVisible, KitUi.VERDE);
                } else {
                    tiempoPendiente -= delta;
                    if (tiempoPendiente <= 0) {
                        modoPendiente = null;
                        mostrarAviso("El servidor no aceptó el cambio. Probá de nuevo.", KitUi.NARANJA);
                    }
                }
            } else if (ultimoModoVisto != null && actual != ultimoModoVisto) {
                // Lo cambió el anfitrión (o el servidor, porque alguien se fue).
                mostrarAviso(soyAnfitrion(estadoActual) ? "Modo: " + actual.nombreVisible
                    : "Ahora se juega: " + actual.nombreVisible, KitUi.AMARILLO);
            }
            ultimoModoVisto = actual;
        }
        return juego.seguirServidor(FasePartida.SALA, FasePartida.CUENTA_REGRESIVA);
    }

    /** Jugadores en el orden en que se muestran (en Equipos, primero los azules). */
    private List<EstadoMundo.EstadoJugador> jugadoresOrdenados(EstadoMundo estadoActual) {
        List<EstadoMundo.EstadoJugador> lista = new ArrayList<>(estadoActual.jugadores);
        if (estadoActual.getModo() == ModoJuego.EQUIPOS) lista.sort((a, b) -> Integer.compare(a.equipo, b.equipo));
        return lista;
    }

    @Override
    protected void dibujarFormas(Vector3 mouse) {
        RedCliente conexion = juego.getRed();
        EstadoMundo estadoActual = ultimo();
        boolean listo = conexion != null && conexion.estaListo();
        TipoPersonaje seleccionado = conexion == null ? TipoPersonaje.VOLT : conexion.getPersonaje();

        // Panel de jugadores y panel de modo
        dibujarPanel(12, 134, 232, 96, KitUi.CIAN);
        dibujarPanel(250, 134, 218, 96, KitUi.AMARILLO);

        if (estadoActual != null) {
            // Marca de color de cada jugador
            float y = 196;
            for (EstadoMundo.EstadoJugador p : jugadoresOrdenados(estadoActual)) {
                Color c = KitUi.colorJugador(estadoActual.getModo(), p.equipo);
                formas.setColor(c);
                formas.rect(20, y - 8, 4, 10);
                y -= 15;
            }

            // Botones de modo: el elegido queda encendido; los no disponibles, apagados
            boolean anfitrion = soyAnfitrion(estadoActual);
            int cantidad = estadoActual.jugadores.size();
            for (ModoJuego modo : ModoJuego.values()) {
                Rectangle r = modo == ModoJuego.EQUIPOS ? botonEquipos : botonTodosContraTodos;
                boolean encendido = estadoActual.getModo() == modo && modoPendiente == null;
                boolean esperando = modoPendiente == modo;
                boolean disponible = anfitrion && modo.permite(cantidad);
                Vector3 m = disponible ? mouse : new Vector3(-100, -100, 0);
                if (esperando) {
                    // Esperando la confirmación del servidor: el botón titila
                    float k = 0.6f + 0.4f * Math.abs(MathUtils.sin(tiempoParpadeo * 8f));
                    dibujarBoton(r, m, k, k * 0.86f, 0.2f, true);
                } else if (encendido) dibujarBoton(r, m, 1f, 0.86f, 0.2f, true);
                else if (disponible) dibujarBoton(r, m, 0.3f, 0.7f, 1f);
                else dibujarBoton(r, m, 0.35f, 0.35f, 0.4f);
            }
        }

        // Tarjetas de personaje: la elegida tiene un borde brillante
        for (TipoPersonaje tipo : TipoPersonaje.values()) {
            Rectangle tarjeta = tarjetasPersonaje[tipo.id()];
            Color acento = KitUi.colorPersonaje(tipo);
            dibujarBoton(tarjeta, mouse, acento.r, acento.g, acento.b, tipo == seleccionado);
            formas.setColor(acento.r * 0.15f, acento.g * 0.15f, acento.b * 0.15f, 0.9f);
            formas.rect(tarjeta.x + 8, tarjeta.y + 30, 38, 38);
        }

        if (listo) dibujarBoton(botonListo, mouse, 0.20f, 0.55f, 0.30f);
        else dibujarBoton(botonListo, mouse, 0.20f, 0.50f, 0.85f);
        dibujarBoton(botonSalir, mouse, 0.45f, 0.30f, 0.30f);
    }

    @Override
    protected void dibujarTexto(Vector3 mouse) {
        RedCliente conexion = juego.getRed();
        EstadoMundo estadoActual = ultimo();

        dibujarTitulo("Sala de espera", ANCHO / 2f, 264, Color.WHITE);
        fuenteChica.setColor(Color.GRAY);
        if (conexion != null) {
            dibujarCentrado(fuenteChica, "Servidor: " + conexion.getNombreServidor() + " (" + conexion.getDireccionServidor() + ")"
                + textoPing(conexion), ANCHO / 2f, 244);
        }

        // --- Jugadores ---
        int cantidad = estadoActual == null ? 0 : estadoActual.jugadores.size();
        fuente.setColor(Color.WHITE);
        fuente.draw(batch, "Jugadores " + cantidad + "/" + Protocolo.MAX_JUGADORES, 20, 224);
        if (estadoActual != null) {
            float y = 204;
            for (EstadoMundo.EstadoJugador p : jugadoresOrdenados(estadoActual)) {
                boolean soyYo = p.id == conexion.getJugadorId();
                boolean anfitrion = p.id == estadoActual.anfitrionId;
                fuenteChica.setColor(soyYo ? Color.YELLOW : Color.WHITE);
                dibujarAjustado(fuenteChica, (anfitrion ? "* " : "") + p.nombre + (soyYo ? " (vos)" : ""), 30, y, 82);
                fuenteChica.setColor(KitUi.colorPersonaje(TipoPersonaje.desdeId(p.personaje)));
                fuenteChica.draw(batch, TipoPersonaje.desdeId(p.personaje).nombreVisible, 116, y);
                if (estadoActual.getModo() == ModoJuego.EQUIPOS) {
                    fuenteChica.setColor(KitUi.colorJugador(ModoJuego.EQUIPOS, p.equipo));
                    fuenteChica.draw(batch, p.equipo == Equipo.AZUL ? "Azul" : "Rojo", 160, y);
                }
                fuenteChica.setColor(p.listo ? Color.GREEN : Color.GRAY);
                fuenteChica.draw(batch, p.listo ? "LISTO" : "...", 200, y);
                y -= 15;
            }
            for (int i = cantidad; i < Protocolo.MAX_JUGADORES; i++) {
                fuenteChica.setColor(0.4f, 0.45f, 0.55f, 1f);
                fuenteChica.draw(batch, "- libre -", 30, y);
                y -= 15;
            }
        }

        // --- Modo de juego ---
        fuente.setColor(Color.WHITE);
        fuente.draw(batch, "Modo de juego", 258, 224);
        dibujarTextoBoton(botonTodosContraTodos, ModoJuego.TODOS_CONTRA_TODOS.nombreVisible);
        dibujarTextoBoton(botonEquipos, ModoJuego.EQUIPOS.nombreVisible);
        if (estadoActual != null) {
            fuenteChica.setColor(Color.LIGHT_GRAY);
            String pista;
            if (!soyAnfitrion(estadoActual)) {
                EstadoMundo.EstadoJugador anfitrion = estadoActual.buscarJugador(estadoActual.anfitrionId);
                pista = "Elige el anfitrión" + (anfitrion == null ? "" : ": " + anfitrion.nombre);
            } else if (modoPendiente != null) {
                fuenteChica.setColor(KitUi.AMARILLO);
                pista = "Cambiando a " + modoPendiente.nombreVisible + "...";
            } else if (cantidad < ModoJuego.EQUIPOS.jugadoresNecesarios) {
                pista = "SOS EL ANFITRIÓN - Equipos necesita 4 (" + cantidad + "/4)";
            } else {
                fuenteChica.setColor(KitUi.AMARILLO);
                pista = "SOS EL ANFITRIÓN - elegí el modo (tecla M)";
            }
            dibujarAjustado(fuenteChica, pista, 258, 143, 204);
        }

        // --- Tarjetas de personaje ---
        for (TipoPersonaje tipo : TipoPersonaje.values()) {
            Rectangle tarjeta = tarjetasPersonaje[tipo.id()];
            Color acento = KitUi.colorPersonaje(tipo);
            batch.draw(retratos[tipo.id()], tarjeta.x + 8, tarjeta.y + 31, 38, 38);
            float tx = tarjeta.x + 50;
            float anchoMax = tarjeta.width - 54;
            fuente.setColor(Color.WHITE);
            dibujarAjustado(fuente, (tipo.id() + 1) + " " + tipo.nombreVisible, tx, tarjeta.y + tarjeta.height - 7, anchoMax);
            fuenteChica.setColor(Color.GRAY);
            dibujarAjustado(fuenteChica, tipo.rol, tx, tarjeta.y + tarjeta.height - 22, anchoMax);
            fuenteChica.setColor(Color.LIGHT_GRAY);
            dibujarAjustado(fuenteChica, "Vida " + (int) tipo.vidaMaxima, tx, tarjeta.y + tarjeta.height - 35, anchoMax);
            dibujarAjustado(fuenteChica, "Daño " + (int) tipo.danio + "  Vel " + (int) tipo.velocidad,
                tarjeta.x + 9, tarjeta.y + 24, tarjeta.width - 14);
            fuenteChica.setColor(acento);
            dibujarAjustado(fuenteChica, tipo.nombreHabilidad, tarjeta.x + 9, tarjeta.y + 12, tarjeta.width - 14);
        }

        fuente.setColor(Color.WHITE);
        if (tiempoAviso > 0 && aviso != null) {
            // Los avisos reemplazan por un momento al texto de estado
            fuenteChica.setColor(colorAviso);
            dibujarCentrado(fuenteChica, aviso, ANCHO / 2f, 46);
        } else if (estadoActual != null) {
            dibujarCentrado(fuenteChica, textoEstado(estadoActual), ANCHO / 2f, 46);
        }

        boolean listo = conexion != null && conexion.estaListo();
        dibujarTextoBoton(botonListo, listo ? "No estoy listo" : "Listo (ENTER)");
        dibujarTextoBoton(botonSalir, "Salir (ESC)");
    }

    private String textoEstado(EstadoMundo estadoActual) {
        fuenteChica.setColor(Color.WHITE);
        if (estadoActual.fase == FasePartida.CUENTA_REGRESIVA) {
            fuenteChica.setColor(KitUi.AMARILLO);
            return "La partida comienza en " + (int) Math.ceil(estadoActual.tiempoFase) + "...";
        }
        if (estadoActual.jugadores.size() < ConstantesJuego.MIN_JUGADORES) {
            return "Esperando jugadores (minimo " + ConstantesJuego.MIN_JUGADORES + ")";
        }
        return "Elegi tu personaje (1 a 4) y marca Listo";
    }

    static String textoPing(RedCliente conexion) {
        float ping = conexion.getPingMs();
        return ping < 0 ? "" : "  -  " + Math.round(ping) + " ms";
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Texture t : hojas) t.dispose();
    }
}
