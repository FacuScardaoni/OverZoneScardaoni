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
* **LibGDX 1.13.5** — Framework para desarrollo de videojuegos 2D.
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
git clone <URL_DEL_REPOSITORIO>
cd OverZone
```

### Importar el proyecto

Abrir el proyecto mediante **IntelliJ IDEA** y permitir que Gradle sincronice las dependencias necesarias.

### Ejecutar

El proyecto utiliza Gradle como sistema de compilación. La ejecución dependerá de la configuración de módulos y tareas generada en el proyecto LibGDX.

En una configuración estándar de LibGDX con módulo de escritorio, puede utilizarse:

```bash
./gradlew lwjgl3:run
```

En Windows:

```bash
gradlew.bat lwjgl3:run
```

> **Nota:** La tarea exacta de ejecución puede variar según la estructura final del proyecto Gradle.

Para ejecutar una partida multijugador, será necesario iniciar el servidor y posteriormente conectar las instancias de los clientes dentro de la misma red local.

## Estado actual

**Configuración inicial y estructura del proyecto.**

El desarrollo se encuentra planteado de manera progresiva, comenzando por la preparación del proyecto y continuando posteriormente con el prototipo local, el desarrollo del servidor, la sincronización en red, las mecánicas de combate, el contenido y finalmente las pruebas y optimización.

## Roadmap

* [x] Definición de la propuesta.
* [x] Diseño conceptual del videojuego.
* [ ] Configuración inicial del proyecto LibGDX.
* [ ] Menú principal.
* [ ] Prototipo local.
* [ ] Movimiento Top-Down.
* [ ] Cámara y colisiones.
* [ ] Sistema de disparos.
* [ ] Servidor.
* [ ] Comunicación TCP.
* [ ] Comunicación UDP.
* [ ] Sincronización de jugadores.
* [ ] Sincronización de proyectiles.
* [ ] Sistema de combate.
* [ ] Sistema de eliminaciones y reapariciones.
* [ ] Habilidades especiales.
* [ ] Sistema de puntaje.
* [ ] Temporizador.
* [ ] HUD.
* [ ] Pantalla de resultados.
* [ ] Personajes y recursos gráficos definitivos.
* [ ] Pruebas con múltiples clientes.
* [ ] Pruebas en red LAN.
* [ ] Optimización y corrección de errores.

## Alcance futuro

Una vez completado el alcance mínimo, se podrán incorporar características adicionales como nuevos personajes, mapas, facciones, obstáculos destructibles, Power-Ups, modos de juego alternativos, ranking, estadísticas, música, efectos de sonido, animaciones más elaboradas y partidas a través de Internet.

Estas funcionalidades forman parte del **alcance deseable** y estarán supeditadas al correcto funcionamiento de la arquitectura de red principal.

---

**OverZone — Videojuego multijugador 2D Top-Down de disparos en arena**

**Autor:** Facundo Scardaoni
**Fecha de propuesta:** 01/07/2026
