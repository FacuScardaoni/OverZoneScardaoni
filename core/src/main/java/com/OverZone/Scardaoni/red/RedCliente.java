package com.OverZone.Scardaoni.red;

import com.OverZone.Scardaoni.compartido.TipoPersonaje;
import com.OverZone.Scardaoni.compartido.ModoJuego;
import com.OverZone.Scardaoni.compartido.EntradaJugador;
import com.OverZone.Scardaoni.compartido.Protocolo;
import com.OverZone.Scardaoni.compartido.EstadoMundo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class RedCliente {
    public enum EstadoConexion { INACTIVO, BUSCANDO, CONECTANDO, CONECTADO, FALLIDO }

    private static final long REINTENTO_BUSQUEDA_MS = 700;
    private static final int INTENTOS_BUSQUEDA = 5;
    private static final long REINTENTO_CONEXION_MS = 500;
    private static final int INTENTOS_CONEXION = 6;

    /** Las dos últimas fotos recibidas, juntas, para poder interpolar entre ellas. */
    public static class ParEstados {
        public final EstadoMundo anterior;
        public final EstadoMundo actual;

        ParEstados(EstadoMundo anterior, EstadoMundo actual) {
            this.anterior = anterior;
            this.actual = actual;
        }
    }

    private final String nombreJugador;

    private DatagramSocket socket;
    private Thread hilo;
    private volatile boolean corriendo = false;

    // "volatile": los escribe el hilo de red y los lee el hilo de dibujo.
    private volatile EstadoConexion estadoConexion = EstadoConexion.INACTIVO;
    private volatile String mensajeFalla = "";
    private volatile String nombreServidor = "";
    private volatile int jugadorId = -1;
    private volatile boolean listo = false;
    private volatile TipoPersonaje personaje = TipoPersonaje.VOLT;
    private volatile int contadorPedidosModo = 0;
    private volatile ModoJuego modoPedido = ModoJuego.TODOS_CONTRA_TODOS;
    private volatile ParEstados estados = null;
    private volatile float pingMs = -1f;

    private InetAddress direccionServidor;
    private int puertoServidor;
    private volatile long ultimoPaqueteServidorNanos;

    private long secuenciaEntrada = 0;
    private int contadorHabilidad = 0;

    public RedCliente(String nombreJugador) {
        this.nombreJugador = Protocolo.limpiarNombre(nombreJugador);
    }

    // ------------------------------------------------------------------
    // Inicio y cierre
    // ------------------------------------------------------------------

    /** Abre el socket y arranca el hilo que busca el servidor. */
    public void iniciar() {
        try {
            // Sin número de puerto: el sistema elige uno libre. Así se pueden
            // abrir varios clientes en la misma computadora.
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(100);
        } catch (SocketException e) {
            fallar("No se pudo abrir la conexion de red:\n" + e.getMessage());
            return;
        }
        corriendo = true;
        estadoConexion = EstadoConexion.BUSCANDO;
        hilo = new Thread(this::run, "RedCliente");
        // Hilo "daemon": si el juego se cierra, no queda ejecutándose solo.
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Avisa al servidor que se va, cierra el socket y espera que el hilo termine. */
    public void detener() {
        if (estadoConexion == EstadoConexion.CONECTADO) {
            // Se manda dos veces por si una se pierde. Si las dos se pierden,
            // el servidor igual lo detecta por tiempo de espera.
            enviar(Protocolo.DISCONNECT);
            enviar(Protocolo.DISCONNECT);
        }
        corriendo = false;
        if (socket != null) socket.close();
        if (hilo != null && hilo != Thread.currentThread()) {
            try {
                hilo.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (estadoConexion != EstadoConexion.FALLIDO) estadoConexion = EstadoConexion.INACTIVO;
    }

    // ------------------------------------------------------------------
    // Hilo de red
    // ------------------------------------------------------------------

    private void run() {
        byte[] bufer = new byte[Protocolo.TAMANIO_MAX_PAQUETE];
        long ultimoIntentoMs = 0;
        int intentos = 0;
        EstadoConexion intentosPara = null;
        long ultimoLatidoMs = 0;

        while (corriendo) {
            long ahoraMs = System.currentTimeMillis();

            // Cuando cambia la etapa, se reinicia la cuenta de intentos.
            if (intentosPara != estadoConexion) {
                intentosPara = estadoConexion;
                intentos = 0;
                ultimoIntentoMs = 0;
            }

            switch (estadoConexion) {
                case BUSCANDO:
                    if (ahoraMs - ultimoIntentoMs >= REINTENTO_BUSQUEDA_MS) {
                        if (intentos >= INTENTOS_BUSQUEDA) {
                            fallar("No se encontro ningun servidor en la red local.\n"
                                + "Revisa que el servidor este abierto, que las computadoras\n"
                                + "esten en la misma red y que el firewall permita Java.");
                            break;
                        }
                        enviarBusqueda();
                        intentos++;
                        ultimoIntentoMs = ahoraMs;
                    }
                    break;
                case CONECTANDO:
                    if (ahoraMs - ultimoIntentoMs >= REINTENTO_CONEXION_MS) {
                        if (intentos >= INTENTOS_CONEXION) {
                            fallar("Se encontro el servidor, pero no respondio al pedido de conexion.");
                            break;
                        }
                        enviar(Protocolo.unir(Protocolo.CONNECT, nombreJugador, Protocolo.VERSION));
                        intentos++;
                        ultimoIntentoMs = ahoraMs;
                    }
                    break;
                case CONECTADO:
                    if (ahoraMs - ultimoLatidoMs >= Protocolo.INTERVALO_LATIDO_MS) {
                        enviarLatido();
                        ultimoLatidoMs = ahoraMs;
                    }
                    long silencioMs = (System.nanoTime() - ultimoPaqueteServidorNanos) / 1_000_000L;
                    if (silencioMs > Protocolo.TIEMPO_ESPERA_SERVIDOR_MS) {
                        fallar("Se perdio la conexion con el servidor\n(no llegan datos desde hace "
                            + (silencioMs / 1000) + " segundos).");
                    }
                    break;
                default:
                    break;
            }
            if (!corriendo) break;

            // Espera un paquete como máximo 100 ms (setSoTimeout); así el
            // bucle vuelve a revisar reintentos y tiempos de espera.
            try {
                DatagramPacket paquete = new DatagramPacket(bufer, bufer.length);
                socket.receive(paquete);
                String msj = new String(paquete.getData(), 0, paquete.getLength(), StandardCharsets.UTF_8);
                procesarMensaje(msj, paquete.getAddress(), paquete.getPort());
            } catch (SocketTimeoutException e) {
                // No llegó nada en este intervalo: es normal.
            } catch (IOException e) {
                // El socket se cerró (stop) o hubo un error de red.
                if (corriendo) fallar("Error de red: " + e.getMessage());
            }
        }
        if (socket != null) socket.close();
    }

    private void procesarMensaje(String msj, InetAddress desde, int puertoOrigen) {
        String[] partes = msj.split(";", -1);
        String tipo = partes[0];

        // Mientras se busca, se acepta la primera respuesta de un servidor.
        if (estadoConexion == EstadoConexion.BUSCANDO) {
            if (tipo.equals(Protocolo.SERVER_INFO)) {
                int versionServidor = partes.length > 4 ? enteroO(partes[4], -1) : -1;
                if (versionServidor != Protocolo.VERSION) {
                    fallar(mensajeVersion(versionServidor));
                    return;
                }
                direccionServidor = desde;
                puertoServidor = puertoOrigen;
                nombreServidor = partes.length > 1 ? partes[1] : "";
                ultimoPaqueteServidorNanos = System.nanoTime();
                // El pedido CONNECT lo envía el bucle en la próxima vuelta.
                estadoConexion = EstadoConexion.CONECTANDO;
            }
            return;
        }

        // Después de elegir un servidor, se ignora todo lo que venga de otro lado.
        if (!desde.equals(direccionServidor) || puertoOrigen != puertoServidor) return;
        ultimoPaqueteServidorNanos = System.nanoTime();

        try {
            switch (tipo) {
                case Protocolo.WELCOME:
                    jugadorId = Integer.parseInt(partes[1]);
                    estadoConexion = EstadoConexion.CONECTADO;
                    break;
                case Protocolo.REJECT:
                    String motivo = partes.length > 1 ? partes[1] : "";
                    if (motivo.equals(Protocolo.MOTIVO_VERSION)) {
                        fallar(mensajeVersion(partes.length > 2 ? enteroO(partes[2], -1) : -1));
                    } else if (motivo.equals(Protocolo.MOTIVO_LLENO)) {
                        fallar("La partida esta llena (maximo " + Protocolo.MAX_JUGADORES + " jugadores).");
                    } else {
                        fallar("El servidor no reconoce esta conexion\n(puede que el servidor se haya reiniciado).");
                    }
                    break;
                case Protocolo.STATE:
                    if (estadoConexion != EstadoConexion.CONECTADO) break;
                    EstadoMundo estadoActual = EstadoMundo.interpretar(msj, System.nanoTime());
                    ParEstados par = estados;
                    // UDP no garantiza el orden: si llega una foto más vieja
                    // que la que ya tenemos, se descarta.
                    if (par == null) {
                        estados = new ParEstados(estadoActual, estadoActual);
                    } else if (estadoActual.secuencia > par.actual.secuencia) {
                        estados = new ParEstados(par.actual, estadoActual);
                    }
                    break;
                case Protocolo.PONG:
                    long enviadoNanos = Long.parseLong(partes[1]);
                    pingMs = (System.nanoTime() - enviadoNanos) / 1_000_000f;
                    break;
                case Protocolo.SHUTDOWN:
                    fallar("El servidor se cerro.");
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            // Mensaje mal formado: se ignora en lugar de cerrar el juego.
        }
    }

    // ------------------------------------------------------------------
    // Envíos
    // ------------------------------------------------------------------

    private void enviarBusqueda() {
        byte[] datos = Protocolo.DISCOVER.getBytes(StandardCharsets.UTF_8);
        for (InetAddress objetivo : destinosBusqueda()) {
            try {
                socket.send(new DatagramPacket(datos, datos.length, objetivo, Protocolo.PUERTO_SERVIDOR));
            } catch (IOException e) {
                // Una dirección que falla no impide probar las demás.
            }
        }
    }

    private List<InetAddress> destinosBusqueda() {
        List<InetAddress> destinos = new ArrayList<>();
        try {
            destinos.add(InetAddress.getByName("255.255.255.255"));
            destinos.add(InetAddress.getLoopbackAddress());
            // Windows con varias placas de red puede mandar el 255.255.255.255
            // por la placa equivocada; por eso también se usa la dirección de
            // broadcast de cada placa (por ejemplo 192.168.0.255).
            Enumeration<NetworkInterface> interfacesRed = NetworkInterface.getNetworkInterfaces();
            while (interfacesRed != null && interfacesRed.hasMoreElements()) {
                NetworkInterface ni = interfacesRed.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress difusion = ia.getBroadcast();
                    if (difusion != null && !destinos.contains(difusion)) destinos.add(difusion);
                }
            }
        } catch (IOException e) {
            // Si no se pueden listar las placas, quedan las direcciones generales.
        }
        return destinos;
    }

    private void enviarLatido() {
        enviar(Protocolo.unir(Protocolo.PING, listo ? 1 : 0, personaje.id(),
            contadorPedidosModo, modoPedido.id(), System.nanoTime()));
    }

    public void enviarEntrada(float moverX, float moverY, float apuntarX, float apuntarY,
                          boolean disparar, boolean habilidadApretada) {
        if (estadoConexion != EstadoConexion.CONECTADO) return;
        if (habilidadApretada) contadorHabilidad++;
        secuenciaEntrada++;
        enviar(new EntradaJugador(secuenciaEntrada, moverX, moverY, apuntarX, apuntarY, disparar, contadorHabilidad).aMensaje());
    }

    private void enviar(String mensaje) {
        if (socket == null || socket.isClosed() || direccionServidor == null) return;
        byte[] datos = mensaje.getBytes(StandardCharsets.UTF_8);
        try {
            socket.send(new DatagramPacket(datos, datos.length, direccionServidor, puertoServidor));
        } catch (IOException e) {
            // Un envío fallido no es grave: el próximo lo reemplaza. Si la
            // conexión se cortó de verdad, lo detecta el tiempo de espera.
        }
    }

    private static String mensajeVersion(int versionServidor) {
        return "El servidor usa otra version del protocolo (servidor: " + versionServidor
            + ", cliente: " + Protocolo.VERSION + ").\n"
            + "Actualiza el cliente y el servidor a la misma version.";
    }

    private static int enteroO(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private void fallar(String mensaje) {
        mensajeFalla = mensaje;
        estadoConexion = EstadoConexion.FALLIDO;
        corriendo = false;
    }

    // ------------------------------------------------------------------
    // Consultas desde las pantallas
    // ------------------------------------------------------------------

    public void setListo(boolean listo) {
        this.listo = listo;
        enviarLatido(); // se avisa enseguida, sin esperar el próximo PING
    }

    public boolean estaListo() { return listo; }

    /** Elige el personaje (el servidor solo lo acepta en la sala de espera). */
    public void setPersonaje(TipoPersonaje personaje) {
        this.personaje = personaje;
        enviarLatido();
    }

    public TipoPersonaje getPersonaje() { return personaje; }

    public synchronized void pedirModo(ModoJuego modo) {
        modoPedido = modo;
        contadorPedidosModo++;
        enviarLatido();
    }
    public EstadoConexion getEstadoConexion() { return estadoConexion; }
    public String getMensajeFalla() { return mensajeFalla; }
    public String getNombreServidor() { return nombreServidor; }
    public String getDireccionServidor() { return direccionServidor == null ? "" : direccionServidor.getHostAddress(); }
    public int getJugadorId() { return jugadorId; }
    public float getPingMs() { return pingMs; }
    public ParEstados getEstados() { return estados; }

    public EstadoMundo getUltimoEstado() {
        ParEstados par = estados;
        return par == null ? null : par.actual;
    }
}
