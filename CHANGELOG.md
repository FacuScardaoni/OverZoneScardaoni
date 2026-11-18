# Changelog

Todos los cambios importantes realizados en **OverZone** serán documentados en este archivo.

El formato utilizado está basado en el estándar **Keep a Changelog**, organizando las modificaciones desde las más recientes hasta las más antiguas.

## [0.3.0] - 2026-11-14

Implementación de la arquitectura Cliente-Servidor sobre UDP. El juego pasa de un prototipo local contra torretas a partidas en red por equipos.

### Agregado

**Servidor (nuevo módulo `server`)**

* `GameServer`: servidor autoritativo con un hilo de recepción y un bucle de juego de paso fijo (60 actualizaciones por segundo) que envía el estado completo a los clientes 30 veces por segundo.
* `ServerLauncher`: programa independiente, ejecutable desde el cmd (`gradlew server:run` o `java -jar`), con comandos de consola `estado`, `retardo`, `perdida`, `normal` y `salir`.
* Simulación de retardo y pérdida de paquetes desde la consola, para probar el comportamiento ante problemas de red.

**Lógica de partida compartida (paquete `game`)**

* `MatchSimulation`: la partida real, ejecutada solo en el servidor. Equipos Azul y Rojo, de 2 a 4 jugadores, eliminaciones, reaparición con protección, puntaje por equipo y temporizador.
* `MatchPhase`: etapas de la partida (sala, cuenta regresiva, juego y resultados).

**Red (paquete `network`)**

* `Protocol`: mensajes de texto, puerto, tiempos de espera y frecuencias.
* `ClientNetwork`: búsqueda del servidor por broadcast, conexión con reintentos, latido cada 250 ms, medición del ping y detección de conexión perdida.
* `WorldSnapshot` y `PlayerInput`: estado de la partida y entradas del jugador, con número de secuencia para descartar paquetes viejos.

**Pantallas**

* `ConnectScreen`: búsqueda del servidor y conexión, con opción de reintentar.
* `LobbyScreen`: sala de espera con equipos y marca de "listo".
* `MessageScreen`: avisos de errores de red con regreso al menú.
* `BaseMenuScreen`: clase base abstracta con el código común de las pantallas de menú.

**Documentación**

* `docs/arquitectura-red.md`: protocolo, hilos, manejo de errores y cumplimiento de los requisitos técnicos.

### Cambiado

* `GameScreen`: ya no simula la partida. Envía las entradas al servidor y dibuja el estado recibido, interpolando entre los dos últimos estados.
* `Player` y `Projectile`: incorporan identificador y equipo; el jugador maneja eliminación, reaparición y estadísticas.
* `Hud`: muestra el puntaje por equipo, el equipo propio, el ping y la cuenta para reaparecer.
* `PauseScreen`: en red la partida no se detiene; ofrece reanudar, ver controles o desconectarse.
* `ResultsScreen`: muestra victoria, derrota o empate por equipo y las estadísticas de cada jugador, y vuelve sola a la sala.
* Las pantallas de menú heredan de `BaseMenuScreen`.
* Al cambiar de pantalla se libera la anterior (`Main.replaceScreen`), lo que evita la acumulación de memoria.
* Comunicación únicamente por UDP; la confiabilidad de los eventos importantes se resuelve con el envío del estado completo, números de secuencia y reintentos.

### Eliminado

* `EnemyDummy`: las torretas del prototipo local se reemplazan por el combate entre jugadores.

### Corregido

* Un disparo con el mouse ubicado exactamente sobre el jugador creaba un proyectil sin dirección que quedaba inmóvil para siempre.

## [0.2.3] - 2026-09-02

* `README.md`: se actualizó el estado del proyecto, se incorporó la lista de controles, se agregó la descripción de la arquitectura del código por paquetes y se enlazó el documento del diagrama de clases.
* `README.md`: se marcaron como completados los puntos del roadmap correspondientes al prototipo local.
### Corregido

* N/A.
## [0.2.2] - 2026-08-31

Incorporación del sonido al prototipo y de la pantalla de consulta de controles.

### Agregado

**Sonido y música**

