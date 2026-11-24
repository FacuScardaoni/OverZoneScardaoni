# OverZone

**Videojuego multijugador 2D Top-Down de disparos en arena**

## Integrante

* **Facundo Scardaoni**

## Descripción

**OverZone** es un videojuego multijugador competitivo en 2D, con perspectiva **Top-Down**, ambientado en un universo de ciencia ficción futurista.

Dos equipos se enfrentan dentro de arenas cerradas utilizando armas de proyectiles, habilidades especiales y estrategias de posicionamiento. El objetivo principal es conseguir la mayor cantidad de eliminaciones antes de que finalice el tiempo de la partida.

El proyecto está especialmente orientado a la implementación de una arquitectura **Cliente-Servidor**, donde un servidor autoritativo administra el estado de la partida y sincroniza a todos los jugadores en tiempo real.

La versión mínima está diseñada para partidas de **2 a 4 jugadores dentro de una red local (LAN)**.

## Historia

La historia de OverZone transcurre hacia finales del siglo XXII, cuando la humanidad descubre una poderosa fuente de energía denominada **Núcleo Over**.

Su enorme capacidad energética provoca una disputa entre distintas organizaciones que buscan controlar sus yacimientos. Para evitar una guerra global, los conflictos comienzan a resolverse mediante enfrentamientos organizados dentro de instalaciones construidas alrededor de estos recursos, conocidas como **OverZones**.

El jugador asumirá el papel de uno de los combatientes enviados por estas organizaciones para disputar el control de los Núcleos Over.

## Características principales

La versión mínima del proyecto contempla:

* Partidas de 2 a 4 jugadores.
* Dos modos de juego: **Todos contra todos** y **Equipos 2v2**, basados en eliminaciones.
* Arquitectura Cliente-Servidor.
* Servidor autoritativo.
* Comunicación mediante UDP.
* Sincronización en tiempo real.
* Un mapa funcional.
* Movimiento en ocho direcciones.
* Perspectiva Top-Down.
* Sistema de colisiones.
* Obstáculos estáticos.
* Sistema de disparos mediante proyectiles.
* Cuatro personajes jugables con habilidades diferentes (Volt y Brutus del alcance mínimo, Iris y Phantom del alcance deseable).
* Sistema de vida y daño.
* Eliminación y reaparición de jugadores.
* Puntaje por equipo.
* Temporizador de partida.
* HUD.
* Pantalla final de resultados.

Al finalizar el temporizador gana quien consiga más eliminaciones: el jugador, en Todos contra todos, o el equipo, en Equipos 2v2.

### Modos de juego

| | Todos contra todos | Equipos 2v2 |
| --- | --- | --- |
| Jugadores | 2 a 4 | exactamente 4 |
| Quién gana | el jugador con más eliminaciones | el equipo con más eliminaciones |
| Colores | cada jugador tiene el suyo (azul, rojo, verde, violeta) | equipo Azul y equipo Rojo |
| Aparición | en las esquinas; al reaparecer, en la más alejada de los rivales | en la base de cada equipo |

El modo lo elige el **anfitrión** de la sala (el primer jugador que se conectó, marcado con `*`). Si no hay 4 jugadores en la sala, el servidor no permite elegir Equipos; y si alguien se va de la sala estando en Equipos, el modo vuelve a Todos contra todos.

## Personajes

### Volt

**Facción:** CyberCore
**Rol:** Asalto

Combatiente especializado en velocidad y movilidad. Cuenta con una alta velocidad de movimiento y cadencia de disparo.

**Habilidad especial — Impulso Over**

Realiza una aceleración instantánea en la dirección seleccionada, permitiendo escapar de situaciones peligrosas o acercarse rápidamente a un enemigo.

### Brutus

**Facción:** Titan Force
**Rol:** Tanque

Combatiente pesado especializado en resistencia y daño. Posee una gran cantidad de vida, aunque su velocidad de movimiento y cadencia de disparo son reducidas.

**Habilidad especial — Escudo Cinético**

Activa un escudo frontal capaz de absorber parte del daño recibido durante algunos segundos.

### Iris

**Facción:** Aegis Protocol
**Rol:** Soporte

