# Changelog

Todos los cambios importantes realizados en **OverZone** serán documentados en este archivo.

El formato utilizado está basado en el estándar **Keep a Changelog**, organizando las modificaciones desde las más recientes hasta las más antiguas.

## [0.7.1] - 2026-11-24

Power-Ups y arreglo de la elección del modo de juego. **Cambia el protocolo (versión 4): hay que actualizar también el servidor.**

### Agregado

* **Power-Ups temporales** (`PowerUpType`): Botiquín (+50 de vida), Turbo (+35 % de velocidad, 6 s), Sobrecarga (+50 % de daño, 6 s) y Blindaje (mitad de daño recibido, 6 s). Aparecen en cinco puntos del mapa (`Arena.POWERUP_SPAWNS`): el primero a los 5 s y después cada 8 s, hasta 3 a la vez.
* Íconos de los Power-Ups y base en el piso que late (`tools/generar_powerups.py`), aura del color del efecto en los jugadores que tienen uno, texto "¡TURBO!" (o el que corresponda) sobre quien lo agarra y sonido `powerup.wav`.
* HUD: debajo del panel del personaje se ven los Power-Ups activos con los segundos que les quedan; titilan cuando están por terminar.
* El estado (`STATE`) agrega la lista de Power-Ups del mapa y, por jugador, el tiempo restante de cada efecto.

### Corregido

