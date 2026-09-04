package com.OverZone.Scardaoni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.Main;
import com.OverZone.Scardaoni.audio.AudioManager;
import com.OverZone.Scardaoni.entities.EnemyDummy;
import com.OverZone.Scardaoni.entities.Obstacle;
import com.OverZone.Scardaoni.entities.Player;
import com.OverZone.Scardaoni.entities.Projectile;
import com.OverZone.Scardaoni.graphics.DirectionalAnimation;
import com.OverZone.Scardaoni.input.PlayerInputProcessor;
import com.OverZone.Scardaoni.ui.Hud;
import com.OverZone.Scardaoni.world.Arena;

public class GameScreen implements Screen {

    private static final float MATCH_DURATION = 90f; // segundos de partida
    private static final float PLAYER_FIRE_COOLDOWN = 0.22f;

    private final Main game;
    private final AudioManager audio;

    private final OrthographicCamera worldCamera;
    private final Viewport worldViewport;

    private final SpriteBatch batch;
    private final Hud hud;
    private final PlayerInputProcessor input;
    private final Arena arena;

    // --- Recursos gráficos ---
    private Texture playerSheet, enemySheet, obstacleTexture, floorTexture, projPlayerTex, projEnemyTex;
    private DirectionalAnimation playerAnimation;
    private TextureRegion[] enemyFrames;
    private TextureRegion floorRegion;

    // --- Entidades ---
    private Player player;
    private final Array<EnemyDummy> enemies = new Array<>();
    private final Array<Projectile> projectiles = new Array<>();

    // --- Estado de partida ---
    private int score = 0;
    private float timeRemaining = MATCH_DURATION;
    private float playerFireCooldown = 0f;
    private float animationTimer = 0f;
    private boolean matchFinished = false;
    private boolean disposed = false;

    public GameScreen(Main game) {
        this.game = game;
        this.audio = game.getAudioManager();

        worldCamera = new OrthographicCamera();
        // Ventana visible de 480x270 unidades. La arena mide 960x540, por lo
        // que la cámara necesariamente debe desplazarse siguiendo al jugador.
        // ExtendViewport evita deformaciones al cambiar el tamaño de ventana.
        worldViewport = new ExtendViewport(480, 270, worldCamera);

        batch = new SpriteBatch();
        hud = new Hud();
        input = new PlayerInputProcessor();
        arena = new Arena();

        loadAssets();
        resetMatch();
    }

    private void loadAssets() {
        // Punto 2: spritesheet de 4 filas (direcciones) x 4 columnas (cuadros)
        playerSheet = new Texture(Gdx.files.internal("player_walk.png"));
        playerAnimation = new DirectionalAnimation(playerSheet, 32, 32, 4, 0.12f);

        enemySheet = new Texture(Gdx.files.internal("enemy_dummy.png"));
        enemyFrames = TextureRegion.split(enemySheet, 32, 32)[0];

        obstacleTexture = new Texture(Gdx.files.internal("obstacle.png"));

        floorTexture = new Texture(Gdx.files.internal("floor_tile.png"));
        floorTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        // Región mayor que la textura: con wrap Repeat, el piso se repite en mosaico.
        floorRegion = new TextureRegion(floorTexture, 0, 0, (int) Arena.WIDTH, (int) Arena.HEIGHT);

        projPlayerTex = new Texture(Gdx.files.internal("projectile_player.png"));
        projEnemyTex = new Texture(Gdx.files.internal("projectile_enemy.png"));
    }

    /** Deja la partida en su estado inicial (usado al crear la pantalla y al reiniciar). */
    private void resetMatch() {
        // El punto de aparición se consulta a la arena, que garantiza que esté
        // libre de obstáculos (de lo contrario el jugador quedaría trabado).
        Vector2 spawn = arena.findFreeSpawn(Player.SIZE);
        player = new Player(spawn.x, spawn.y);
        enemies.clear();
        // Posiciones verificadas como libres de obstáculos: si un enemigo
        // apareciera dentro de un bloque, sus disparos impactarían contra él
        // en el mismo frame en que se crean.
        addEnemyIfFree(150, Arena.HEIGHT / 2f);
        addEnemyIfFree(Arena.WIDTH - 150, Arena.HEIGHT / 2f);
        addEnemyIfFree(Arena.WIDTH / 2f, 150);
        projectiles.clear();
        score = 0;
        timeRemaining = MATCH_DURATION;
        playerFireCooldown = 0f;
        matchFinished = false;
    }

