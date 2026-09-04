# Diagrama de clases y paquetes — OverZone (prototipo)

Este documento describe la organización interna del código del prototipo entregado en la segunda pre-entrega. El objetivo de esta estructura es que cada clase tenga **una única responsabilidad clara**, de modo que la capa de red (Cliente-Servidor, TCP/UDP) pueda integrarse en la siguiente etapa sin reescribir la lógica de juego.

## 1. Vista general por paquetes

```mermaid
flowchart TD
    MAIN["<b>Main</b><br/>extends Game<br/><i>administra las pantallas</i>"]

    subgraph SCREENS["paquete screens — pantallas y estados"]
        MENU["MenuScreen"]
        CONTROLS["ControlsScreen"]
        GAME["GameScreen<br/><i>bucle de juego</i>"]
        PAUSE["PauseScreen"]
        RESULTS["ResultsScreen"]
    end

    subgraph INPUT["paquete input"]
        PIP["PlayerInputProcessor"]
    end

    subgraph ENTITIES["paquete entities"]
        PLAYER["Player"]
        PROJ["Projectile"]
        ENEMY["EnemyDummy"]
        OBS["Obstacle"]
    end

    subgraph WORLD["paquete world"]
        ARENA["Arena"]
    end

    subgraph GRAPHICS["paquete graphics"]
        ANIM["DirectionalAnimation"]
    end

    subgraph UI["paquete ui"]
        HUD["Hud"]
    end

    subgraph AUDIO["paquete audio"]
        AM["AudioManager"]
    end

    MAIN --> SCREENS
    MAIN --> AM

    GAME --> PIP
    GAME --> PLAYER
    GAME --> PROJ
    GAME --> ENEMY
    GAME --> ARENA
    GAME --> ANIM
    GAME --> HUD
    GAME --> AM

    ARENA --> OBS
    HUD -.lee estado.-> PLAYER
```

## 2. Diagrama de clases

```mermaid
classDiagram
    class Main {
        -AudioManager audioManager
        +create()
        +getAudioManager() AudioManager
        +dispose()
    }

    class PlayerInputProcessor {
        -boolean up, down, left, right
        -boolean firePressed
        -float mouseWorldX, mouseWorldY
        +keyDown(int) boolean
        +keyUp(int) boolean
        +touchDown(...) boolean
        +getMoveX() float
        +getMoveY() float
        +isFirePressed() boolean
        +isAbilityJustPressed() boolean
        +isPauseJustPressed() boolean
        +resetTransient()
        +releaseAllKeys()
    }

    class Player {
        +float SIZE, SPEED, MAX_HEALTH
        -float x, y, health
        -int facingDirection
        -float dashTimeRemaining
        -float dashCooldownRemaining
        +update(float)
        +tryStartDash(float, float)
        +isDashing() boolean
        +getCurrentSpeed() float
        +takeDamage(float)
        +getBounds() Rectangle
        +getDashCooldownProgress() float
    }

    class Projectile {
        +float SPEED, SIZE
        -float x, y, dirX, dirY
        -boolean fromPlayer, alive
        +update(float)
        +getBounds() Rectangle
        +kill()
    }

    class EnemyDummy {
        +float MAX_HEALTH, FIRE_RANGE
        +int SCORE_VALUE
        -float x, y, health
        -boolean alive
        +update(float)
        +canFireAt(float, float) boolean
        +takeDamage(float)
        +getBounds() Rectangle
    }

    class Obstacle {
        -Rectangle bounds
        +getBounds() Rectangle
    }

    class Arena {
        +float WIDTH, HEIGHT
        -List~Obstacle~ obstacles
        -buildLayout()
        +collidesWithAny(Rectangle) boolean
        +findCollision(Rectangle) Obstacle
        +getObstacles() List~Obstacle~
    }

    class DirectionalAnimation {
        -Animation~TextureRegion~[] walkByDirection
        -TextureRegion[] idleByDirection
        -float stateTime
        +update(float)
        +getFrame(int, boolean) TextureRegion
        +dispose()
    }

    class AudioManager {
        -Music backgroundMusic
        -Sound shootSound, hitSound
        -float volume
        -boolean muted
        +load()
        +playMusic()
        +playShoot()
        +toggleMute()
        +increaseVolume()
        +decreaseVolume()
    }

    class Hud {
        -OrthographicCamera camera
        -Viewport viewport
        -ShapeRenderer shapes
        +render(SpriteBatch, Player, int, float, boolean)
        +resize(int, int)
    }

    class GameScreen {
        -OrthographicCamera worldCamera
        -Viewport worldViewport
        -Array~Projectile~ projectiles
        -Array~EnemyDummy~ enemies
        -int score
        -float timeRemaining
        +render(float)
        -update(float)
        -moveWithCollision(float, float)
        -resolveCollisions()
        -updateCamera()
        -draw()
    }

    class MenuScreen
    class ControlsScreen
    class PauseScreen
    class ResultsScreen

    Main --|> Game
    PlayerInputProcessor --|> InputAdapter
    GameScreen ..|> Screen
    MenuScreen ..|> Screen
    ControlsScreen ..|> Screen
    PauseScreen ..|> Screen
    ResultsScreen ..|> Screen

    Main o-- AudioManager
    Main --> MenuScreen : pantalla inicial

    MenuScreen --> GameScreen : Jugar
    MenuScreen --> ControlsScreen : Controles
    GameScreen --> PauseScreen : ESC
    PauseScreen --> GameScreen : Reanudar
    PauseScreen --> ControlsScreen : Controles
    PauseScreen --> MenuScreen : Salir
    GameScreen --> ResultsScreen : fin de partida
    ResultsScreen --> MenuScreen : volver

    GameScreen o-- PlayerInputProcessor
    GameScreen o-- Player
    GameScreen o-- Arena
    GameScreen o-- Hud
    GameScreen o-- DirectionalAnimation
    GameScreen "1" *-- "muchos" Projectile
    GameScreen "1" *-- "muchos" EnemyDummy
    GameScreen ..> AudioManager : usa
    Arena "1" *-- "muchos" Obstacle
    Hud ..> Player : lee estado
```

