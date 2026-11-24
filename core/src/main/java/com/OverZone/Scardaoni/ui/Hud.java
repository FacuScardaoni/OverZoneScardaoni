package com.OverZone.Scardaoni.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.shared.CharacterType;
import com.OverZone.Scardaoni.shared.GameMode;
import com.OverZone.Scardaoni.shared.Team;
import com.OverZone.Scardaoni.shared.WorldSnapshot;

/**
 * Interfaz fija sobre el juego, con cámara propia que no se mueve:
 *  - arriba a la izquierda: personaje, barra de vida y recarga de la habilidad;
 *  - arriba al centro: reloj y puntaje de cada equipo;
 *  - abajo a la derecha: botón rojo de ataque (con mira) y botón amarillo de súper;
 *  - abajo a la izquierda: ping y volumen.
 *
 * Todo lo que muestra lo recibe por parámetro (datos que llegaron del
 * servidor); el HUD nunca cambia el estado de la partida.
 */
public class Hud {

    public static final float VIRTUAL_WIDTH = 480f;
    public static final float VIRTUAL_HEIGHT = 270f;

    private final UiKit ui;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final GlyphLayout layout = new GlyphLayout();
    private float time = 0f;

    public Hud(UiKit ui) {
        this.ui = ui;
        camera = new OrthographicCamera();
        // ExtendViewport: como mínimo 480x270, y si la ventana tiene otra forma
        // se agranda. Los elementos se ubican respecto de los bordes reales
        // (arriba, derecha), así siempre quedan en las esquinas de la ventana.
        viewport = new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        // Permite cambiar entre relleno y contorno sin cerrar el ShapeRenderer.
        shapes.setAutoShapeType(true);
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    /**
     * @param me      mi jugador (puede ser null un instante al conectarse)
     * @param snap    estado de la partida recibido del servidor
     * @param pingMs  retardo ida y vuelta con el servidor, o -1 si no se midió
     */
    public void render(SpriteBatch batch, WorldSnapshot.PlayerState me, WorldSnapshot snap,
                       boolean muted, float volume, float pingMs, HudControls controls, float delta) {
        time += delta;
        camera.update();
        float top = viewport.getWorldHeight();          // borde superior
        float midX = viewport.getWorldWidth() / 2f;     // centro horizontal
        float ax = controls.attackX(), ay = controls.attackY();
        float sx = controls.superX(), sy = controls.superY();
        Vector3 m = viewport.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        CharacterType character = me == null ? CharacterType.VOLT : CharacterType.fromId(me.character);
        float healthRatio = me == null ? 0f : MathUtils.clamp(me.health / character.maxHealth, 0f, 1f);
        float superReady = me == null ? 0f : 1f - me.abilityCooldown;
        boolean superCharged = superReady >= 0.999f;
        boolean superActive = me != null && me.abilityActive;
        Color accent = UiKit.characterColor(character);

        // Puntajes: en Equipos, azul contra rojo. En Todos contra todos, mis
        // eliminaciones contra las del jugador que va primero.
        boolean ffa = snap.getMode() == GameMode.FREE_FOR_ALL;
        WorldSnapshot.PlayerState leader = ffa ? leader(snap) : null;
        int leftScore, rightScore;
        Color leftColor, rightColor;
        if (ffa) {
            leftScore = me == null ? 0 : me.kills;
            rightScore = leader == null ? 0 : leader.kills;
            leftColor = me == null ? Color.GRAY : UiKit.playerColor(GameMode.FREE_FOR_ALL, me.team);
            rightColor = leader == null ? Color.GRAY : UiKit.playerColor(GameMode.FREE_FOR_ALL, leader.team);
        } else {
            leftScore = snap.scoreBlue;
            rightScore = snap.scoreRed;
            leftColor = UiKit.BLUE_TEAM;
            rightColor = UiKit.RED_TEAM;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // --- Panel del jugador (arriba a la izquierda) ---
        panel(6, top - 56, 140, 50, accent);
        float barX = 12, barY = top - 34, barW = 128, barH = 9;
        shapes.setColor(0.08f, 0.09f, 0.13f, 1f);
        shapes.rect(barX, barY, barW, barH);
        Color hp = healthRatio > 0.5f ? new Color(0.3f, 0.95f, 0.45f, 1f)
            : healthRatio > 0.25f ? new Color(1f, 0.8f, 0.2f, 1f) : new Color(1f, 0.25f, 0.25f, 1f);
        shapes.setColor(hp);
        shapes.rect(barX, barY, barW * healthRatio, barH);
        shapes.setColor(1f, 1f, 1f, 0.25f);
        shapes.rect(barX, barY + barH - 2.5f, barW * healthRatio, 2.5f);   // brillo superior
        // Marcas cada 20 puntos de vida
        shapes.setColor(0.02f, 0.03f, 0.07f, 0.8f);
        for (float hpMark = 20; hpMark < character.maxHealth; hpMark += 20) {
            shapes.rect(barX + barW * hpMark / character.maxHealth, barY, 1f, barH);
        }
        // Barra de recarga de la habilidad
        shapes.setColor(0.08f, 0.09f, 0.13f, 1f);
        shapes.rect(barX, top - 42, barW, 4);
        shapes.setColor(superActive ? UiKit.ORANGE : (superCharged ? UiKit.YELLOW : new Color(0.55f, 0.48f, 0.2f, 1f)));
        shapes.rect(barX, top - 42, barW * superReady, 4);

        // --- Reloj y puntajes (arriba al centro) ---
        panel(midX - 36, top - 34, 72, 28, Color.WHITE);
        scoreBox(midX - 82, top - 32, leftColor);
        scoreBox(midX + 42, top - 32, rightColor);

        // --- Botón de súper: anillo de carga ---
        shapes.setColor(0.05f, 0.05f, 0.08f, 0.85f);
        shapes.circle(sx, sy, HudControls.SUPER_R + 4, 40);
        shapes.setColor(superActive ? UiKit.ORANGE : UiKit.YELLOW);
        if (superReady > 0) {
            shapes.arc(sx, sy, HudControls.SUPER_R + 4, 90 - 360 * superReady, 360 * superReady, 40);
        }
        // --- Botón de ataque: aro oscuro ---
        shapes.setColor(0.05f, 0.05f, 0.08f, 0.85f);
        shapes.circle(ax, ay, HudControls.ATTACK_R + 3, 48);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Brillos de los botones
        boolean overAttack = controls.overAttack(m.x, m.y);
        UiDraw.glow(batch, ui.glow, ax, ay, 110,
            UiKit.RED, controls.isAttackHeld() ? 0.8f : (overAttack ? 0.55f : 0.3f));
        if (superCharged || superActive) {
            float pulse = 0.55f + 0.35f * MathUtils.sin(time * 6f);
            UiDraw.glow(batch, ui.glow, sx, sy, 95,
                superActive ? UiKit.ORANGE : UiKit.YELLOW, pulse);
        }

        // Botón de ataque (se achica un poco al presionarlo)
        float attackScale = controls.isAttackHeld() ? 0.92f : (overAttack ? 1.04f : 1f);
        drawCircleButton(batch, ui.attackButton, ax, ay,
            HudControls.ATTACK_R * attackScale, Color.WHITE);
        // Botón de súper: gris mientras carga, amarillo brillante cuando está listo
        Texture superTex = ui.superButton(character);
        float superScale = controls.isSuperHeld() ? 0.9f : (controls.overSuper(m.x, m.y) ? 1.05f : 1f);
        Color superTint = superCharged || superActive ? Color.WHITE : new Color(0.45f, 0.45f, 0.5f, 1f);
        drawCircleButton(batch, superTex, sx, sy,
            HudControls.SUPER_R * superScale, superTint);

        // "Perilla" del joystick mientras se arrastra
        if (controls.isAttackHeld()) knob(batch, ax, ay, controls.getAttackDrag(), UiKit.RED);
        if (controls.isSuperHeld()) knob(batch, sx, sy, controls.getSuperDrag(), UiKit.YELLOW);

        // --- Textos ---
        BitmapFont body = ui.body, small = ui.small, title = ui.title;
        small.setColor(accent);
        small.draw(batch, character.displayName.toUpperCase(), 12, top - 11);
        body.setColor(Color.WHITE);
        String hpText = (me == null ? 0 : (int) Math.ceil(me.health)) + " / " + (int) character.maxHealth;
        layout.setText(small, hpText);
        small.setColor(Color.WHITE);
        small.draw(batch, hpText, 140 - layout.width, top - 11);
        small.setColor(superActive ? UiKit.ORANGE : (superCharged ? UiKit.YELLOW : Color.GRAY));
        small.draw(batch, character.abilityName + (superCharged && !superActive ? "  ¡LISTA!" : ""), 12, top - 45);

        float t = Math.max(0, snap.phaseTimer);
        boolean lastSeconds = t <= 10;
        title.setColor(lastSeconds ? (((int) (time * 4)) % 2 == 0 ? UiKit.RED : Color.WHITE) : Color.WHITE);
        centered(batch, title, String.format("%d:%02d", (int) (t / 60), (int) (t % 60)), midX, top - 11);

        title.setColor(Color.WHITE);
        centered(batch, title, String.valueOf(leftScore), midX - 60, top - 11);
        centered(batch, title, String.valueOf(rightScore), midX + 64, top - 11);
        if (me != null && !ffa) {
            small.setColor(me.team == Team.BLUE ? UiKit.BLUE_TEAM : UiKit.RED_TEAM);
            centered(batch, small, "TU EQUIPO: " + Team.name(me.team).toUpperCase(), midX, top - 40);
        } else if (me != null) {
            small.setColor(Color.LIGHT_GRAY);
            String leaderName = leader == null ? "-" : (leader.id == me.id ? "VOS" : leader.name.toUpperCase());
            centered(batch, small, "VOS  |  LÍDER: " + leaderName, midX, top - 40);
        }

        small.setColor(Color.GRAY);
        small.draw(batch, (pingMs < 0 ? "Ping -" : "Ping " + Math.round(pingMs) + " ms")
            + "   " + (muted ? "Sonido OFF" : "Vol " + (int) (volume * 100) + "%"), 8, 14);
        small.draw(batch, "ESC: menú", 8, 26);

        // Eliminado: cuenta para reaparecer
        if (me != null && !me.alive) {
            BitmapFont huge = ui.huge;
            float y = top / 2f + 26;
            huge.setColor(1f, 0.45f, 0.08f, 0.8f);
            centered(batch, huge, "ELIMINADO", midX + 1.5f, y - 1.5f);
            huge.setColor(UiKit.RED);
            centered(batch, huge, "ELIMINADO", midX, y);
            body.setColor(Color.WHITE);
            centered(batch, body, "Reaparecés en " + (int) Math.ceil(me.respawnIn) + " s", midX, y - 40);
        }

        batch.end();
    }

    /** Jugador con más eliminaciones (null si nadie eliminó a nadie todavía). */
    private static WorldSnapshot.PlayerState leader(WorldSnapshot snap) {
        WorldSnapshot.PlayerState best = null;
        for (WorldSnapshot.PlayerState p : snap.players) {
            if (p.kills > 0 && (best == null || p.kills > best.kills)) best = p;
        }
        return best;
    }

    private void panel(float x, float y, float w, float h, Color accent) {
        shapes.setColor(0.02f, 0.03f, 0.07f, 0.72f);
        UiDraw.chamfer(shapes, x, y, w, h, 6);
        shapes.setColor(accent.r, accent.g, accent.b, 0.75f);
        UiDraw.chamferOutline(shapes, x, y, w, h, 6);
    }

    private void scoreBox(float x, float y, Color color) {
        shapes.setColor(color.r * 0.35f, color.g * 0.35f, color.b * 0.35f, 0.85f);
        UiDraw.chamfer(shapes, x, y, 44, 24, 5);
        shapes.setColor(color);
        UiDraw.chamferOutline(shapes, x, y, 44, 24, 5);
    }

    private static void drawCircleButton(SpriteBatch batch, Texture tex, float cx, float cy, float r, Color tint) {
        batch.setColor(tint);
        batch.draw(tex, cx - r, cy - r, r * 2, r * 2);
        batch.setColor(Color.WHITE);
    }

    private void knob(SpriteBatch batch, float cx, float cy, Vector2 drag, Color color) {
        float x = cx + drag.x, y = cy + drag.y;
        UiDraw.glow(batch, ui.glow, x, y, 40, color, 0.9f);
        UiDraw.glow(batch, ui.glow, x, y, 16, Color.WHITE, 1f);
    }

    private void centered(SpriteBatch batch, BitmapFont f, String text, float x, float y) {
        layout.setText(f, text);
        f.draw(batch, text, x - layout.width / 2f, y);
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void dispose() {
        // Las fuentes y texturas son de UiKit; acá solo se libera lo propio.
        shapes.dispose();
    }
}
