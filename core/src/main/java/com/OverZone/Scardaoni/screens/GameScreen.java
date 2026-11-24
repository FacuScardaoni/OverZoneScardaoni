package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.audio.AudioManager;
import com.OverZone.Scardaoni.shared.MatchPhase;
import com.OverZone.Scardaoni.shared.Team;
import com.OverZone.Scardaoni.graphics.DirectionalAnimation;
import com.OverZone.Scardaoni.input.PlayerInputProcessor;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.shared.WorldSnapshot;
import com.OverZone.Scardaoni.ui.Hud;
import com.OverZone.Scardaoni.ui.UiKit;
import com.OverZone.Scardaoni.ui.HudControls;
import com.OverZone.Scardaoni.shared.Arena;
import com.OverZone.Scardaoni.shared.CharacterType;
import com.OverZone.Scardaoni.shared.Direction;
import com.OverZone.Scardaoni.shared.GameConstants;
import com.OverZone.Scardaoni.shared.GameMode;
import com.OverZone.Scardaoni.shared.Obstacle;

import com.OverZone.Scardaoni.shared.PowerUpType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pantalla de juego del CLIENTE.
 *
 * Ya no simula la partida: eso lo hace el servidor. En cada frame:
 *  1. Lee teclado y mouse y le ENVÍA esas entradas al servidor.
 *  2. Toma los dos últimos estados que mandó el servidor.
 *  3. DIBUJA a cada jugador y proyectil en un punto intermedio entre esos
 *     dos estados (interpolación), para que el movimiento se vea suave
 *     aunque los estados lleguen 30 veces por segundo y con algo de retardo.
 *
 * Los sonidos también se deducen del estado: si aparece un proyectil nuevo,
 * suena un disparo; si baja mi vida, suena un impacto, etc.
 */
public class GameScreen implements Screen {

    /** Si un jugador se movió más que esto entre dos estados, reapareció: no se interpola. */
    private static final float TELEPORT_DISTANCE = 80f;

    /** Segundos por cuadro de caminata de cada personaje (en el orden de CharacterType). */
    private static final float[] WALK_FRAME_TIME = {0.10f, 0.16f, 0.12f, 0.09f};
    /** Opacidad de Phantom camuflado: casi invisible para los rivales, translúcido para su equipo. */
    private static final float CAMO_ALPHA_ENEMY = 0.10f;
    private static final float CAMO_ALPHA_ALLY = 0.45f;
    /** Tono que toma Volt mientras usa el Impulso Over. */
    private static final Color DASH_TINT = new Color(0.55f, 1f, 1f, 0.85f);

    /** Medidas de las zonas de aparición pintadas en el piso (coinciden con el servidor). */
    private static final float SPAWN_ZONE_W = 72f, SPAWN_ZONE_H = 168f, SPAWN_MARGIN_X = 90f;

    private final Main game;
    private final AudioManager audio;

    private final OrthographicCamera worldCamera;
    private final Viewport worldViewport;
    private final SpriteBatch batch;
    private final Hud hud;
    private final PlayerInputProcessor input;
    /** El mapa es fijo e igual en el servidor y en el cliente: se usa solo para dibujarlo. */
    private final Arena arena;
    private final BitmapFont labelFont;
    /** Botones de ataque y súper del HUD (joysticks). */
    private final HudControls hudControls;
    /** Primero los botones del HUD; si el click no es sobre un botón, le llega al teclado/mouse normal. */
    private final InputMultiplexer inputs;
    /** Para dibujar la línea de mira en el mapa. */
    private final ShapeRenderer worldShapes = new ShapeRenderer();
    private float lastDelta = 0f;

    /** Distancia del punto de mira que se envía cuando se apunta con los botones. */
    private static final float AIM_DISTANCE = 150f;
    /** Hasta qué distancia busca enemigos el autoapuntado. */
    private static final float AUTO_AIM_RANGE = 280f;
    private final GlyphLayout layout = new GlyphLayout();

    // --- Recursos gráficos ---
    /** Una animación por personaje, ordenadas como CharacterType (Volt, Brutus, Iris, Phantom). */
    private DirectionalAnimation[] animations;
    private Texture[] projectileTextures;
    private Texture floorTexture, wallTexture, wallVerticalTexture, coverTexture;
    private Texture coreDecal, spawnBlue, spawnRed, teamRing, shieldTexture, regenZoneTexture;
    private TextureRegion floorRegion, shieldRegion;

