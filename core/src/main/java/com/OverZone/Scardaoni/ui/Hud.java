package com.OverZone.Scardaoni.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.OverZone.Scardaoni.entities.Player;


public class Hud {

    public static final float VIRTUAL_WIDTH = 480f;
    public static final float VIRTUAL_HEIGHT = 270f;

    private static final float MARGIN = 10f;
    private static final float BAR_WIDTH = 110f;
    private static final float BAR_HEIGHT = 10f;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont(); // fuente por defecto (placeholder)

    public Hud() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void render(SpriteBatch batch, Player player, int score, float timeRemaining, boolean muted, float volume) {
        camera.update();

        float healthY = VIRTUAL_HEIGHT - 26;
        float dashY = VIRTUAL_HEIGHT - 44;

        // --- Barras dibujadas con ShapeRenderer ---
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Barra de vida
        shapes.setColor(0.15f, 0.15f, 0.18f, 1f);
        shapes.rect(MARGIN, healthY, BAR_WIDTH, BAR_HEIGHT);
        float healthRatio = Math.max(0f, player.getHealth() / Player.MAX_HEALTH);

        shapes.setColor(1f - healthRatio, 0.15f + healthRatio * 0.75f, 0.2f, 1f);
        shapes.rect(MARGIN, healthY, BAR_WIDTH * healthRatio, BAR_HEIGHT);

        // Barra de la habilidad especial: se llena a medida que se recarga
        shapes.setColor(0.15f, 0.15f, 0.18f, 1f);
        shapes.rect(MARGIN, dashY, BAR_WIDTH, BAR_HEIGHT - 3);
        float abilityReady = 1f - player.getDashCooldownProgress();
        if (abilityReady >= 1f) {
            shapes.setColor(0.35f, 0.85f, 1f, 1f);   // listo: celeste brillante
        } else {
            shapes.setColor(0.2f, 0.35f, 0.55f, 1f); // recargando: apagado
        }
        shapes.rect(MARGIN, dashY, BAR_WIDTH * abilityReady, BAR_HEIGHT - 3);

        shapes.end();

        // --- Textos ---
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        font.draw(batch, "Vida  " + (int) player.getHealth(), MARGIN, VIRTUAL_HEIGHT - 8);
        font.setColor(abilityReady >= 1f ? Color.CYAN : Color.GRAY);
        font.draw(batch, "Impulso Over", MARGIN + BAR_WIDTH + 8, dashY + 9);

        font.setColor(Color.WHITE);
        int minutes = (int) (Math.max(0, timeRemaining) / 60);
        int seconds = (int) (Math.max(0, timeRemaining) % 60);
        font.draw(batch, String.format("%02d:%02d", minutes, seconds), VIRTUAL_WIDTH / 2f - 18, VIRTUAL_HEIGHT - 8);

        font.draw(batch, "Puntaje: " + score, VIRTUAL_WIDTH - 110, VIRTUAL_HEIGHT - 8);

        font.setColor(Color.GRAY);
        font.draw(batch, muted ? "Sonido: OFF" : "Volumen: " + (int) (volume * 100) + "%", MARGIN, 18);
        font.draw(batch, "ESC: pausa y controles", VIRTUAL_WIDTH - 160, 18);

        batch.end();
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void dispose() {
        shapes.dispose();
        font.dispose();
    }
}
