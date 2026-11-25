# Equivalencias de nombres (inglés → español)

En la versión 0.8.0 todos los nombres propios del código (clases, paquetes, métodos, campos, variables y constantes) pasaron a estar en español, en los dos repositorios. Esta tabla sirve para ubicar los nombres viejos, por ejemplo en apuntes o en el historial de GitHub.

## Qué quedó en inglés y por qué

* **Lo que viene de LibGDX o de Java**, porque lo define la librería y no se puede renombrar: `render`, `show`, `dispose`, `resize`, `create`, `touchDown`, `keyUp`, `SpriteBatch`, `ShapeRenderer`, `Rectangle`, `Thread`, `DatagramSocket`, `toString`, `main`, etc. Por ejemplo, LibGDX llama solo al método `render` de cada pantalla 60 veces por segundo: si se llamara `dibujar`, LibGDX no lo encontraría.
* **Las palabras del protocolo** que viajan por la red: `PING`, `PONG`, `STATE`, `INPUT`, `CONNECT`, `WELCOME`, `REJECT`, `DISCONNECT`, `SHUTDOWN`, `OVERZONE_DISCOVER`. Son el "vocabulario" de los mensajes y conviene que sean cortas y fijas.
* **Los nombres propios de los personajes** (`VOLT`, `BRUTUS`, `IRIS`, `PHANTOM`) y de las facciones.
* **Variables de una o dos letras** de uso matemático: `x`, `y`, `dx`, `dy`, `i`, `delta`, `id`.
* Los prefijos `get`, `set` (por ejemplo `getVida()`), que son la convención de Java para leer y modificar campos.

## Paquetes

| Antes | Ahora | Contenido |
| --- | --- | --- |
| `shared` | `compartido` | Código compartido con el servidor (idéntico en los dos repos) |
| `network` | `red` | Conexión del cliente con el servidor |
| `screens` | `pantallas` | Pantallas del juego |
| `ui` | `interfaz` | HUD, botones, fuentes y estilo de menús |
| `input` | `entrada` | Teclado y mouse |
| `graphics` | `graficos` | Animaciones |
| `server` | `servidor` | Programa del servidor |
| `server.game` | `servidor.juego` | La partida real (solo en el servidor) |

## Clases del cliente

| Antes | Ahora |
| --- | --- |
| `Main` | `JuegoOverZone` |
| `ClientNetwork` | `RedCliente` |
| `PlayerInputProcessor` | `ProcesadorEntrada` |
| `DirectionalAnimation` | `AnimacionDireccional` |
| `AudioManager` | `GestorAudio` |
| `BaseMenuScreen` | `PantallaMenuBase` |
| `MenuScreen` | `PantallaMenu` |
| `ConnectScreen` | `PantallaConexion` |
| `LobbyScreen` | `PantallaSala` |
| `GameScreen` | `PantallaJuego` |
| `PauseScreen` | `PantallaPausa` |
| `ControlsScreen` | `PantallaControles` |
| `ResultsScreen` | `PantallaResultados` |
| `MessageScreen` | `PantallaMensaje` |
| `Hud` | `Hud` |
| `HudControls` | `ControlesHud` |
| `UiKit` | `KitUi` |
| `UiDraw` | `DibujoUi` |
| `MenuBackground` | `FondoMenu` |
| `Lwjgl3Launcher` | `LanzadorEscritorio` |

## Clases compartidas (paquete `compartido`)

| Antes | Ahora |
| --- | --- |
| `Protocol` | `Protocolo` |
| `WorldSnapshot` | `EstadoMundo` |
| `PlayerInput` | `EntradaJugador` |
| `MatchPhase` | `FasePartida` |
| `GameMode` | `ModoJuego` |
| `CharacterType` | `TipoPersonaje` |
| `PowerUpType` | `TipoPotenciador` |
| `Team` | `Equipo` |
| `Direction` | `Direccion` |
| `GameConstants` | `ConstantesJuego` |
| `Arena` | `Arena` |
| `Obstacle` | `Obstaculo` |

## Clases del servidor