Especialista en apoyo. Dispara pequeños pulsos de energía de daño reducido y mantiene a su equipo en combate.

**Habilidad especial — Campo de Regeneración**

Crea una zona circular donde los aliados recuperan vida durante algunos segundos. El servidor crea la zona, calcula quién está adentro y aplica la curación; los clientes solo la dibujan. En Todos contra todos, el campo cura únicamente a Iris.

### Phantom

**Facción:** Shadow Syndicate
**Rol:** Asesino

Combatiente de emboscada: muy rápido y con disparos veloces de mucho daño, pero de corto alcance y poca vida.

**Habilidad especial — Camuflaje Óptico**

Se vuelve casi invisible durante unos segundos. El servidor marca a Phantom como camuflado en el estado y todos los clientes muestran el mismo efecto: los rivales lo ven casi transparente y sin nombre, y su equipo lo ve translúcido. Disparar corta el camuflaje.

### Comparación

| | Volt | Brutus | Iris | Phantom |
| --- | --- | --- | --- | --- |
| Facción | CyberCore | Titan Force | Aegis Protocol | Shadow Syndicate |
| Rol | Asalto | Tanque | Soporte | Asesino |
| Vida | 100 | 180 | 110 | 90 |
| Velocidad | 140 | 100 | 125 | 170 |
| Daño por disparo | 25 | 40 | 15 | 38 |
| Tiempo entre disparos | 0,22 s | 0,50 s | 0,28 s | 0,40 s |
| Alcance | medio (330) | medio (330) | medio (300) | corto (170) |
| Habilidad | Impulso Over: dash de 0,18 s a más del triple de velocidad | Escudo Cinético: durante 4 s absorbe el 70 % del daño que llega de frente (camina al 75 % de velocidad) | Campo de Regeneración: zona de 5 s que cura 14 de vida por segundo | Camuflaje Óptico: 4 s casi invisible, se corta al disparar |
| Recarga de la habilidad | 10 s | 15 s | 20 s | 18 s |

Las recargas y los colores siguen la propuesta: Volt con armadura azul (CyberCore), Brutus gris con rojo (Titan Force), Iris blanca con verde y cian (Aegis Protocol) y Phantom negro con rojo y casco cerrado (Shadow Syndicate).

Cada jugador elige su personaje en la sala de espera (teclas `1` a `4` o con el mouse). Las estadísticas están definidas en `CharacterType`, dentro del paquete compartido `shared`.

## Arquitectura de red

OverZone utiliza una arquitectura **Cliente-Servidor con servidor autoritativo**, con toda la comunicación sobre **UDP**.