* `AudioManager`: centraliza la carga y reproducción de la música de fondo en bucle y de los efectos de sonido, evitando que esta responsabilidad quede repartida entre las pantallas.
* Efectos de sonido para el disparo, el impacto, la eliminación de un enemigo y el Impulso Over.
* Recursos de audio temporales (placeholders) generados para el prototipo.
* Control de volumen y silenciado por teclado, mediante `M` y `+` / `-`.
* Barra deslizante de volumen en la pantalla de pausa, que se arrastra con el mouse y ajusta el volumen en tiempo real, junto con un botón de silenciar y activar el sonido.
  **Pantallas**

* `ControlsScreen`: lista completa de controles, accesible tanto desde el menú principal como desde la pantalla de pausa. Al volver, regresa a la pantalla desde la que fue invocada, de modo que consultarla durante una partida no la interrumpe.
### Cambiado

* El menú principal y la pantalla de pausa incorporan la opción "Controles".
* El HUD ahora indica también el estado del sonido y el porcentaje de volumen.
* La música se mantiene sonando durante la pausa, para que el ajuste de la barra de volumen resulte audible mientras se la mueve.
* `ESC` reanuda la partida desde la pausa, y `ENTER` la inicia desde el menú principal.
### Corregido

* N/A.
## [0.2.1] - 2026-08-29

Incorporación de la mecánica de combate, del sistema de puntaje y de la interfaz de juego, sobre la base de movimiento y colisiones de la versión anterior.

### Agregado

**Combate**

* `Projectile`: proyectil con dirección y velocidad propias, que distingue si fue disparado por el jugador o por un enemigo.
* Disparo del jugador en la dirección del mouse, con tiempo de recarga entre disparos.
* `EnemyDummy`: enemigo estático tipo torreta que dispara al jugador por proximidad, recibe daño, es eliminado y reaparece transcurrido un tiempo. Es una pieza temporal, destinada a demostrar la mecánica de combate mientras la sincronización multijugador no está implementada.
* Habilidad especial "Impulso Over" (dash) con tiempo de recarga, correspondiente al personaje Volt de la propuesta.
* Sistema de vida del jugador, con invulnerabilidad breve tras recibir daño y parpadeo del personaje como indicación visual.
  **Colisiones e interacción**

* Detección de colisiones entre proyectil y obstáculo, proyectil y enemigo, y proyectil y jugador, con la aplicación del daño correspondiente.
* Los proyectiles se eliminan al impactar contra un obstáculo o al salir de los límites de la arena.
* Validación de las posiciones de aparición de los enemigos mediante `Arena.isFreeFor()`, para que no aparezcan dentro de un obstáculo.
  **HUD y estado de partida**

* `Hud`: interfaz con cámara y `Viewport` propios, independientes de la cámara del mundo, que permanece fija en pantalla sin importar el desplazamiento de la cámara de juego. Muestra la vida, la disponibilidad de la habilidad especial, el puntaje y el tiempo restante.
* Sistema de puntaje por eliminación de enemigos.
* Temporizador de partida.
  **Pantallas**

* `PauseScreen`: congela la partida conservándola por referencia, sin destruirla ni reiniciarla, y permite reanudarla o volver al menú principal.
* `ResultsScreen`: pantalla de finalización, que muestra el resultado de victoria o derrota junto con el puntaje final.
### Cambiado

* La partida finaliza al agotarse el temporizador (victoria) o al quedar la vida del jugador en cero (derrota), dando paso a la pantalla de resultados.
### Corregido

* N/A.
## [0.2.0] - 2026-08-24

Primera versión ejecutable del prototipo: un personaje que se desplaza con animación por un mapa con obstáculos, seguido por la cámara. Sienta la base estructural sobre la que se apoyan las versiones siguientes.

### Agregado

**Estructura del proyecto**

* Organización del código en paquetes por responsabilidad: `input`, `entities`, `world`, `graphics`, `ui` y `screens`.
* Recursos gráficos temporales (placeholders) generados para el prototipo: spritesheet del personaje, y texturas de piso y de obstáculo.
  **Manejo de entradas**