    /** Agrega un enemigo solo si la posición no se superpone con un obstáculo. */
    private void addEnemyIfFree(float x, float y) {
        if (arena.isFreeFor(x, y, EnemyDummy.SIZE)) {
            enemies.add(new EnemyDummy(x, y));
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(input);
        audio.playMusic();
    }

    // ------------------------------------------------------------------
    // Bucle principal
    // ------------------------------------------------------------------

    @Override
    public void render(float delta) {
        update(delta);
        // Si la partida terminó, ya se cambió de pantalla: no se dibuja ni se
        // sigue usando esta pantalla, y se liberan sus recursos.
        if (matchFinished) {
            dispose();
            return;
        }
        draw();
    }

    private void update(float delta) {
        animationTimer += delta;
        playerAnimation.update(delta);

        updateMouseWorldPosition();

        // Acciones globales que no dependen del estado del jugador.
        if (input.isMuteJustPressed()) audio.MutearDesmutear();
        if (input.isVolumeUpJustPressed()) audio.SubirVolumen();
        if (input.isVolumeDownJustPressed()) audio.BajarVolumen();

        // La pausa cambia de pantalla; se sale del update para no seguir
        // simulando la partida en el mismo frame.
        if (input.isPauseJustPressed()) {
            input.resetTransient();
            input.releaseAllKeys();
            game.setScreen(new PauseScreen(game, this));
            return;
        }

        player.update(delta);
        handlePlayerMovement(delta);
        handlePlayerShooting(delta);
        updateEnemies(delta);
        updateProjectiles(delta);
        resolveCollisions();
        updateCamera();

        input.resetTransient();

        timeRemaining -= delta;
        if (!matchFinished && timeRemaining <= 0) {
            endMatch(true);
        } else if (!matchFinished && !player.isAlive()) {
            endMatch(false);
        }
    }

    /** Convierte la posición del mouse (píxeles de pantalla) a coordenadas del mundo. */
    private void updateMouseWorldPosition() {
        Vector3 mouseWorld = worldViewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        input.setMouseWorldPosition(mouseWorld.x, mouseWorld.y);
    }

    private void handlePlayerMovement(float delta) {
        float moveX = input.getMoveX();
        float moveY = input.getMoveY();
        boolean pressingMovementKeys = input.isMoving();

        // Habilidad especial: si hay teclas de movimiento, se impulsa en esa
        // dirección; si no, hacia donde apunta el mouse.
        if (input.isAbilityJustPressed() && !player.isDashing()) {
            float dashX = pressingMovementKeys ? moveX : (input.getMouseWorldX() - player.getX());
            float dashY = pressingMovementKeys ? moveY : (input.getMouseWorldY() - player.getY());
            boolean wasDashing = player.isDashing();
            player.tryStartDash(dashX, dashY);
            if (!wasDashing && player.isDashing()) {
                audio.playDash();
            }
        }

        float speed = player.getCurrentSpeed();
        float dx = 0, dy = 0;

        if (player.isDashing()) {
            // Durante el dash el jugador conserva la dirección del impulso.
            dx = player.getDashDirX() * speed * delta;
            dy = player.getDashDirY() * speed * delta;
        } else if (pressingMovementKeys) {
            float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            if (length > 0) {
                dx = (moveX / length) * speed * delta;
                dy = (moveY / length) * speed * delta;
            }
        }

        moveWithCollision(dx, dy);
        player.setMoving(pressingMovementKeys || player.isDashing());

        if (pressingMovementKeys) {
            player.setFacingDirection(directionFromVector(moveX, moveY));
        } else {
            player.setFacingDirection(directionFromVector(
                input.getMouseWorldX() - player.getX(),
                input.getMouseWorldY() - player.getY()));
        }
    }

    private int directionFromVector(float x, float y) {
        if (Math.abs(x) > Math.abs(y)) {
            return x > 0 ? DirectionalAnimation.RIGHT : DirectionalAnimation.LEFT;
        }
        return y > 0 ? DirectionalAnimation.UP : DirectionalAnimation.DOWN;
    }

    private void moveWithCollision(float dx, float dy) {
        if (dx != 0) {
            float newX = player.getX() + dx;
            Rectangle testX = new Rectangle(newX - Player.SIZE / 2f,
                player.getY() - Player.SIZE / 2f, Player.SIZE, Player.SIZE);
            if (!arena.collidesWithAny(testX)) {
                player.setX(newX);
            }
        }
        if (dy != 0) {
            float newY = player.getY() + dy;
            Rectangle testY = new Rectangle(player.getX() - Player.SIZE / 2f,
                newY - Player.SIZE / 2f, Player.SIZE, Player.SIZE);
            if (!arena.collidesWithAny(testY)) {
                player.setY(newY);
            }
        }
    }

    private void handlePlayerShooting(float delta) {
        if (playerFireCooldown > 0) {
            playerFireCooldown -= delta;
        }
        if (input.isFirePressed() && playerFireCooldown <= 0) {
            projectiles.add(new Projectile(
                player.getX(), player.getY(),
                input.getMouseWorldX() - player.getX(),
                input.getMouseWorldY() - player.getY(),
                true));
            audio.playShoot();
            playerFireCooldown = PLAYER_FIRE_COOLDOWN;
        }
    }

    private void updateEnemies(float delta) {
        for (EnemyDummy enemy : enemies) {
            enemy.update(delta);
            if (enemy.canFireAt(player.getX(), player.getY())) {
                projectiles.add(new Projectile(
                    enemy.getX(), enemy.getY(),
                    player.getX() - enemy.getX(),
                    player.getY() - enemy.getY(),
                    false));
                enemy.resetFireCooldown();
            }
        }
    }

    private void updateProjectiles(float delta) {
        for (Projectile p : projectiles) {
            p.update(delta);
            if (p.getX() < 0 || p.getX() > Arena.WIDTH || p.getY() < 0 || p.getY() > Arena.HEIGHT) {
                p.kill();
            }
        }
    }

    private void resolveCollisions() {
        for (Projectile p : projectiles) {
            if (!p.isAlive()) continue;
            Rectangle projectileBounds = p.getBounds();

            // Proyectil contra obstáculo del entorno.
            if (arena.collidesWithAny(projectileBounds)) {
                p.kill();
                continue;
            }

            if (p.isFromPlayer()) {
                // Proyectil del jugador contra enemigos.
                for (EnemyDummy enemy : enemies) {
                    if (enemy.isAlive() && enemy.getBounds().overlaps(projectileBounds)) {
                        enemy.takeDamage(Projectile.DAMAGE_PLAYER_TO_ENEMY);
                        p.kill();
                        if (enemy.isAlive()) {
                            audio.playHit();
                        } else {
                            score += EnemyDummy.SCORE_VALUE;
                            audio.playEnemyDown();
                        }
                        break;
                    }
                }
            } else if (player.getBounds().overlaps(projectileBounds)) {
                // Proyectil enemigo contra el jugador.
                if (!player.isInvulnerable()) {
                    player.takeDamage(Projectile.DAMAGE_ENEMY_TO_PLAYER);
                    audio.playHit();
                }
                p.kill();
            }
        }

        // Eliminación de los proyectiles marcados como muertos.
        for (int i = projectiles.size - 1; i >= 0; i--) {
            if (!projectiles.get(i).isAlive()) {
                projectiles.removeIndex(i);
            }
        }
    }

    private void updateCamera() {
        float halfWidth = worldCamera.viewportWidth * worldCamera.zoom / 2f;
        float halfHeight = worldCamera.viewportHeight * worldCamera.zoom / 2f;

        float camX = (halfWidth * 2 >= Arena.WIDTH)
            ? Arena.WIDTH / 2f
            : MathUtils.clamp(player.getX(), halfWidth, Arena.WIDTH - halfWidth);

        float camY = (halfHeight * 2 >= Arena.HEIGHT)
            ? Arena.HEIGHT / 2f
            : MathUtils.clamp(player.getY(), halfHeight, Arena.HEIGHT - halfHeight);

        worldCamera.position.set(camX, camY, 0);
        worldCamera.update();
    }

    private void endMatch(boolean victory) {
        matchFinished = true;
        audio.pauseMusic();
        game.setScreen(new ResultsScreen(game, victory, score));
    }


    private void draw() {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewport.apply();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        batch.draw(floorRegion, 0, 0, Arena.WIDTH, Arena.HEIGHT);

        for (Obstacle obstacle : arena.getObstacles()) {
            Rectangle b = obstacle.getBounds();
            batch.draw(obstacleTexture, b.x, b.y, b.width, b.height);
        }

        int enemyFrame = ((int) (animationTimer * 4)) % enemyFrames.length;
        for (EnemyDummy enemy : enemies) {
            if (!enemy.isAlive()) continue;
            batch.draw(enemyFrames[enemyFrame],
                enemy.getX() - EnemyDummy.SIZE / 2f,
                enemy.getY() - EnemyDummy.SIZE / 2f,
                EnemyDummy.SIZE, EnemyDummy.SIZE);
        }

        boolean visible = !player.isInvulnerable() || ((int) (animationTimer * 12) % 2 == 0);
        if (visible) {
            TextureRegion frame = playerAnimation.getFrame(player.getFacingDirection(), player.isMoving());
            batch.draw(frame,
                player.getX() - Player.SIZE / 2f,
                player.getY() - Player.SIZE / 2f,
                Player.SIZE, Player.SIZE);
        }

        for (Projectile p : projectiles) {
            Texture tex = p.isFromPlayer() ? projPlayerTex : projEnemyTex;
            batch.draw(tex,
                p.getX() - Projectile.SIZE / 2f,
                p.getY() - Projectile.SIZE / 2f,
                Projectile.SIZE, Projectile.SIZE);
        }
        batch.end();

        hud.getViewport().apply();
        hud.render(batch, player, score, timeRemaining, audio.isMuted(), audio.getVolume());
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height);
        hud.resize(width, height);
    }

    /** Llamado por PauseScreen al reanudar la partida. */
    public void resumeGame() {
        Gdx.input.setInputProcessor(input);
        input.releaseAllKeys();
        audio.playMusic();
    }

    /** Reinicia la partida desde cero conservando la pantalla y sus recursos. */
    public void restart() {
        resetMatch();
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        // Guarda: esta pantalla puede liberarse al terminar la partida o al
        // salir al menú desde la pausa; liberar dos veces provocaría un error.
        if (disposed) return;
        disposed = true;

        batch.dispose();
        hud.dispose();
        playerAnimation.dispose();
        enemySheet.dispose();
        obstacleTexture.dispose();
        floorTexture.dispose();
        projPlayerTex.dispose();
        projEnemyTex.dispose();
    }
}