El servidor es un programa aparte, en su propio repositorio ([**OverZoneScardaoni-Server**](https://github.com/FacuScardaoni/OverZoneScardaoni-Server)), sin ventana, que puede ejecutarse en otra computadora de la red local. Este repositorio contiene solamente el **cliente**: el código del cliente no incluye la lógica de la partida. Es el único que ejecuta la lógica de la partida: movimiento, colisiones, disparos, daño, eliminaciones, reapariciones, puntaje, temporizador y finalización.

Los clientes solo hacen dos cosas:

* **Envían sus entradas** (teclas de movimiento, posición del mouse, disparo y habilidad), nunca su posición.
* **Dibujan el estado** que reciben del servidor.

### Por qué solo UDP

La propuesta inicial contemplaba TCP para los eventos críticos y UDP para las actualizaciones frecuentes. En la implementación se utiliza únicamente UDP, siguiendo lo trabajado en la materia, y la confiabilidad de los eventos críticos se resuelve con el diseño de los mensajes:

* **El servidor envía el estado completo, no los cambios.** 30 veces por segundo manda una "foto" de toda la partida (fase, modo, tiempo, puntaje, jugadores, proyectiles y campos de regeneración). Si un paquete se pierde, el siguiente lo reemplaza por completo, por lo que una eliminación, un cambio de puntaje o el fin de la partida nunca se pierden.
* **Números de secuencia.** Cada estado y cada entrada llevan un número; los paquetes que llegan tarde o desordenados se descartan.
* **La habilidad se envía como contador.** El cliente manda cuántas veces pidió la habilidad en total, no un "se apretó ahora": si un paquete se pierde, el siguiente igual informa el pedido.
* **Reintentos y latido.** La búsqueda y la conexión se reintentan; durante la partida, el cliente avisa cada 250 ms que sigue conectado. Si alguno de los dos lados deja de recibir datos durante 3 segundos, se considera la conexión perdida.

El detalle del protocolo, los hilos y el manejo de errores está en [`docs/arquitectura-red.md`](docs/arquitectura-red.md).

## Controles

| Acción              | Control         |
| ------------------- | --------------- |
| Movimiento          | `W A S D`       |
| Apuntar             | Mouse           |
| Disparo básico      | Click izquierdo |
| Habilidad especial  | Click derecho   |
| Pausa               | `ESC`           |
| Selección en menús  | Mouse           |
| Confirmar selección | Click izquierdo |

## Tecnologías

El proyecto utiliza las siguientes tecnologías y herramientas:

* **Java SE 21 (LTS)** — Lenguaje y plataforma principal.
* **LibGDX 1.14.2** — Framework para desarrollo de videojuegos 2D.
* **Gradle** — Sistema de compilación y gestión de dependencias.
* **IntelliJ IDEA Community Edition 2025.x** — Entorno de desarrollo.
* **Git** — Control de versiones.
* **GitHub** — Repositorio remoto.
* **Aseprite / LibreSprite** — Creación de recursos Pixel Art.
* **Tiled Map Editor** — Diseño de mapas.
* **Audacity** — Edición de efectos de sonido.
* **Draw.io** — Diagramas y documentación.

Estas herramientas forman parte de la planificación tecnológica definida en la propuesta.

## Plataforma objetivo

La versión mínima está orientada a **computadoras de escritorio conectadas mediante una red local (LAN)**.

El proyecto contempla inicialmente pruebas utilizando múltiples clientes en una misma computadora y posteriormente mediante varias computadoras conectadas a una misma red LAN.

La conexión a través de Internet queda planteada como una posible mejora futura y no forma parte del alcance mínimo.

## Cómo compilar y ejecutar

### Requisitos

Antes de ejecutar el proyecto se recomienda disponer de:

* Java JDK 21.
* Git.
* IntelliJ IDEA Community Edition.
* Una computadora de escritorio compatible con la ejecución de aplicaciones Java/LibGDX.

### Clonar el repositorio

```bash
git clone https://github.com/FacuScardaoni/OverZoneScardaoni.git
cd OverZoneScardaoni
```

### Importar el proyecto

Abrir el proyecto mediante **IntelliJ IDEA** y permitir que Gradle sincronice las dependencias necesarias.

### Ejecutar en red

Se ejecutan dos programas: el **servidor** (una sola vez, en cualquier computadora de la red) y un **cliente** por jugador. No hace falta escribir ninguna IP: el cliente encuentra el servidor solo, mediante broadcast.

Cada programa está en su propio repositorio:

| Programa | Repositorio |
| --- | --- |
| Cliente (el juego) | Este repositorio, `OverZoneScardaoni` |
| Servidor | [`OverZoneScardaoni-Server`](https://github.com/FacuScardaoni/OverZoneScardaoni-Server) |

**1. Abrir el servidor.** Seguir las instrucciones del README del repositorio del servidor. En resumen, desde su carpeta:

```bash
gradlew.bat jar
java -jar build\libs\OverZone-Server-1.0.0.jar
```

**2. Generar el cliente** (desde la carpeta de este repositorio, en el cmd):

```bash
gradlew.bat lwjgl3:jar
```

**3. Abrir un cliente por jugador** (en la misma computadora o en otras de la red):

```bash
java -jar lwjgl3\build\libs\OverZone-1.0.0.jar
```

En el menú elegir **Jugar en red**. En la sala de espera, cada jugador marca **Listo**; con al menos dos jugadores listos empieza la partida.

También se puede abrir el cliente sin generar el `.jar`, con `gradlew.bat lwjgl3:run` (en Linux o macOS, `./gradlew` en lugar de `gradlew.bat`).

Los dos programas requieren **Java 21**.

**Requisitos de la red**

* Todas las computadoras deben estar en la **misma subred** (por ejemplo, `192.168.0.x`): el broadcast no atraviesa routers. Conviene que tengan la IP en automático (DHCP).
* La primera vez que se abre el servidor o el cliente, Windows puede pedir permiso en el firewall: hay que permitir Java en **redes privadas**. Si el cliente no encuentra el servidor, es lo primero que conviene revisar.
* El servidor usa el puerto UDP **5555**. Si ya hay un servidor abierto en la misma computadora, el segundo avisa que el puerto está ocupado y se cierra.

## Estado actual

**Tercera etapa: juego en red (Cliente-Servidor sobre UDP).**

* Servidor autoritativo independiente, con bucle de juego de paso fijo (60 actualizaciones por segundo) y envío del estado a los clientes 30 veces por segundo.
* Descubrimiento del servidor por broadcast en la red local, sin IP escrita en el código.
* Partidas de 2 a 4 jugadores en dos modos: Todos contra todos y Equipos 2v2 (solo con 4 jugadores), elegidos por el anfitrión de la sala. Eliminaciones, reaparición, puntaje y temporizador de 2 minutos.
* Sala de espera con marca de "listo", cuenta regresiva y pantalla de resultados. Al terminar, todos vuelven a la sala y pueden jugar otra partida sin reiniciar los programas.
* Interpolación del movimiento en el cliente, descarte de paquetes viejos y medición del retardo (ping) visible en el HUD.
* Manejo de errores de red: puerto ocupado, servidor no encontrado, partida llena, desconexión de un cliente (avisada o por tiempo de espera), cierre o caída del servidor, y cierre ordenado de sockets e hilos.
* Simulación de retardo y pérdida de paquetes desde la consola del servidor.

* Cuatro personajes jugables, Volt, Brutus, Iris y Phantom, con estadísticas, alcance y habilidades distintas, elegibles en la sala de espera.
* Sprites animados de los cuatro personajes (caminata en cuatro direcciones), proyectiles, Escudo Cinético, Campo de Regeneración y mapa con texturas de estilo ciencia ficción. Los jugadores se distinguen por el anillo del color de su equipo (o el suyo propio en Todos contra todos).

Los recursos gráficos se generan con los scripts `tools/generar_sprites.py` (personajes y mapa) y `tools/generar_ui.py` (logo y botones del HUD), y los sonidos de las habilidades con `tools/generar_sonido_escudo.py` y `tools/generar_sonidos_v07.py`, así que se pueden modificar cambiando colores o medidas y volviendo a ejecutarlo.

**Controles:** `W A S D` moverse · Mouse apuntar · Click izquierdo disparar · Click derecho o `Espacio` habilidad · `1` a `4` elegir personaje en la sala · `M` cambiar el modo en la sala (solo el anfitrión).

También se puede jugar con los **botones del HUD**, abajo a la derecha: el botón rojo (ataque) y el amarillo (súper) se arrastran para apuntar y al soltarlos disparan o activan la habilidad; si solo se tocan, el disparo apunta al enemigo más cercano · `ESC` menú de pausa · `M` silenciar · `+`/`-` volumen. En red, la pausa no detiene la partida: el personaje se queda quieto mientras el menú está abierto.

**Video de demostración:** _(agregar aquí el enlace a YouTube/Google Drive con permisos de visualización para los docentes)._

## Arquitectura del código

El juego está dividido en **dos repositorios**:

* **Cliente** (este repositorio): pantallas, gráficos, sonido, entrada y la parte de red del cliente.
* **Servidor** ([`OverZoneScardaoni-Server`](https://github.com/FacuScardaoni/OverZoneScardaoni-Server)): la partida real (`MatchSimulation`, `Player`, `Projectile`) y la red del servidor (`GameServer`).

Los dos comparten el paquete **`shared`**, con lo que ambos necesitan conocer igual: el protocolo de mensajes, el mapa y algunas constantes. Ese paquete está copiado en los dos repositorios y tiene que ser idéntico en ambos. `Protocol.VERSION` indica la versión del protocolo: si un cliente y un servidor de versiones distintas intentan conectarse, la conexión se rechaza con un aviso claro, en lugar de fallar de forma confusa.

Este repositorio tiene dos módulos: `core` (el juego) y `lwjgl3` (el lanzador para computadoras de escritorio). Paquetes de `core/src/main/java/com/OverZone/Scardaoni/`:

| Paquete | Contenido |
| --- | --- |
| `shared` | **Compartido con el servidor:** `Protocol`, `PlayerInput`, `WorldSnapshot`, `MatchPhase`, `GameMode`, `Team`, `Direction`, `CharacterType`, `GameConstants`, `Arena`, `Obstacle`. |
| `network` | `ClientNetwork`: búsqueda del servidor, conexión y recepción del estado. |
| `input` | `PlayerInputProcessor`: captura teclado y mouse. |
| `graphics` | `DirectionalAnimation`: animación desde spritesheet. |
| `audio` | `AudioManager`: música, efectos y volumen. |
| `ui` | `Hud` (interfaz fija en pantalla), `HudControls` (botones de ataque y súper), `UiKit` (fuentes e imágenes compartidas), `UiDraw` y `MenuBackground` (estilo visual de los menús). |
| `screens` | `BaseMenuScreen` (base abstracta), `MenuScreen`, `ConnectScreen`, `LobbyScreen`, `GameScreen`, `PauseScreen`, `ControlsScreen`, `ResultsScreen`, `MessageScreen`. |

El detalle de la red está en [`docs/arquitectura-red.md`](docs/arquitectura-red.md). El documento [`docs/diagrama-clases.md`](docs/diagrama-clases.md) describe el prototipo local de la etapa anterior.

## Roadmap

* [x] Definición de la propuesta.
* [x] Diseño conceptual del videojuego.
* [x] Configuración inicial del proyecto LibGDX.
* [x] Menú principal.
* [x] Prototipo local.
* [x] Movimiento Top-Down.
* [x] Cámara y colisiones.
* [x] Sistema de disparos.
* [x] Servidor (en un repositorio propio).
* [x] Comunicación UDP (con descubrimiento por broadcast).
* [x] Sincronización de jugadores.
* [x] Sincronización de proyectiles.
* [x] Manejo de errores y desconexiones.
* [x] Reinicio de partidas sin cerrar los programas.
* [x] Sistema de combate por equipos.
* [x] Sistema de eliminaciones y reapariciones.
* [x] Habilidades especiales (Impulso Over).
* [x] Sistema de puntaje.
* [x] Temporizador.
* [x] HUD.
* [x] Pantalla de resultados.
* [x] Segundo personaje (Brutus).
* [x] Personajes del alcance deseable (Iris y Phantom).
* [x] Modos Todos contra todos y Equipos 2v2.
* [x] Sprites y animaciones de los personajes y texturas del mapa.
* [ ] Modo de juego local.
* [x] Pruebas con múltiples clientes (4 clientes simulados).
* [ ] Pruebas en red LAN.
* [ ] Optimización y corrección de errores.

## Alcance futuro

Una vez completado el alcance mínimo, se podrán incorporar características adicionales como nuevos mapas, obstáculos destructibles, Power-Ups, modos de juego alternativos, ranking, estadísticas, música, efectos de sonido, animaciones más elaboradas y partidas a través de Internet.

Estas funcionalidades forman parte del **alcance deseable** y estarán supeditadas al correcto funcionamiento de la arquitectura de red principal.

---

**OverZone — Videojuego multijugador 2D Top-Down de disparos en arena**

**GamePlay del primer prototipo Local jugable de OverZone**
https://drive.google.com/file/d/1tHq9PHhATy7u8D4hXtat0BgTIXoNtNCn/view?usp=drive_link

**Enlace a la Wiki del Proyecto (Propuesta Detallada):**
[Ver la Propuesta Completa del Proyecto aquí](https://github.com/FacuScardaoni/OverZoneScardaoni/wiki/Propuesta-del-Proyecto-‐-%5BOverZone%5D)

**Autor:** Facundo Scardaoni
**Fecha de propuesta:** 01/07/2026
