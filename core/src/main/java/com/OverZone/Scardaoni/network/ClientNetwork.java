package com.OverZone.Scardaoni.network;

import com.OverZone.Scardaoni.shared.PlayerInput;
import com.OverZone.Scardaoni.shared.Protocol;
import com.OverZone.Scardaoni.shared.WorldSnapshot;

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

/**
 * Parte de red del CLIENTE.
 *
 * Tiene un socket UDP y un hilo propio que:
 *  1. Busca el servidor enviando un broadcast a toda la red local.
 *  2. Se conecta al servidor que respondió.
 *  3. Recibe el estado de la partida y lo deja disponible para las pantallas.
 *  4. Cada 250 ms avisa que sigue conectado (PING), y mide el retardo.
 *  5. Si el servidor deja de responder, marca la conexión como perdida.
 *
 * Las pantallas (hilo de dibujo de LibGDX) solo leen el estado y mandan las
 * entradas del jugador. No usa nada de LibGDX, así que se puede probar sin
 * ventana.
 */
public class ClientNetwork {

    public enum Status { IDLE, SEARCHING, CONNECTING, CONNECTED, FAILED }

    private static final long DISCOVERY_RETRY_MS = 700;
    private static final int DISCOVERY_ATTEMPTS = 5;
    private static final long CONNECT_RETRY_MS = 500;
    private static final int CONNECT_ATTEMPTS = 6;

    /** Las dos últimas fotos recibidas, juntas, para poder interpolar entre ellas. */
    public static class SnapshotPair {
        public final WorldSnapshot previous;
        public final WorldSnapshot current;

        SnapshotPair(WorldSnapshot previous, WorldSnapshot current) {
            this.previous = previous;
            this.current = current;
        }
    }

    private final String playerName;

    private DatagramSocket socket;
    private Thread thread;
    private volatile boolean running = false;

    // "volatile": los escribe el hilo de red y los lee el hilo de dibujo.
    private volatile Status status = Status.IDLE;
    private volatile String failureMessage = "";
    private volatile String serverName = "";
    private volatile int playerId = -1;
    private volatile boolean ready = false;
    private volatile SnapshotPair snapshots = null;
    private volatile float pingMs = -1f;

    private InetAddress serverAddress;
    private int serverPort;
    private volatile long lastServerPacketNanos;

    private long inputSeq = 0;
    private int abilityCount = 0;

    public ClientNetwork(String playerName) {
        this.playerName = Protocol.sanitizeName(playerName);
    }

    // ------------------------------------------------------------------
    // Inicio y cierre
    // ------------------------------------------------------------------

    /** Abre el socket y arranca el hilo que busca el servidor. */
    public void start() {
        try {
            // Sin número de puerto: el sistema elige uno libre. Así se pueden
            // abrir varios clientes en la misma computadora.
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(100);
        } catch (SocketException e) {
            fail("No se pudo abrir la conexion de red:\n" + e.getMessage());
            return;
        }
        running = true;
        status = Status.SEARCHING;
        thread = new Thread(this::run, "ClientNetwork");
        // Hilo "daemon": si el juego se cierra, no queda ejecutándose solo.
        thread.setDaemon(true);
        thread.start();
    }

    /** Avisa al servidor que se va, cierra el socket y espera que el hilo termine. */
    public void stop() {
        if (status == Status.CONNECTED) {
            // Se manda dos veces por si una se pierde. Si las dos se pierden,
            // el servidor igual lo detecta por tiempo de espera.
            send(Protocol.DISCONNECT);
            send(Protocol.DISCONNECT);
        }
        running = false;
        if (socket != null) socket.close();
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (status != Status.FAILED) status = Status.IDLE;
    }

    // ------------------------------------------------------------------
    // Hilo de red
    // ------------------------------------------------------------------

