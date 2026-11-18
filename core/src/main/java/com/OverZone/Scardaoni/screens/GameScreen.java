package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.audio.AudioManager;
import com.OverZone.Scardaoni.entities.Obstacle;
import com.OverZone.Scardaoni.entities.Player;
import com.OverZone.Scardaoni.entities.Projectile;
import com.OverZone.Scardaoni.game.MatchPhase;
import com.OverZone.Scardaoni.game.Team;
import com.OverZone.Scardaoni.graphics.DirectionalAnimation;
import com.OverZone.Scardaoni.input.PlayerInputProcessor;
import com.OverZone.Scardaoni.network.ClientNetwork;
import com.OverZone.Scardaoni.network.WorldSnapshot;
import com.OverZone.Scardaoni.ui.Hud;
import com.OverZone.Scardaoni.world.Arena;

import java.util.HashSet;
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

    private static final Color BLUE_TINT = new Color(0.65f, 0.8f, 1f, 1f);
    private static final Color RED_TINT = new Color(1f, 0.65f, 0.65f, 1f);

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
    private final GlyphLayout layout = new GlyphLayout();

    private Texture playerSheet, obstacleTexture, floorTexture, projOwnTex, projEnemyTex;
    private DirectionalAnimation playerAnimation;
    private TextureRegion floorRegion;

    private float animationTimer = 0f;
    private boolean disposed = false;

    // --- Para deducir sonidos a partir de los estados ---
    private long lastProcessedSeq = -1;
    private Set<Integer> knownProjectiles = new HashSet<>();
    private float lastHealth = -1f;
    private int lastScoreTotal = -1;
    private boolean wasDashing = false;

    public GameScreen(Main game) {
        this.game = game;
        this.audio = game.getAudioManager();

        worldCamera = new OrthographicCamera();
        worldViewport = new ExtendViewport(480, 270, worldCamera);
        batch = new SpriteBatch();
        hud = new Hud();
        input = new PlayerInputProcessor();
        arena = new Arena();
        labelFont = new BitmapFont();
        labelFont.getData().setScale(0.6f);

        loadAssets();
    }

    private void loadAssets() {
        playerSheet = new Texture(Gdx.files.internal("player_walk.png"));
        playerAnimation = new DirectionalAnimation(playerSheet, 32, 32, 4, 0.12f);

        obstacleTexture = new Texture(Gdx.files.internal("obstacle.png"));

        floorTexture = new Texture(Gdx.files.internal("floor_tile.png"));
        floorTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        floorRegion = new TextureRegion(floorTexture, 0, 0, (int) Arena.WIDTH, (int) Arena.HEIGHT);

        projOwnTex = new Texture(Gdx.files.internal("projectile_player.png"));
        projEnemyTex = new Texture(Gdx.files.internal("projectile_enemy.png"));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(input);
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
        playerAnimation.update(delta);

        if (input.isMuteJustPressed()) audio.MutearDesmutear();
        if (input.isVolumeUpJustPressed()) audio.SubirVolumen();
        if (input.isVolumeDownJustPressed()) audio.BajarVolumen();

        if (input.isPauseJustPressed()) {
            input.resetTransient();
            input.releaseAllKeys();
            game.setScreen(new PauseScreen(game, this));
            return;
        }

        // 1. Enviar entradas al servidor (la posición del mouse, en coordenadas del mapa).
        Vector3 mouse = worldViewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        net.sendInput(input.getMoveX(), input.getMoveY(), mouse.x, mouse.y,
            input.isFirePressed(), input.isAbilityJustPressed());
        input.resetTransient();

        // 2. Tomar los dos últimos estados recibidos.
        ClientNetwork.SnapshotPair pair = net.getSnapshots();
        if (pair == null) return;
        float alpha = interpolationAlpha(pair);
        int myId = net.getPlayerId();

        playSoundEvents(pair.current, myId);

        // 3. Cámara sobre mi jugador y dibujo.
        WorldSnapshot.PlayerState me = pair.current.findPlayer(myId);
        if (me != null) {
            updateCamera(lerpX(pair, me, alpha), lerpY(pair, me, alpha));
        }
        draw(pair, alpha, me, net.getPingMs());
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
            if (me.dashing && !wasDashing) audio.playDash();
            wasDashing = me.dashing;
        }

        int scoreTotal = snap.scoreBlue + snap.scoreRed;
        if (lastScoreTotal >= 0 && scoreTotal > lastScoreTotal) audio.playEnemyDown();
        lastScoreTotal = scoreTotal;
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
        int myTeam = me == null ? Team.NONE : me.team;

        worldViewport.apply();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        batch.draw(floorRegion, 0, 0, Arena.WIDTH, Arena.HEIGHT);

        for (Obstacle obstacle : arena.getObstacles()) {
            Rectangle b = obstacle.getBounds();
            batch.draw(obstacleTexture, b.x, b.y, b.width, b.height);
        }

        // Jugadores, teñidos con el color de su equipo.
        for (WorldSnapshot.PlayerState p : snap.players) {
            if (!p.alive) continue;
            boolean visible = !p.invulnerable || ((int) (animationTimer * 12) % 2 == 0);
            if (!visible) continue;
            float x = lerpX(pair, p, alpha);
            float y = lerpY(pair, p, alpha);
            batch.setColor(p.team == Team.BLUE ? BLUE_TINT : RED_TINT);
            TextureRegion frame = playerAnimation.getFrame(p.facing, p.moving);
            batch.draw(frame, x - Player.SIZE / 2f, y - Player.SIZE / 2f, Player.SIZE, Player.SIZE);
        }
        batch.setColor(Color.WHITE);

        // Proyectiles: los de mi equipo con una imagen y los rivales con otra.
        for (WorldSnapshot.ProjectileState p : snap.projectiles) {
            WorldSnapshot.ProjectileState before = pair.previous.findProjectile(p.id);
            float x = before == null ? p.x : MathUtils.lerp(before.x, p.x, alpha);
            float y = before == null ? p.y : MathUtils.lerp(before.y, p.y, alpha);
            Texture tex = p.team == myTeam ? projOwnTex : projEnemyTex;
            batch.draw(tex, x - Projectile.SIZE / 2f, y - Projectile.SIZE / 2f, Projectile.SIZE, Projectile.SIZE);
        }

        // Nombres sobre cada jugador.
        for (WorldSnapshot.PlayerState p : snap.players) {
            if (!p.alive) continue;
            boolean isMe = me != null && p.id == me.id;
            labelFont.setColor(isMe ? Color.YELLOW : (p.team == Team.BLUE ? Color.SKY : Color.SALMON));
            String label = isMe ? "Vos" : p.name;
            layout.setText(labelFont, label);
            labelFont.draw(batch, label, lerpX(pair, p, alpha) - layout.width / 2f, lerpY(pair, p, alpha) + Player.SIZE);
        }

        batch.end();

        hud.getViewport().apply();
        hud.render(batch, me, snap, audio.isMuted(), audio.getVolume(), pingMs);
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
        Gdx.input.setInputProcessor(input);
        input.releaseAllKeys();
        input.resetTransient();
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
        labelFont.dispose();
        playerAnimation.dispose(); // libera playerSheet
        obstacleTexture.dispose();
        floorTexture.dispose();
        projOwnTex.dispose();
        projEnemyTex.dispose();
    }
}