    private float animationTimer = 0f;

    private Texture powerUpPad;
    /** Power-Ups del estado anterior, para darse cuenta cuándo alguien agarra uno. */
    private final Map<Integer, WorldSnapshot.PowerUpState> lastPowerUps = new HashMap<>();
    /** Textos que suben y se desvanecen sobre quien agarró un Power-Up ("¡TURBO!"). */
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    private static class FloatingText {
        final String text;
        final Color color;
        final float x;
        float y;
        float life = 1.2f;

        FloatingText(String text, Color color, float x, float y) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
        }
    }
    private boolean disposed = false;

    // --- Para deducir sonidos a partir de los estados ---
    private long lastProcessedSeq = -1;
    private Set<Integer> knownProjectiles = new HashSet<>();
    private float lastHealth = -1f;
    /** Eliminaciones de cada jugador en el último estado (id -> eliminaciones). */
    private Map<Integer, Integer> lastKills = new HashMap<>();
    private boolean wasUsingAbility = false;

    public GameScreen(Main game) {
        this.game = game;
        this.audio = game.getAudioManager();

        worldCamera = new OrthographicCamera();
        worldViewport = new ExtendViewport(480, 270, worldCamera);
        batch = new SpriteBatch();
        hud = new Hud(game.getUi());
        input = new PlayerInputProcessor();
        arena = new Arena();
        labelFont = game.getUi().label;
        hudControls = new HudControls(hud.getViewport());
        inputs = new InputMultiplexer(hudControls, input);
        worldShapes.setAutoShapeType(true);

        loadAssets();
    }

    private void loadAssets() {
        // Cada personaje tiene su hoja de sprites "<nombre>_walk.png" y su
        // proyectil "projectile_<nombre>.png" (por ejemplo, iris_walk.png).
        CharacterType[] types = CharacterType.values();
        animations = new DirectionalAnimation[types.length];
        projectileTextures = new Texture[types.length];
        for (CharacterType type : types) {
            String name = type.name().toLowerCase();
            Texture sheet = new Texture(Gdx.files.internal(name + "_walk.png"));
            animations[type.id()] = new DirectionalAnimation(sheet, 32, 32, 4, WALK_FRAME_TIME[type.id()]);
            projectileTextures[type.id()] = new Texture(Gdx.files.internal("projectile_" + name + ".png"));
        }
        powerUpPad = new Texture(Gdx.files.internal("powerup_pad.png"));
        powerUpPad.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        regenZoneTexture = new Texture(Gdx.files.internal("regen_zone.png"));
        regenZoneTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        floorTexture = repeating("floor_tile.png");
        floorRegion = new TextureRegion(floorTexture, 0, 0, (int) Arena.WIDTH, (int) Arena.HEIGHT);
        wallTexture = repeating("wall_tile.png");
        wallVerticalTexture = repeating("wall_tile_v.png");
        coverTexture = new Texture(Gdx.files.internal("cover_block.png"));

        coreDecal = new Texture(Gdx.files.internal("core_decal.png"));
        spawnBlue = new Texture(Gdx.files.internal("spawn_blue.png"));
        spawnRed = new Texture(Gdx.files.internal("spawn_red.png"));
        teamRing = new Texture(Gdx.files.internal("team_ring.png"));
        shieldTexture = new Texture(Gdx.files.internal("shield.png"));
        shieldRegion = new TextureRegion(shieldTexture);
    }

    /** Carga una textura que se repite (como baldosas) al dibujar un área más grande. */
    private static Texture repeating(String file) {
        Texture t = new Texture(Gdx.files.internal(file));
        t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        return t;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(inputs);
        audio.playMusic();
    }

    // ------------------------------------------------------------------
    // Bucle del cliente
    // ------------------------------------------------------------------

    @Override
    public void render(float delta) {
        // Si la partida terminó o se perdió la conexión, se cambia de pantalla.
        if (game.followServer(MatchPhase.PLAYING)) return;

        ClientNetwork net = game.getNetwork();
        animationTimer += delta;
        for (DirectionalAnimation animation : animations) animation.update(delta);

        if (input.isMuteJustPressed()) audio.MutearDesmutear();
        if (input.isVolumeUpJustPressed()) audio.SubirVolumen();
        if (input.isVolumeDownJustPressed()) audio.BajarVolumen();

        if (input.isPauseJustPressed()) {
            input.resetTransient();
            input.releaseAllKeys();
            hudControls.reset();
            game.setScreen(new PauseScreen(game, this));
            return;
        }

        lastDelta = delta;
        hudControls.update(delta);
        ClientNetwork.SnapshotPair pair = net.getSnapshots();
        if (pair == null) return;

        // 1. Enviar entradas al servidor: movimiento, punto de mira, disparo y habilidad.
        sendInputs(net, pair.current.findPlayer(net.getPlayerId()), pair.current);
        input.resetTransient();

        // 2. Interpolación entre los dos últimos estados recibidos.
        float alpha = interpolationAlpha(pair);
        int myId = net.getPlayerId();

        playSoundEvents(pair.current, myId);
        detectPowerUpPickups(pair.current, myId, delta);

        // 3. Cámara sobre mi jugador y dibujo.
        WorldSnapshot.PlayerState me = pair.current.findPlayer(myId);
        if (me != null) {
            updateCamera(lerpX(pair, me, alpha), lerpY(pair, me, alpha));
        }
        draw(pair, alpha, me, net.getPingMs());
    }

    // ------------------------------------------------------------------
    // Entradas: teclado, mouse y botones del HUD
    // ------------------------------------------------------------------

    /**
     * Arma y envía la entrada de este frame. El punto de mira es la posición
     * del mouse en el mapa, salvo que se esté usando un botón del HUD: en ese
     * caso apunta en la dirección en que se arrastró el botón (o al enemigo
     * más cercano si solo se tocó). Son solo pedidos: el servidor decide.
     */
    private void sendInputs(ClientNetwork net, WorldSnapshot.PlayerState me, WorldSnapshot snap) {
        Vector3 mouse = worldViewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        float aimX = mouse.x, aimY = mouse.y;
        boolean fire = input.isFirePressed();
        boolean ability = input.isAbilityJustPressed();

        if (me != null) {
            if (hudControls.isAimingAttack()) {
                Vector2 d = new Vector2(hudControls.getAttackDrag()).nor();
                aimX = me.x + d.x * AIM_DISTANCE;
                aimY = me.y + d.y * AIM_DISTANCE;
            } else if (hudControls.isShooting()) {
                fire = true;
                Vector2 target = hudControls.isShotAuto() ? autoAimTarget(me, snap)
                    : new Vector2(me.x + hudControls.getShotDir().x * AIM_DISTANCE,
                                  me.y + hudControls.getShotDir().y * AIM_DISTANCE);
                aimX = target.x;
                aimY = target.y;
            }
            if (hudControls.isAimingSuper()) {
                Vector2 d = new Vector2(hudControls.getSuperDrag()).nor();
                aimX = me.x + d.x * AIM_DISTANCE;
                aimY = me.y + d.y * AIM_DISTANCE;
            }
            if (hudControls.consumeSuper()) {
                ability = true;
                if (!hudControls.isSuperAuto()) {
                    aimX = me.x + hudControls.getSuperDir().x * AIM_DISTANCE;
                    aimY = me.y + hudControls.getSuperDir().y * AIM_DISTANCE;
                } else {
                    Vector2 target = autoAimTarget(me, snap);
                    aimX = target.x;
                    aimY = target.y;
                }
            }
        }
        net.sendInput(input.getMoveX(), input.getMoveY(), aimX, aimY, fire, ability);
    }

    /** Enemigo vivo más cercano dentro del alcance; si no hay, hacia donde mira el jugador. */
    private Vector2 autoAimTarget(WorldSnapshot.PlayerState me, WorldSnapshot snap) {
        WorldSnapshot.PlayerState best = null;
        float bestDist = AUTO_AIM_RANGE;
        for (WorldSnapshot.PlayerState p : snap.players) {
            // Solo rivales vivos; a un Phantom camuflado no se lo puede "ver".
            if (!p.alive || !snap.areEnemies(me, p) || isCamouflaged(p)) continue;
            float d = Vector2.dst(me.x, me.y, p.x, p.y);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        if (best != null) return new Vector2(best.x, best.y);
        switch (me.facing) {
            case Direction.LEFT: return new Vector2(me.x - AIM_DISTANCE, me.y);
            case Direction.RIGHT: return new Vector2(me.x + AIM_DISTANCE, me.y);
            case Direction.UP: return new Vector2(me.x, me.y + AIM_DISTANCE);
            default: return new Vector2(me.x, me.y - AIM_DISTANCE);
        }
    }

    /**
     * Mientras se arrastra un botón del HUD, dibuja en el mapa hacia dónde se
     * va a disparar (línea roja) o usar la habilidad (amarillo).
     */
    private void drawAimIndicator(WorldSnapshot.PlayerState me, ClientNetwork.SnapshotPair pair, float alpha) {
        boolean attack = hudControls.isAimingAttack();
        boolean ability = hudControls.isAimingSuper();
        if (me == null || !me.alive || (!attack && !ability)) return;

        float x = lerpX(pair, me, alpha), y = lerpY(pair, me, alpha);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        worldShapes.setProjectionMatrix(worldCamera.combined);
        worldShapes.begin(ShapeRenderer.ShapeType.Filled);
        if (attack) {
            Vector2 d = new Vector2(hudControls.getAttackDrag()).nor();
            // Línea punteada de mira
            for (float t = 14; t < 170; t += 12) {
                float a = 0.85f * (1f - t / 190f);
                worldShapes.setColor(1f, 0.3f, 0.3f, a);
                worldShapes.rectLine(x + d.x * t, y + d.y * t, x + d.x * (t + 7), y + d.y * (t + 7), 2.4f);
            }
            worldShapes.setColor(1f, 0.35f, 0.35f, 0.9f);
            worldShapes.circle(x + d.x * 176, y + d.y * 176, 3.5f, 16);
        }
        if (ability) {
            Vector2 d = new Vector2(hudControls.getSuperDrag()).nor();
            CharacterType type = CharacterType.fromId(me.character);
            if (type == CharacterType.BRUTUS) {
                // Cono del escudo: la zona de la que frena los disparos
                float angle = d.angleDeg();
                worldShapes.setColor(1f, 0.85f, 0.2f, 0.28f);
                worldShapes.arc(x, y, 44, angle - 66, 132, 24);
            } else if (type == CharacterType.IRIS) {
                // Campo de Regeneración: se crea donde está parada Iris
                worldShapes.setColor(0.35f, 1f, 0.55f, 0.22f);
                worldShapes.circle(x, y, CharacterType.REGEN_RADIUS, 40);
            } else if (type == CharacterType.PHANTOM) {
                // Camuflaje: un aro que se cierra sobre Phantom
                worldShapes.setColor(1f, 0.25f, 0.35f, 0.35f);
                worldShapes.set(ShapeRenderer.ShapeType.Line);
                worldShapes.circle(x, y, 20, 32);
                worldShapes.set(ShapeRenderer.ShapeType.Filled);
            } else {
                // Flecha del impulso: aproximadamente la distancia que recorre
                float len = type.speed * 3.2f * type.abilityDuration;
                worldShapes.setColor(1f, 0.88f, 0.25f, 0.6f);
                worldShapes.rectLine(x, y, x + d.x * len, y + d.y * len, 6f);
                Vector2 side = new Vector2(-d.y, d.x);
                float tx = x + d.x * (len + 10), ty = y + d.y * (len + 10);
                worldShapes.triangle(tx, ty,
                    x + d.x * len + side.x * 8, y + d.y * len + side.y * 8,
                    x + d.x * len - side.x * 8, y + d.y * len - side.y * 8);
            }
        }
        worldShapes.end();
    }

    // ------------------------------------------------------------------
    // Interpolación
    // ------------------------------------------------------------------

    /**
     * Qué tan avanzado está el tiempo entre el estado anterior (0) y el
     * actual (1). Se mide con el tiempo que tardó en llegar el estado actual
     * después del anterior.
     */
    private float interpolationAlpha(ClientNetwork.SnapshotPair pair) {
        long interval = pair.current.receivedNanos - pair.previous.receivedNanos;
        if (interval <= 0) return 1f;
        float elapsed = System.nanoTime() - pair.current.receivedNanos;
        return MathUtils.clamp(elapsed / interval, 0f, 1f);
    }

    private float lerpX(ClientNetwork.SnapshotPair pair, WorldSnapshot.PlayerState p, float alpha) {
        WorldSnapshot.PlayerState before = pair.previous.findPlayer(p.id);
        if (!canInterpolate(before, p)) return p.x;
        return MathUtils.lerp(before.x, p.x, alpha);
    }

    private float lerpY(ClientNetwork.SnapshotPair pair, WorldSnapshot.PlayerState p, float alpha) {
        WorldSnapshot.PlayerState before = pair.previous.findPlayer(p.id);
        if (!canInterpolate(before, p)) return p.y;
        return MathUtils.lerp(before.y, p.y, alpha);
    }

    private boolean canInterpolate(WorldSnapshot.PlayerState before, WorldSnapshot.PlayerState now) {
        if (before == null || !before.alive) return false;
        return Math.abs(before.x - now.x) < TELEPORT_DISTANCE && Math.abs(before.y - now.y) < TELEPORT_DISTANCE;
    }

    // ------------------------------------------------------------------
    // Sonidos deducidos del estado
    // ------------------------------------------------------------------

    private void playSoundEvents(WorldSnapshot snap, int myId) {
        if (snap.seq == lastProcessedSeq) return; // este estado ya se procesó
        boolean first = lastProcessedSeq < 0;
        lastProcessedSeq = snap.seq;

        Set<Integer> current = new HashSet<>();
        boolean newShot = false;
        for (WorldSnapshot.ProjectileState p : snap.projectiles) {
            current.add(p.id);
            if (!knownProjectiles.contains(p.id)) newShot = true;
        }
        knownProjectiles = current;
        if (newShot && !first) audio.playShoot();

        WorldSnapshot.PlayerState me = snap.findPlayer(myId);
        if (me != null) {
            if (lastHealth >= 0 && me.health < lastHealth) audio.playHit();
            lastHealth = me.health;
            if (me.abilityActive && !wasUsingAbility) {
                switch (CharacterType.fromId(me.character)) {
                    case BRUTUS: audio.playShield(); break;
                    case IRIS: audio.playHeal(); break;
                    case PHANTOM: audio.playCamo(); break;
                    default: audio.playDash(); break;
                }
            }
            wasUsingAbility = me.abilityActive;
        }

        // Eliminaciones de cada jugador: si a alguno le subió, hubo una eliminación.
        // (Antes se sumaban las de todos: si alguien se iba justo cuando otro
        // eliminaba, la suma no cambiaba y el sonido no salía.)
        boolean someoneScored = false;
        Map<Integer, Integer> kills = new HashMap<>();
        for (WorldSnapshot.PlayerState p : snap.players) {
            kills.put(p.id, p.kills);
            Integer before = lastKills.get(p.id);
            if (before != null && p.kills > before) someoneScored = true;
        }
        if (someoneScored && !first) audio.playEnemyDown();
        lastKills = kills;
    }

    // ------------------------------------------------------------------
    // Cámara y dibujo
    // ------------------------------------------------------------------

    private void updateCamera(float targetX, float targetY) {
        float halfWidth = worldCamera.viewportWidth * worldCamera.zoom / 2f;
        float halfHeight = worldCamera.viewportHeight * worldCamera.zoom / 2f;

        float camX = (halfWidth * 2 >= Arena.WIDTH)
            ? Arena.WIDTH / 2f
            : MathUtils.clamp(targetX, halfWidth, Arena.WIDTH - halfWidth);
        float camY = (halfHeight * 2 >= Arena.HEIGHT)
            ? Arena.HEIGHT / 2f
            : MathUtils.clamp(targetY, halfHeight, Arena.HEIGHT - halfHeight);

        worldCamera.position.set(camX, camY, 0);
        worldCamera.update();
    }

    private void draw(ClientNetwork.SnapshotPair pair, float alpha, WorldSnapshot.PlayerState me, float pingMs) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        WorldSnapshot snap = pair.current;
        GameMode mode = snap.getMode();

        worldViewport.apply();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        drawMap(mode);
        drawZones(snap, mode);
        drawPowerUps(snap);

        // Jugadores: anillo del color del equipo (o del jugador en Todos contra
        // todos), sprite del personaje y efectos de la habilidad activa.
        for (WorldSnapshot.PlayerState p : snap.players) {
            if (!p.alive) continue;
            float x = lerpX(pair, p, alpha);
            float y = lerpY(pair, p, alpha);
            CharacterType type = CharacterType.fromId(p.character);
            float opacity = opacityFor(p, me, snap);
            boolean hiddenEnemy = opacity < CAMO_ALPHA_ALLY;

            if (!hiddenEnemy && p.hasAnyBoost()) {
                // Aura del Power-Up activo (si tiene varios, va alternando el color)
                Color aura = boostColor(p);
                float pulse = 0.55f + 0.3f * MathUtils.sin(animationTimer * 7f);
                batch.setColor(aura.r, aura.g, aura.b, pulse * opacity);
                batch.draw(teamRing, x - 22, y - 22, 44, 22);
            }

            if (!hiddenEnemy) {
                Color ring = UiKit.playerColor(mode, p.team);
                batch.setColor(ring.r, ring.g, ring.b, opacity);
                batch.draw(teamRing, x - 16, y - 18, 32, 16);
            }

            boolean visible = !p.invulnerable || ((int) (animationTimer * 12) % 2 == 0);
            if (visible) {
                if (p.abilityActive && type == CharacterType.VOLT) batch.setColor(DASH_TINT);
                else batch.setColor(1f, 1f, 1f, opacity);
                TextureRegion frame = animations[type.id()].getFrame(p.facing, p.moving);
                float size = GameConstants.PLAYER_DRAW_SIZE;
                // El sprite se dibuja un poco más arriba que el centro, para
                // que los pies queden sobre el anillo.
                batch.draw(frame, x - size / 2f, y - size / 2f + 4, size, size);
            }
            batch.setColor(Color.WHITE);

            if (p.abilityActive && type == CharacterType.BRUTUS) {
                // El dibujo del escudo apunta a la derecha; se rota según el ángulo.
                batch.draw(shieldRegion, x - 24, y - 24, 24, 24, 48, 48, 1f, 1f, p.shieldAngle);
            }
        }

        // Proyectiles: imagen según el personaje que disparó, teñida con el color del equipo.
        for (WorldSnapshot.ProjectileState p : snap.projectiles) {
            WorldSnapshot.ProjectileState before = pair.previous.findProjectile(p.id);
            float x = before == null ? p.x : MathUtils.lerp(before.x, p.x, alpha);
            float y = before == null ? p.y : MathUtils.lerp(before.y, p.y, alpha);
            CharacterType type = CharacterType.fromId(p.character);
            float size = type.projectileSize * 1.5f;   // el brillo es más grande que el área de choque
            batch.setColor(UiKit.playerColor(mode, p.team));
            batch.draw(projectileTextures[type.id()], x - size / 2f, y - size / 2f, size, size);
        }
        batch.setColor(Color.WHITE);

        // Nombres sobre cada jugador (un Phantom camuflado rival no muestra el suyo).
        for (WorldSnapshot.PlayerState p : snap.players) {
            if (!p.alive || opacityFor(p, me, snap) < CAMO_ALPHA_ALLY) continue;
            boolean isMe = me != null && p.id == me.id;
            labelFont.setColor(isMe ? Color.YELLOW : UiKit.playerColor(mode, p.team));
            String label = isMe ? "Vos" : p.name;
            layout.setText(labelFont, label);
            labelFont.draw(batch, label, lerpX(pair, p, alpha) - layout.width / 2f,
                lerpY(pair, p, alpha) + GameConstants.PLAYER_DRAW_SIZE / 2f + 12);
        }

        // Textos de Power-Ups agarrados
        for (FloatingText t : floatingTexts) {
            float a = MathUtils.clamp(t.life / 0.5f, 0f, 1f);
            labelFont.setColor(t.color.r, t.color.g, t.color.b, a);
            layout.setText(labelFont, t.text);
            labelFont.draw(batch, t.text, t.x - layout.width / 2f, t.y);
        }
        labelFont.setColor(Color.WHITE);

        batch.end();

        hud.getViewport().apply();
        drawAimIndicator(me, pair, alpha);

        hud.render(batch, me, snap, audio.isMuted(), audio.getVolume(), pingMs, hudControls, lastDelta);
    }

    private static boolean isCamouflaged(WorldSnapshot.PlayerState p) {
        return p.abilityActive && CharacterType.fromId(p.character) == CharacterType.PHANTOM;
    }

    /**
     * Qué tan visible se dibuja un jugador. El servidor solo informa que
     * Phantom está camuflado; cada cliente decide cómo mostrarlo: casi
     * invisible si es rival, translúcido si es uno mismo o un compañero.
     */
    private float opacityFor(WorldSnapshot.PlayerState p, WorldSnapshot.PlayerState me, WorldSnapshot snap) {
        if (!isCamouflaged(p)) return 1f;
        boolean friendly = me == null || p.id == me.id || !snap.areEnemies(me, p);
        return friendly ? CAMO_ALPHA_ALLY : CAMO_ALPHA_ENEMY;
    }

    /** Color del aura: el de un Power-Up activo; si hay varios, cambia cada medio segundo. */
    private Color boostColor(WorldSnapshot.PlayerState p) {
        List<PowerUpType> active = new ArrayList<>();
        for (PowerUpType t : PowerUpType.values()) {
            if (p.boost(t) > 0f) active.add(t);
        }
        return UiKit.powerUpColor(active.get((int) (animationTimer * 2f) % active.size()));
    }

    /** Power-Ups en el mapa: flotan arriba y abajo sobre una base que late. */
    private void drawPowerUps(WorldSnapshot snap) {
        for (WorldSnapshot.PowerUpState pu : snap.powerUps) {
            Color c = UiKit.powerUpColor(pu.getType());
            float pulse = 0.6f + 0.4f * MathUtils.sin(animationTimer * 5f + pu.id);
            batch.setColor(c.r, c.g, c.b, 0.75f * pulse);
            batch.draw(powerUpPad, pu.x - 20, pu.y - 20, 40, 40);
            float bob = MathUtils.sin(animationTimer * 3f + pu.id) * 2.5f;
            batch.setColor(Color.WHITE);
            batch.draw(game.getUi().powerUpIcon(pu.getType()), pu.x - 12, pu.y - 12 + bob + 3, 24, 24);
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * El servidor no avisa "fulano agarró un Power-Up": el cliente lo deduce.
     * Si un Power-Up desapareció durante la partida y había un jugador encima,
     * ese jugador lo agarró: se muestra el nombre del efecto y, si fui yo,
     * suena el efecto de sonido.
     */
    private void detectPowerUpPickups(WorldSnapshot snap, int myId, float delta) {
        if (snap.phase == MatchPhase.PLAYING) {
            for (WorldSnapshot.PowerUpState old : lastPowerUps.values()) {
                if (snap.findPowerUp(old.id) != null) continue;
                WorldSnapshot.PlayerState taker = null;
                float best = (PowerUpType.PICKUP_RADIUS + 14f) * (PowerUpType.PICKUP_RADIUS + 14f);
                for (WorldSnapshot.PlayerState p : snap.players) {
                    if (!p.alive) continue;
                    float dx = p.x - old.x, dy = p.y - old.y;
                    float d2 = dx * dx + dy * dy;
                    if (d2 <= best) {
                        best = d2;
                        taker = p;
                    }
                }
                if (taker == null) continue;
                PowerUpType type = old.getType();
                if (taker.id == myId) audio.playPowerUp();
                floatingTexts.add(new FloatingText("¡" + type.displayName.toUpperCase() + "!",
                    UiKit.powerUpColor(type), taker.x, taker.y + GameConstants.PLAYER_DRAW_SIZE / 2f + 26));
            }
        }
        lastPowerUps.clear();
        for (WorldSnapshot.PowerUpState pu : snap.powerUps) lastPowerUps.put(pu.id, pu);

        Iterator<FloatingText> it = floatingTexts.iterator();
        while (it.hasNext()) {
            FloatingText t = it.next();
            t.life -= delta;
            t.y += 22f * delta;
            if (t.life <= 0f) it.remove();
        }
    }

    /** Campos de Regeneración de Iris: círculo verde que late y se apaga al terminar. */
    private void drawZones(WorldSnapshot snap, GameMode mode) {
        for (WorldSnapshot.ZoneState z : snap.zones) {
            float fade = MathUtils.clamp(z.remaining / 0.6f, 0f, 1f);       // se apaga en el final
            float pulse = 0.75f + 0.25f * MathUtils.sin(animationTimer * 5f);
            Color c = mode == GameMode.TEAMS ? UiKit.playerColor(mode, z.team) : UiKit.GREEN;
            // Mezcla del verde de curación con el color del equipo dueño
            batch.setColor(0.6f * UiKit.GREEN.r + 0.4f * c.r, 0.6f * UiKit.GREEN.g + 0.4f * c.g,
                0.6f * UiKit.GREEN.b + 0.4f * c.b, 0.85f * fade * pulse);
            float size = z.radius * 2f;
            batch.draw(regenZoneTexture, z.x - z.radius, z.y - z.radius, size, size);
        }
        batch.setColor(Color.WHITE);
    }

    /** Piso, marcas en el piso, paredes y bloques de cobertura. */
    private void drawMap(GameMode mode) {
        batch.draw(floorRegion, 0, 0, Arena.WIDTH, Arena.HEIGHT);

        // Lugares donde aparecen los Power-Ups (marcados tenues aunque estén vacíos).
        batch.setColor(1f, 1f, 1f, 0.18f);
        for (float[] pos : Arena.POWERUP_SPAWNS) {
            batch.draw(powerUpPad, pos[0] - 20, pos[1] - 20, 40, 40);
        }
        batch.setColor(Color.WHITE);

        // Las zonas de aparición de cada equipo solo tienen sentido en Equipos.
        if (mode == GameMode.TEAMS) {
            float zoneY = Arena.HEIGHT / 2f - SPAWN_ZONE_H / 2f;
            batch.draw(spawnBlue, SPAWN_MARGIN_X - SPAWN_ZONE_W / 2f, zoneY);
            batch.draw(spawnRed, Arena.WIDTH - SPAWN_MARGIN_X - SPAWN_ZONE_W / 2f, zoneY);
        }
        batch.draw(coreDecal, Arena.WIDTH / 2f - coreDecal.getWidth() / 2f, Arena.HEIGHT / 2f - coreDecal.getHeight() / 2f);

        for (Obstacle obstacle : arena.getObstacles()) {
            Rectangle b = obstacle.getBounds();
            if (b.width == coverTexture.getWidth() && b.height == coverTexture.getHeight()) {
                batch.draw(coverTexture, b.x, b.y, b.width, b.height);
            } else {
                // Las paredes se arman repitiendo la baldosa a lo largo:
                // horizontal o vertical según la forma de la pared.
                Texture tile = b.width >= b.height ? wallTexture : wallVerticalTexture;
                batch.draw(tile, b.x, b.y, b.width, b.height, 0, 0, (int) b.width, (int) b.height, false, false);
            }
        }
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        hud.resize(width, height);
    }

    /** Lo llama PauseScreen al volver a la partida. */
    public void resumeGame() {
        Gdx.input.setInputProcessor(inputs);
        input.releaseAllKeys();
        input.resetTransient();
        hudControls.reset();
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        // Guarda: puede liberarse desde la pausa o al terminar la partida.
        if (disposed) return;
        disposed = true;

        batch.dispose();
        hud.dispose();
        worldShapes.dispose();
        for (DirectionalAnimation animation : animations) animation.dispose(); // libera las hojas de sprites
        for (Texture t : projectileTextures) t.dispose();
        floorTexture.dispose();
        wallTexture.dispose();
        wallVerticalTexture.dispose();
        coverTexture.dispose();
        coreDecal.dispose();
        spawnBlue.dispose();
        spawnRed.dispose();
        teamRing.dispose();
        shieldTexture.dispose();
        regenZoneTexture.dispose();
        powerUpPad.dispose();
    }
}