* `PlayerInputProcessor`, clase dedicada que extiende `InputAdapter` y traduce el teclado y el mouse en un estado consultable por el resto del juego, sin contener lógica de juego.
* Movimiento del jugador con `WASD` en ocho direcciones, con normalización de la velocidad diagonal para que desplazarse en diagonal no resulte más rápido que hacerlo en línea recta.
* Apuntado con el mouse, que determina la dirección del personaje cuando está detenido.
  **Animaciones y spritesheets**

* `DirectionalAnimation`: divide el spritesheet con `TextureRegion.split` y arma un `Animation<TextureRegion>` por cada dirección, administrando el `stateTime` para la animación de caminata.
  **Mapa y entorno**

* `Arena`: entorno de juego generado por código, con paredes perimetrales y bloques de cobertura interiores distribuidos de forma simétrica, que bloquean el movimiento del jugador.
* `Arena.findFreeSpawn()` y `Arena.isFreeFor()`: determinan un punto de aparición libre de obstáculos, buscando en espiral la posición válida más cercana al centro de la arena.
* Resolución del movimiento eje por eje, de modo que el personaje se deslice a lo largo de las paredes en lugar de quedar trabado al chocar en diagonal.
  **Cámara y viewport**

* Cámara ortográfica con `ExtendViewport` que sigue al jugador y se adapta a distintos tamaños de ventana sin deformar la imagen.
* La cámara se limita a los bordes de la arena para no mostrar el exterior del mapa, y se centra en él si la ventana llegara a ser más grande que la propia arena.
  **Pantallas**

* `MenuScreen`: pantalla inicial con las opciones de iniciar la partida y salir.
* `GameScreen`: bucle principal de la partida, encargado de actualizar el estado del juego y de representarlo.
### Cambiado

* `Main` ahora extiende `Game` en lugar de `ApplicationAdapter`, para poder administrar el ciclo de vida de las distintas pantallas mediante `setScreen()`.
* `lwjgl3/build.gradle` pasó de Java 8 a Java 21, para que coincida con la configuración del módulo `core` y del proyecto raíz.
* Se quitó el módulo `html` de `settings.gradle`. GWT no admite Java 21 ni el uso de sockets TCP/UDP, por lo que resulta incompatible con el alcance del proyecto.
### Corregido
## [0.1.0] - 2026-08-21

### Agregado

* Estructura inicial de documentación del proyecto.
* Archivo `README.md`.
* Archivo `CHANGELOG.md`.
* Definición de la descripción general de OverZone.
* Definición del alcance mínimo y alcance deseable.
* Definición de la arquitectura Cliente-Servidor.
* Definición del modelo de servidor autoritativo.
* Definición del uso de los protocolos TCP y UDP.
* Definición de los controles principales.
* Definición de los personajes iniciales Volt y Brutus.
* Definición de las principales mecánicas de combate.

### Agregado

* Creación de la propuesta inicial de **OverZone**.
* Definición de OverZone como videojuego multijugador competitivo 2D Top-Down.
* Definición del modo de juego basado en eliminaciones por equipos.
* Definición del objetivo de partidas de 2 a 4 jugadores.
* Definición de partidas dentro de una red local LAN.
* Definición del mapa inicial y las mecánicas principales.
* Definición de dos personajes para el alcance mínimo.
* Definición de las futuras funcionalidades correspondientes al alcance deseable.
* Definición de las tecnologías principales: Java SE 21, LibGDX 1.13.5, Gradle, Git y GitHub.
* Definición de herramientas complementarias: Aseprite/LibreSprite, Tiled Map Editor, Audacity y Draw.io.
* Definición de la ambientación futurista basada en el Núcleo Over.
* Definición de las facciones CyberCore, Titan Force, Shadow Syndicate y Aegis Protocol.
* Definición de las etapas de desarrollo del proyecto.

## Formato de versiones

Las versiones seguirán, cuando corresponda, el siguiente criterio:

* **MAJOR:** cambios importantes que modifiquen significativamente el proyecto.
* **MINOR:** incorporación de nuevas funcionalidades.
* **PATCH:** correcciones de errores, ajustes y mejoras menores.

---

> El changelog será actualizado a medida que avance el desarrollo del proyecto.