    private void run() {
        byte[] buffer = new byte[Protocol.MAX_PACKET_SIZE];
        long lastAttemptMs = 0;
        int attempts = 0;
        Status attemptsFor = null;
        long lastHeartbeatMs = 0;

        while (running) {
            long nowMs = System.currentTimeMillis();

            // Cuando cambia la etapa, se reinicia la cuenta de intentos.
            if (attemptsFor != status) {
                attemptsFor = status;
                attempts = 0;
                lastAttemptMs = 0;
            }

            switch (status) {
                case SEARCHING:
                    if (nowMs - lastAttemptMs >= DISCOVERY_RETRY_MS) {
                        if (attempts >= DISCOVERY_ATTEMPTS) {
                            fail("No se encontro ningun servidor en la red local.\n"
                                + "Revisa que el servidor este abierto, que las computadoras\n"
                                + "esten en la misma red y que el firewall permita Java.");
                            break;
                        }
                        sendDiscovery();
                        attempts++;
                        lastAttemptMs = nowMs;
                    }
                    break;
                case CONNECTING:
                    if (nowMs - lastAttemptMs >= CONNECT_RETRY_MS) {
                        if (attempts >= CONNECT_ATTEMPTS) {
                            fail("Se encontro el servidor, pero no respondio al pedido de conexion.");
                            break;
                        }
                        send(Protocol.join(Protocol.CONNECT, playerName, Protocol.VERSION));
                        attempts++;
                        lastAttemptMs = nowMs;
                    }
                    break;
                case CONNECTED:
                    if (nowMs - lastHeartbeatMs >= Protocol.HEARTBEAT_INTERVAL_MS) {
                        sendHeartbeat();
                        lastHeartbeatMs = nowMs;
                    }
                    long silenceMs = (System.nanoTime() - lastServerPacketNanos) / 1_000_000L;
                    if (silenceMs > Protocol.SERVER_TIMEOUT_MS) {
                        fail("Se perdio la conexion con el servidor\n(no llegan datos desde hace "
                            + (silenceMs / 1000) + " segundos).");
                    }
                    break;
                default:
                    break;
            }
            if (!running) break;

            // Espera un paquete como máximo 100 ms (setSoTimeout); así el
            // bucle vuelve a revisar reintentos y tiempos de espera.
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                handleMessage(msg, packet.getAddress(), packet.getPort());
            } catch (SocketTimeoutException e) {
                // No llegó nada en este intervalo: es normal.
            } catch (IOException e) {
                // El socket se cerró (stop) o hubo un error de red.
                if (running) fail("Error de red: " + e.getMessage());
            }
        }
        if (socket != null) socket.close();
    }

    private void handleMessage(String msg, InetAddress from, int fromPort) {
        String[] parts = msg.split(";", -1);
        String type = parts[0];

        // Mientras se busca, se acepta la primera respuesta de un servidor.
        if (status == Status.SEARCHING) {
            if (type.equals(Protocol.SERVER_INFO)) {
                int serverVersion = parts.length > 4 ? parseIntOr(parts[4], -1) : -1;
                if (serverVersion != Protocol.VERSION) {
                    fail(versionMessage(serverVersion));
                    return;
                }
                serverAddress = from;
                serverPort = fromPort;
                serverName = parts.length > 1 ? parts[1] : "";
                lastServerPacketNanos = System.nanoTime();
                // El pedido CONNECT lo envía el bucle en la próxima vuelta.
                status = Status.CONNECTING;
            }
            return;
        }

        // Después de elegir un servidor, se ignora todo lo que venga de otro lado.
        if (!from.equals(serverAddress) || fromPort != serverPort) return;
        lastServerPacketNanos = System.nanoTime();

        try {
            switch (type) {
                case Protocol.WELCOME:
                    playerId = Integer.parseInt(parts[1]);
                    status = Status.CONNECTED;
                    break;
                case Protocol.REJECT:
                    String reason = parts.length > 1 ? parts[1] : "";
                    if (reason.equals(Protocol.REASON_VERSION)) {
                        fail(versionMessage(parts.length > 2 ? parseIntOr(parts[2], -1) : -1));
                    } else if (reason.equals(Protocol.REASON_FULL)) {
                        fail("La partida esta llena (maximo " + Protocol.MAX_PLAYERS + " jugadores).");
                    } else {
                        fail("El servidor no reconoce esta conexion\n(puede que el servidor se haya reiniciado).");
                    }
                    break;
                case Protocol.STATE:
                    if (status != Status.CONNECTED) break;
                    WorldSnapshot snap = WorldSnapshot.parse(msg, System.nanoTime());
                    SnapshotPair pair = snapshots;
                    // UDP no garantiza el orden: si llega una foto más vieja
                    // que la que ya tenemos, se descarta.
                    if (pair == null) {
                        snapshots = new SnapshotPair(snap, snap);
                    } else if (snap.seq > pair.current.seq) {
                        snapshots = new SnapshotPair(pair.current, snap);
                    }
                    break;
                case Protocol.PONG:
                    long sentNanos = Long.parseLong(parts[1]);
                    pingMs = (System.nanoTime() - sentNanos) / 1_000_000f;
                    break;
                case Protocol.SHUTDOWN:
                    fail("El servidor se cerro.");
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

    /**
     * Envía el pedido de descubrimiento a todas las direcciones de broadcast
     * de la computadora, a la dirección de broadcast general y a la propia
     * computadora (por si el servidor corre en la misma PC).
     */
    private void sendDiscovery() {
        byte[] data = Protocol.DISCOVER.getBytes(StandardCharsets.UTF_8);
        for (InetAddress target : discoveryTargets()) {
            try {
                socket.send(new DatagramPacket(data, data.length, target, Protocol.SERVER_PORT));
            } catch (IOException e) {
                // Una dirección que falla no impide probar las demás.
            }
        }
    }

    private List<InetAddress> discoveryTargets() {
        List<InetAddress> targets = new ArrayList<>();
        try {
            targets.add(InetAddress.getByName("255.255.255.255"));
            targets.add(InetAddress.getLoopbackAddress());
            // Windows con varias placas de red puede mandar el 255.255.255.255
            // por la placa equivocada; por eso también se usa la dirección de
            // broadcast de cada placa (por ejemplo 192.168.0.255).
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress broadcast = ia.getBroadcast();
                    if (broadcast != null && !targets.contains(broadcast)) targets.add(broadcast);
                }
            }
        } catch (IOException e) {
            // Si no se pueden listar las placas, quedan las direcciones generales.
        }
        return targets;
    }

    private void sendHeartbeat() {
        send(Protocol.join(Protocol.PING, ready ? 1 : 0, System.nanoTime()));
    }

    /**
     * Envía las entradas del jugador. Lo llama la pantalla de juego en cada
     * frame. Cuenta como pedido de habilidad si abilityPressed es true.
     */
    public void sendInput(float moveX, float moveY, float aimX, float aimY,
                          boolean fire, boolean abilityPressed) {
        if (status != Status.CONNECTED) return;
        if (abilityPressed) abilityCount++;
        inputSeq++;
        send(new PlayerInput(inputSeq, moveX, moveY, aimX, aimY, fire, abilityCount).toMessage());
    }

    private void send(String message) {
        if (socket == null || socket.isClosed() || serverAddress == null) return;
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        try {
            socket.send(new DatagramPacket(data, data.length, serverAddress, serverPort));
        } catch (IOException e) {
            // Un envío fallido no es grave: el próximo lo reemplaza. Si la
            // conexión se cortó de verdad, lo detecta el tiempo de espera.
        }
    }

    private static String versionMessage(int serverVersion) {
        return "El servidor usa otra version del protocolo (servidor: " + serverVersion
            + ", cliente: " + Protocol.VERSION + ").\n"
            + "Actualiza el cliente y el servidor a la misma version.";
    }

    private static int parseIntOr(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void fail(String message) {
        failureMessage = message;
        status = Status.FAILED;
        running = false;
    }

    // ------------------------------------------------------------------
    // Consultas desde las pantallas
    // ------------------------------------------------------------------

    public void setReady(boolean ready) {
        this.ready = ready;
        sendHeartbeat(); // se avisa enseguida, sin esperar el próximo PING
    }

    public boolean isReady() { return ready; }
    public Status getStatus() { return status; }
    public String getFailureMessage() { return failureMessage; }
    public String getServerName() { return serverName; }
    public String getServerAddress() { return serverAddress == null ? "" : serverAddress.getHostAddress(); }
    public int getPlayerId() { return playerId; }
    public float getPingMs() { return pingMs; }
    public SnapshotPair getSnapshots() { return snapshots; }

    public WorldSnapshot getLatestSnapshot() {
        SnapshotPair pair = snapshots;
        return pair == null ? null : pair.current;
    }
}
