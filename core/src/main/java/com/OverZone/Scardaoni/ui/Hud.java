package com.OverZone.Scardaoni.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.entities.Player;
import com.OverZone.Scardaoni.game.Team;
import com.OverZone.Scardaoni.network.WorldSnapshot;

/**
 * Interfaz fija sobre el juego: vida, habilidad, reloj, puntaje por equipo,
 * retardo de red y volumen. Tiene su propia cámara, que no se mueve.
 *
 * Todo lo que muestra lo recibe por parámetro (datos que llegaron del
 * servidor); el HUD nunca cambia el estado de la partida.
 */
public class Hud {

    public static final float VIRTUAL_WIDTH = 480f;
    public static final float VIRTUAL_HEIGHT = 270f;

    private static final float MARGIN = 10f;
    private static final float BAR_WIDTH = 110f;
    private static final float BAR_HEIGHT = 10f;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final BitmapFont bigFont = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    public Hud() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
        bigFont.getData().setScale(1.6f);
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
                       boolean muted, float volume, float pingMs) {
        camera.update();

        float healthY = VIRTUAL_HEIGHT - 26;
        float dashY = VIRTUAL_HEIGHT - 44;
        float healthRatio = me == null ? 0f : Math.max(0f, me.health / Player.MAX_HEALTH);
        float abilityReady = me == null ? 0f : 1f - me.dashCooldown;

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.15f, 0.15f, 0.18f, 1f);
        shapes.rect(MARGIN, healthY, BAR_WIDTH, BAR_HEIGHT);
        shapes.setColor(1f - healthRatio, 0.15f + healthRatio * 0.75f, 0.2f, 1f);
        shapes.rect(MARGIN, healthY, BAR_WIDTH * healthRatio, BAR_HEIGHT);

        shapes.setColor(0.15f, 0.15f, 0.18f, 1f);
        shapes.rect(MARGIN, dashY, BAR_WIDTH, BAR_HEIGHT - 3);
        if (abilityReady >= 1f) shapes.setColor(0.35f, 0.85f, 1f, 1f);
        else shapes.setColor(0.2f, 0.35f, 0.55f, 1f);
        shapes.rect(MARGIN, dashY, BAR_WIDTH * abilityReady, BAR_HEIGHT - 3);

        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch, "Vida  " + (me == null ? 0 : (int) me.health), MARGIN, VIRTUAL_HEIGHT - 8);
        font.setColor(abilityReady >= 1f ? Color.CYAN : Color.GRAY);
        font.draw(batch, "Impulso Over", MARGIN + BAR_WIDTH + 8, dashY + 9);

        // Reloj
        font.setColor(Color.WHITE);
        float time = Math.max(0, snap.phaseTimer);
        drawCentered(batch, font, String.format("%02d:%02d", (int) (time / 60), (int) (time % 60)),
            VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT - 8);

        // Puntaje por equipo (eliminaciones)
        font.setColor(Color.SKY);
        font.draw(batch, "Azul " + snap.scoreBlue, VIRTUAL_WIDTH - 120, VIRTUAL_HEIGHT - 8);
        font.setColor(Color.SALMON);
        font.draw(batch, "Rojo " + snap.scoreRed, VIRTUAL_WIDTH - 60, VIRTUAL_HEIGHT - 8);
        if (me != null) {
            font.setColor(me.team == Team.BLUE ? Color.SKY : Color.SALMON);
            font.draw(batch, "Tu equipo: " + Team.name(me.team), VIRTUAL_WIDTH - 120, VIRTUAL_HEIGHT - 26);
        }

        // Eliminado: cuenta para reaparecer
        if (me != null && !me.alive) {
            bigFont.setColor(Color.SCARLET);
            drawCentered(batch, bigFont, "ELIMINADO", VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f + 20);
            font.setColor(Color.WHITE);
            drawCentered(batch, font, "Reapareces en " + (int) Math.ceil(me.respawnIn) + " s",
                VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f - 6);
        }

        font.setColor(Color.GRAY);
        font.draw(batch, muted ? "Sonido: OFF" : "Volumen: " + (int) (volume * 100) + "%", MARGIN, 18);
        String ping = pingMs < 0 ? "Ping: -" : "Ping: " + Math.round(pingMs) + " ms";
        drawCentered(batch, font, ping, VIRTUAL_WIDTH / 2f, 18);
        font.draw(batch, "ESC: menu", VIRTUAL_WIDTH - 75, 18);

        batch.end();
    }

    private void drawCentered(SpriteBatch batch, BitmapFont f, String text, float x, float y) {
        layout.setText(f, text);
        f.draw(batch, text, x - layout.width / 2f, y);
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void dispose() {
        shapes.dispose();
        font.dispose();
        bigFont.dispose();
    }
}