* **Contador de eliminaciones en Todos contra todos:** el recuadro derecho del HUD mostraba al jugador que iba primero, y si el primero eras vos, los dos recuadros mostraban tus propias eliminaciones (con 2 jugadores, cada eliminación parecía sumar en los dos lados). Ahora el izquierdo muestra las tuyas y el derecho las de tu **mejor rival**, y debajo se ve tu puesto (por ejemplo, "VOS 1°/2 | RIVAL: FACU").
* Resultados de Todos contra todos: los jugadores empatados comparten el puesto en el ranking (antes salían como #1 y #2 aunque el título dijera "EMPATE").
* El sonido de eliminación se calcula por jugador: antes, si alguien se iba justo cuando otro eliminaba, no sonaba.
* Cambiar el modo a veces no reaccionaba. Antes el cliente mandaba en cada `PING` "el modo que quiero" y el servidor solo reaccionaba cuando ese valor cambiaba, así que un pedido rechazado podía dejar el botón trabado hasta elegir el otro modo y volver. Ahora cada pedido es **numerado** (como el contador de la habilidad): el servidor atiende cada número una sola vez, sobrevive a la pérdida de paquetes y cada clic es un pedido nuevo.
* El pedido se envía en el momento del clic, sin esperar al próximo `PING`.

### Cambiado

* La sala de espera explica por qué no se puede cambiar el modo: "Solo el anfitrión (nombre) puede cambiar el modo" o "Equipos 2v2 necesita exactamente 4 jugadores (hay 3)".
* El botón del modo pedido titila mientras espera la confirmación del servidor, y aparece "Modo: ..." cuando se aplica. Si no se aplica en 1,5 s, se avisa.
* El anfitrión ve "SOS EL ANFITRIÓN" en el panel del modo, y los demás jugadores reciben un aviso cuando el modo cambia.
* La tecla `M` alterna a partir del modo pedido, así apretarla dos veces seguidas funciona bien.

## [0.7.0] - 2026-11-22

Hasta 4 jugadores, dos modos de juego y los personajes del alcance deseable. **Cambia el protocolo (versión 3): hay que actualizar también el servidor.**

### Agregado

* **Modos de juego:** Todos contra todos (2 a 4 jugadores, gana quien más elimina) y Equipos 2v2 (solo con exactamente 4 jugadores). `GameMode` en el paquete compartido.
* **Anfitrión de la sala:** el primer jugador conectado elige el modo (botones o tecla `M`). El servidor valida el pedido: rechaza Equipos si no hay 4 jugadores y vuelve a Todos contra todos si alguien se va.
* **Iris** (Aegis Protocol, Soporte): pulsos de energía de poco daño y el **Campo de Regeneración**, una zona que cura a los aliados durante 5 s.
* **Phantom** (Shadow Syndicate, Asesino): muy rápido, disparos veloces de mucho daño y corto alcance, y el **Camuflaje Óptico**, que lo vuelve casi invisible para los rivales durante 4 s.
* Alcance máximo de los disparos según el personaje.
* Sprites de Iris y Phantom, proyectiles propios, textura del Campo de Regeneración, botones de súper con cruz y ojo tachado, y sonidos `heal.wav` y `camo.wav` (`tools/generar_sonidos_v07.py`).
* Colores propios de cada jugador en Todos contra todos (azul, rojo, verde y violeta).

### Cambiado

* Sala de espera rediseñada: lista de hasta 4 jugadores con el anfitrión marcado, panel de modo de juego y cuatro tarjetas de personaje (teclas `1` a `4`).
* HUD: en Todos contra todos muestra tus eliminaciones y las del jugador que va primero.
* Resultados: en Todos contra todos, ranking por eliminaciones y "¡GANASTE!" o "GANÓ (nombre)".
* El autoapuntado ignora a los aliados y a los Phantom camuflados.
* Volt y Brutus ajustados a la propuesta: recarga de 10 s y 15 s, Volt con armadura azul, Brutus con detalles rojos, y Brutus camina más lento con el escudo activo.
* El estado (`STATE`) agrega el modo, el anfitrión y los campos de regeneración; el `PING` agrega el modo pedido.

## [0.6.0] - 2026-11-22

Rediseño de la interfaz: HUD con botones de ataque y súper, y menús con estilo futurista. Solo cambia el cliente; el servidor y el protocolo no se modifican.

### Agregado

* **Botón de ataque** (rojo, con mira de francotirador) y **botón de súper** (amarillo) en el HUD, que funcionan como joysticks: se arrastran para apuntar y al soltarlos se dispara o se usa la habilidad. Si solo se tocan, el disparo apunta al enemigo más cercano.
* Línea de mira en el mapa mientras se apunta con el botón rojo, flecha del Impulso Over y cono del Escudo Cinético mientras se apunta con el amarillo.
* El botón de súper muestra un anillo de carga y brilla cuando la habilidad está lista.
* `UiKit`: fuentes y texturas de la interfaz compartidas por todas las pantallas. Fuentes Russo One y Chakra Petch (licencia OFL), generadas con FreeType; ahora se pueden usar tildes y eñes en los textos.
* `MenuBackground`: fondo animado de los menús, con grilla en perspectiva, chispas, líneas de velocidad y explosiones de energía.
* `UiDraw`: paneles con esquinas biseladas y brillos de neón.
* Logo de OverZone e imágenes de los botones, generados con `tools/generar_ui.py`.
* Menú principal con logo, los dos personajes y navegación con flechas y ENTER.

### Cambiado

* HUD rediseñado: panel del personaje con barra de vida segmentada y recarga de la habilidad, reloj con puntaje de cada equipo y aviso de "ELIMINADO". El HUD se adapta a la forma de la ventana y queda pegado a sus bordes.
* Todas las pantallas de menú usan el nuevo estilo: botones de neón con animación al pasar el mouse, paneles translúcidos y títulos con efecto de color.

## [0.5.0] - 2026-11-19

Segundo personaje jugable y nuevos recursos gráficos.

### Agregado

* **Brutus** (Titan Force, Tanque): 180 de vida, velocidad 100, 40 de daño por disparo y menor cadencia.
* **Escudo Cinético**: escudo frontal que durante 3 segundos absorbe el 70 % del daño que llega de frente; sigue la dirección del mouse y se recarga en 8 segundos.
* `CharacterType` (paquete `shared`): estadísticas y habilidad de cada personaje en un solo lugar.
* Elección de personaje en la sala de espera, con retrato y estadísticas de cada uno (teclas `1` y `2` o con el mouse).
* Sprites animados de Volt y Brutus, con caminata en cuatro direcciones.
* Texturas nuevas del mapa: piso metálico, paredes perimetrales, bloques de cobertura con Núcleo Over, marca central y zonas de aparición de cada equipo.
* Proyectiles distintos para cada personaje, efecto visual del escudo, anillo del color del equipo bajo cada jugador y sonido del escudo.
* Scripts `tools/generar_sprites.py` y `tools/generar_sonido_escudo.py`, que generan esos recursos.

### Cambiado

* Protocolo versión 2: el `PING` incluye el personaje elegido y el estado de cada jugador incluye su personaje, si tiene la habilidad activa y la dirección del escudo.
* El HUD muestra el personaje, su vida máxima y el nombre de su habilidad. Los resultados muestran el personaje de cada jugador.

### Eliminado

* Imágenes del prototipo que ya no se usan (`player_walk.png`, `enemy_dummy.png`, `obstacle.png`, `projectile_player.png`, `projectile_enemy.png`).

## [0.4.0] - 2026-11-19

Separación del cliente y el servidor en dos repositorios.

### Cambiado

* El servidor pasa a su propio repositorio, `OverZoneScardaoni-Server`, junto con la lógica de la partida (`MatchSimulation`, `Player`, `Projectile`). Este repositorio queda solo con el cliente.
* Nuevo paquete `shared` con las clases que necesitan los dos programas (`Protocol`, `PlayerInput`, `WorldSnapshot`, `MatchPhase`, `Team`, `Direction`, `Arena`, `Obstacle`). Está copiado en los dos repositorios y debe mantenerse idéntico.
* El cliente ya no usa las clases de la lógica de la partida: los tamaños y la vida máxima que necesita para dibujar están en `GameConstants`.

### Agregado

* `Protocol.VERSION`: el cliente envía su versión de protocolo y el servidor rechaza las conexiones con una versión distinta (`REJECT;VERSION`), mostrando un aviso claro en lugar de fallar.

### Eliminado

* Módulo `server` de este repositorio.

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