## 3. Flujo de pantallas (máquina de estados)

```mermaid
stateDiagram-v2
    [*] --> Menu
    Menu --> Juego : Jugar / ENTER
    Menu --> Controles : Controles
    Controles --> Menu : Volver
    Juego --> Pausa : ESC
    Pausa --> Juego : Reanudar / ESC
    Pausa --> Controles : Controles
    Controles --> Pausa : Volver
    Pausa --> Menu : Menu principal
    Juego --> Resultados : tiempo agotado (victoria)
    Juego --> Resultados : vida en 0 (derrota)
    Resultados --> Menu : Menu principal
    Menu --> [*] : Salir
```

## 4. Responsabilidad de cada clase

| Paquete | Clase | Responsabilidad | Punto de la consigna |
| --- | --- | --- | --- |
| *(raíz)* | `Main` | Extiende `Game`. Crea el `AudioManager` y administra qué `Screen` está activa. | 5 |
| `input` | `PlayerInputProcessor` | Extiende `InputAdapter`. Traduce teclado y mouse en un estado consultable. No contiene lógica de juego. | 1 |
| `entities` | `Player` | Posición, vida, dirección, invulnerabilidad y habilidad "Impulso Over" con cooldown. | 1, 6 |
| `entities` | `Projectile` | Proyectil con dirección y velocidad; sabe si lo disparó el jugador o un enemigo. | 6 |
| `entities` | `EnemyDummy` | Enemigo tipo torreta: dispara por proximidad, recibe daño, muere y reaparece. | 6 |
| `entities` | `Obstacle` | Área rectangular sólida del entorno. | 3, 6 |
| `world` | `Arena` | Construye el entorno (paredes y cobertura), responde consultas de colisión y valida los puntos de aparición. | 3, 6 |
| `graphics` | `DirectionalAnimation` | Divide el spritesheet con `TextureRegion.split` y arma un `Animation` por dirección, administrando el `stateTime`. | 2 |
| `audio` | `AudioManager` | Carga y reproduce `Music` y `Sound`; controla volumen y silenciado. | 8 |
| `ui` | `Hud` | Interfaz fija con cámara y `Viewport` propios: vida, habilidad, puntaje, tiempo y sonido. | 7 |
| `screens` | `GameScreen` | Bucle de juego: actualiza entidades, resuelve colisiones, mueve la cámara y dibuja. | 1-8 |
| `screens` | `MenuScreen` | Pantalla inicial con las opciones Jugar, Controles y Salir. | 5 |
| `screens` | `ControlsScreen` | Lista de controles; vuelve a la pantalla desde la que fue invocada. | 5 |
| `screens` | `PauseScreen` | Congela la partida sin destruirla; permite reanudar, ver controles o salir. | 5 |
| `screens` | `ResultsScreen` | Muestra victoria o derrota junto al puntaje final. | 5 |

## 5. Decisiones de diseño

- **`Main` extiende `Game` y no `ApplicationAdapter`**, porque `Game` provee el mecanismo de `setScreen()` necesario para cambiar de pantalla sin reiniciar la aplicación.
- **El `AudioManager` vive en `Main`, no en `GameScreen`**, para que la música siga sonando de forma continua al pasar entre pantallas y no se recargue cada vez.
- **`PauseScreen` recibe el `GameScreen` por referencia** en lugar de crear uno nuevo, de modo que la partida (posición, vida, puntaje, tiempo) se conserve intacta al reanudar.
- **El HUD usa un `Viewport` separado** del mundo: si compartiera la cámara del juego, la interfaz se desplazaría junto con el jugador.
- **Las colisiones se resuelven eje por eje** (primero X, después Y). Así, al chocar en diagonal contra una pared, el personaje se desliza sobre ella en vez de quedarse trabado.
- **La velocidad diagonal se normaliza**: sin esa corrección, moverse en diagonal sería aproximadamente un 41% más rápido que hacerlo en línea recta.
- **La arena valida los puntos de aparición** (`findFreeSpawn`) en lugar de confiar en coordenadas escritas a mano. Al haber un obstáculo justo en el centro, el jugador aparecía dentro de él y el sistema de colisiones lo dejaba inmovilizado; la validación hace que ese error sea imposible de repetir al cambiar el layout.
- **La música sigue sonando durante la pausa**, porque es donde está la barra de volumen: sin sonido, el usuario no tendría forma de escuchar el efecto de lo que ajusta.
- **`EnemyDummy` es temporal**: existe para demostrar la mecánica de combate (disparar, impactar, eliminar, puntuar) mientras la sincronización multijugador no está implementada. En la etapa de red, su lugar lo ocuparán los jugadores remotos.