| Antes | Ahora |
| --- | --- |
| `ServerLauncher` | `LanzadorServidor` |
| `GameServer` | `ServidorJuego` |
| `ConnectedClient` | `ClienteConectado` |
| `MatchSimulation` | `SimulacionPartida` |
| `Player` | `Jugador` |
| `Projectile` | `Proyectil` |
| `RegenZone` | `ZonaRegeneracion` |
| `PowerUp` | `Potenciador` |

## Métodos importantes

| Clase | Antes | Ahora |
| --- | --- | --- |
| `JuegoOverZone` | `followServer` | `seguirServidor` |
| `RedCliente` | `sendDiscovery` | `enviarBusqueda` |
| `RedCliente` | `sendHeartbeat` | `enviarLatido` |
| `RedCliente` | `sendInput` | `enviarEntrada` |
| `RedCliente` | `getLatestSnapshot` | `getUltimoEstado` |
| `RedCliente` | `requestMode` | `pedirModo` |
| `PantallaJuego` | `sendInputs` | `enviarEntradas` |
| `PantallaJuego` | `lerpX` | `interpolarX` |
| `PantallaJuego` | `interpolationAlpha` | `alfaInterpolacion` |
| `PantallaJuego` | `playSoundEvents` | `sonarEventos` |
| `PantallaJuego` | `detectPowerUpPickups` | `detectarRecogidas` |
| `ServidorJuego` | `receiveLoop` | `bucleRecepcion` |
| `ServidorJuego` | `gameLoop` | `bucleJuego` |
| `ServidorJuego` | `processInbox` | `procesarBandejaEntrada` |
| `ServidorJuego` | `handleMessage` | `procesarMensaje` |
| `ServidorJuego` | `dropSilentClients` | `sacarClientesSilenciosos` |
| `ServidorJuego` | `broadcastState` | `difundirEstado` |
| `ServidorJuego` | `handleCommand` | `procesarComando` |
| `SimulacionPartida` | `update` | `actualizar` |
| `SimulacionPartida` | `applyInput` | `aplicarEntrada` |
| `SimulacionPartida` | `requestMode` | `pedirModo` |
| `SimulacionPartida` | `handleMovement` | `procesarMovimiento` |
| `SimulacionPartida` | `handleShooting` | `procesarDisparo` |
| `SimulacionPartida` | `resolveCollisions` | `resolverColisiones` |
| `SimulacionPartida` | `updatePowerUps` | `actualizarPotenciadores` |
| `SimulacionPartida` | `updateZones` | `actualizarZonas` |
| `SimulacionPartida` | `endMatch` | `terminarPartida` |
| `SimulacionPartida` | `createSnapshot` | `crearEstado` |
| `Jugador` | `takeDamage` | `recibirDanio` |
| `Jugador` | `tryUseAbility` | `intentarUsarHabilidad` |
| `Jugador` | `applyPowerUp` | `aplicarPotenciador` |
| `Jugador` | `respawn` | `reaparecer` |
| `EstadoMundo` | `toMessage` | `aMensaje` |
| `EstadoMundo` | `parse` | `interpretar` |
| `EntradaJugador` | `parse` | `interpretar` |
| `Protocolo` | `join` | `unir` |

## Etapas de la partida (`FasePartida`)

| Antes | Ahora |
| --- | --- |
| `LOBBY` | `SALA` |
| `COUNTDOWN` | `CUENTA_REGRESIVA` |
| `PLAYING` | `JUGANDO` |
| `RESULTS` | `RESULTADOS` |

## Modos y potenciadores

| Antes | Ahora |
| --- | --- |
| `FREE_FOR_ALL` | `TODOS_CONTRA_TODOS` |
| `TEAMS` | `EQUIPOS` |
| `HEALTH` | `BOTIQUIN` |
| `OVERLOAD` | `SOBRECARGA` |
| `ARMOR` | `BLINDAJE` |
| `TURBO` | `TURBO` |

Los Power-Ups en el código se llaman **potenciadores** (`TipoPotenciador`, `Potenciador`, `aplicarPotenciador`).
