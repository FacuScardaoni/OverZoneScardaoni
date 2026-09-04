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
* Combate por equipos basado en eliminaciones.
* Arquitectura Cliente-Servidor.
* Servidor autoritativo.
* Comunicación mediante TCP y UDP.
* Sincronización en tiempo real.
* Un mapa funcional.
* Movimiento en ocho direcciones.
* Perspectiva Top-Down.
* Sistema de colisiones.
* Obstáculos estáticos.
* Sistema de disparos mediante proyectiles.
* Dos personajes jugables con habilidades diferentes.
* Sistema de vida y daño.
* Eliminación y reaparición de jugadores.
* Puntaje por equipo.
* Temporizador de partida.
* HUD.
* Pantalla final de resultados.

El equipo que consiga la mayor cantidad de eliminaciones al finalizar el temporizador será declarado ganador.

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

## Arquitectura de red

OverZone utiliza una arquitectura **Cliente-Servidor con servidor autoritativo**.

Cada jugador ejecuta una instancia del cliente, mientras que el servidor administra el estado global de la partida. Los clientes envían sus acciones al servidor y reciben las actualizaciones correspondientes.

El servidor es responsable de validar los eventos importantes, incluyendo impactos, daño, eliminaciones, reapariciones, puntaje y finalización de la partida.

### TCP

TCP se utilizará para información cuya pérdida pueda provocar inconsistencias, como:

* Conexión inicial.
* Creación y unión a partidas.
* Selección de personaje.
* Inicio de partida.
* Eliminaciones.
* Reapariciones.
* Actualización del puntaje.
* Finalización de la partida.
* Resultados.

### UDP

UDP se utilizará para información frecuente donde sea prioritaria la velocidad de transmisión:

* Posición de jugadores.
* Dirección de movimiento.
* Dirección de apuntado.
* Posición de proyectiles.
* Trayectoria de disparos.
* Estado de movimiento.
* Actualizaciones frecuentes del escenario.

Esta división permite priorizar confiabilidad para los eventos críticos y baja latencia para las actualizaciones constantes del juego.

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

### Ejecutar

El proyecto utiliza Gradle como sistema de compilación. 

```bash

./gradlew lwjgl3:run
```

En Windows:

```bash
gradlew.bat lwjgl3:run
```

Para ejecutar una partida multijugador, será necesario iniciar el servidor y posteriormente conectar las instancias de los clientes dentro de la misma red local.

## Estado actual

**Segunda pre-entrega: prototipo local jugable.**

El proyecto cuenta con una primera versión jugable en un solo cliente (sin red todavía), que implementa:

* Menú principal, partida y pantallas de pausa / resultados (victoria o derrota), con transición entre ellas sin cerrar la aplicación.
* Movimiento en ocho direcciones (WASD) con animación de caminata a partir de un spritesheet.
* Apuntado con mouse, disparo de proyectiles y habilidad especial "Impulso Over" (dash) con cooldown.
* Una arena con obstáculos que bloquean el movimiento y los disparos.
* Cámara que sigue al jugador dentro de los límites de la arena, adaptada a distintos tamaños de ventana.
* Enemigos tipo torreta que disparan, reciben daño, son eliminados y reaparecen, otorgando puntaje.
* HUD fijo con vida, cooldown de habilidad, puntaje, tiempo restante y estado del sonido.
* Música de fondo, efectos de sonido y controles de volumen/silenciado.

La arquitectura Cliente-Servidor (TCP/UDP) y la sincronización multijugador descriptas en la propuesta se implementarán en la siguiente etapa del desarrollo, sobre la base de esta estructura de código.

**Controles del prototipo:** `W A S D` moverse · Mouse apuntar · Click izquierdo disparar · Click derecho o `Espacio` Impulso Over · `ESC` pausa · `M` silenciar · `+`/`-` volumen. La lista completa también puede consultarse dentro del juego, desde el menú principal o desde la pantalla de pausa.

**Video de demostración:** _(agregar aquí el enlace a YouTube/Google Drive con permisos de visualización para los docentes)._

## Arquitectura del código (prototipo)

El código del prototipo está organizado por responsabilidad dentro de `core/src/main/java/com/OverZone/Scardaoni/`:

| Paquete | Contenido |
| --- | --- |
| `input` | `PlayerInputProcessor`: captura teclado y mouse. |
| `entities` | `Player`, `Projectile`, `Obstacle`, `EnemyDummy`. |
| `world` | `Arena`: entorno, límites y obstáculos del mapa. |
| `graphics` | `DirectionalAnimation`: animación desde spritesheet. |
| `audio` | `AudioManager`: música, efectos y volumen. |
| `ui` | `Hud`: interfaz fija en pantalla. |
| `screens` | `MenuScreen`, `ControlsScreen`, `GameScreen`, `PauseScreen`, `ResultsScreen`. |

`Main` extiende `Game` y administra la transición entre pantallas.

El detalle completo (diagrama de clases, diagrama de paquetes, flujo de pantallas y justificación de las decisiones de diseño) está en [`docs/diagrama-clases.md`](docs/diagrama-clases.md).

## Roadmap

* [x] Definición de la propuesta.
* [x] Diseño conceptual del videojuego.
* [x] Configuración inicial del proyecto LibGDX.
* [x] Menú principal.
* [x] Prototipo local.
* [x] Movimiento Top-Down.
* [x] Cámara y colisiones.
* [x] Sistema de disparos.
* [ ] Servidor.
* [ ] Comunicación TCP.
* [ ] Comunicación UDP.
* [ ] Sincronización de jugadores.
* [ ] Sincronización de proyectiles.
* [x] Sistema de combate (versión local, contra enemigos tipo torreta).
* [x] Sistema de eliminaciones y reapariciones (versión local).
* [x] Habilidades especiales (Impulso Over).
* [x] Sistema de puntaje.
* [x] Temporizador.
* [x] HUD.
* [x] Pantalla de resultados.
* [ ] Personajes y recursos gráficos definitivos.
* [ ] Pruebas con múltiples clientes.
* [ ] Pruebas en red LAN.
* [ ] Optimización y corrección de errores.

## Alcance futuro

Una vez completado el alcance mínimo, se podrán incorporar características adicionales como nuevos personajes, mapas, facciones, obstáculos destructibles, Power-Ups, modos de juego alternativos, ranking, estadísticas, música, efectos de sonido, animaciones más elaboradas y partidas a través de Internet.

Estas funcionalidades forman parte del **alcance deseable** y estarán supeditadas al correcto funcionamiento de la arquitectura de red principal.

---

**OverZone — Videojuego multijugador 2D Top-Down de disparos en arena**

**GamePlay del primer prototipo Local jugable de OverZone**
https://drive.google.com/file/d/1tHq9PHhATy7u8D4hXtat0BgTIXoNtNCn/view?usp=drive_link

**Enlace a la Wiki del Proyecto (Propuesta Detallada):**
[Ver la Propuesta Completa del Proyecto aquí](https://github.com/FacuScardaoni/OverZoneScardaoni/wiki/Propuesta-del-Proyecto-‐-%5BOverZone%5D)

**Autor:** Facundo Scardaoni
**Fecha de propuesta:** 01/07/2026
